package com.formuladock.feature.formula.run

import com.formuladock.core.data.currentTimeMillis
import com.formuladock.core.data.history.CalculationHistoryRepository
import com.formuladock.core.formula.engine.FormulaEvaluationOutput
import com.formuladock.core.formula.engine.FormulaEvaluationResult
import com.formuladock.core.model.formula.model.FormulaDefinition
import com.formuladock.core.model.history.model.CalculationHistory
import com.formuladock.core.model.history.model.CalculationHistoryInput
import com.formuladock.core.model.history.model.CalculationHistoryOutput
import com.formuladock.core.model.history.model.CalculationStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CalculationSessionRecorder(
    private val repository: CalculationHistoryRepository,
    private val nowProvider: () -> Long = ::currentTimeMillis,
) {
    private val mutex = Mutex()
    private var sessionId: String? = null
    private var revisionNo: Int = 0
    private var lastCommittedInputs: Map<String, String>? = null
    private var latestRevisionId: String? = null
    private var latestRevisionCreatedAt: Long = 0L
    private var latestRevisionUpdatedAt: Long = 0L
    private var latestChangedKeys: Set<String> = emptySet()

    val currentSessionId: String?
        get() = sessionId

    suspend fun loadSessionHistory(): CalculationHistory? = mutex.withLock {
        val id = sessionId ?: return@withLock null
        repository.getHistory(id)
    }

    suspend fun startOrResume(formula: FormulaDefinition) = mutex.withLock {
        val id = repository.openOrResumeSession(
            formula = formula,
            now = nowProvider(),
            resumeTimeoutMillis = SESSION_RESUME_TIMEOUT_MILLIS,
        )
        sessionId = id
        val latest = repository.getLatestRevision(id)
        revisionNo = latest?.revisionNo ?: 0
        lastCommittedInputs = latest?.inputs?.associate { it.key to (it.rawValue ?: "") }
        latestRevisionId = latest?.id
        latestRevisionCreatedAt = latest?.createdAt ?: 0L
        latestRevisionUpdatedAt = latest?.updatedAt ?: 0L
        latestChangedKeys = latest?.changedKeys.orEmpty()
    }

    suspend fun commit(
        formula: FormulaDefinition,
        inputValues: Map<String, String>,
        result: FormulaEvaluationResult.Success,
    ) = mutex.withLock {
        val currentSessionId = sessionId ?: return@withLock
        val changedKeys = findChangedKeys(lastCommittedInputs, inputValues)
        if (lastCommittedInputs != null && changedKeys.isEmpty()) return@withLock

        val timestamp = nowProvider()
        val elapsedSinceLatest = timestamp - latestRevisionUpdatedAt
        val shouldReplaceLatest = latestRevisionId != null &&
            changedKeys == latestChangedKeys &&
            elapsedSinceLatest in 0..REVISION_REPLACE_WINDOW_MILLIS

        if (shouldReplaceLatest) {
            val revisionId = checkNotNull(latestRevisionId)
            repository.replaceLatestRevision(
                sessionId = currentSessionId,
                history = buildSuccessfulRevision(
                    id = revisionId,
                    formula = formula,
                    inputValues = inputValues,
                    result = result,
                    createdAt = latestRevisionCreatedAt,
                    updatedAt = timestamp,
                ),
                revisionNo = revisionNo,
                changedKeys = changedKeys,
            )
            latestRevisionUpdatedAt = timestamp
        } else {
            val nextRevisionNo = revisionNo + 1
            val revisionId = "${currentSessionId}_revision_${nextRevisionNo}_$timestamp"
            repository.appendRevision(
                sessionId = currentSessionId,
                history = buildSuccessfulRevision(
                    id = revisionId,
                    formula = formula,
                    inputValues = inputValues,
                    result = result,
                    createdAt = timestamp,
                    updatedAt = timestamp,
                ),
                revisionNo = nextRevisionNo,
                changedKeys = changedKeys,
            )
            revisionNo = nextRevisionNo
            latestRevisionId = revisionId
            latestRevisionCreatedAt = timestamp
            latestRevisionUpdatedAt = timestamp
            latestChangedKeys = changedKeys
        }
        lastCommittedInputs = inputValues.toMap()
    }

    suspend fun pause() = mutex.withLock {
        sessionId?.let { repository.pauseSession(it, nowProvider()) }
    }

    suspend fun close() = mutex.withLock {
        sessionId?.let { repository.closeSession(it, nowProvider()) }
        sessionId = null
    }

    private fun findChangedKeys(
        previous: Map<String, String>?,
        current: Map<String, String>,
    ): Set<String> {
        if (previous == null) return current.keys
        return (previous.keys + current.keys)
            .filterTo(linkedSetOf()) { key -> previous[key] != current[key] }
    }

    private companion object {
        const val SESSION_RESUME_TIMEOUT_MILLIS = 5 * 60 * 1000L
        const val REVISION_REPLACE_WINDOW_MILLIS = 2_000L
    }
}

/** Kept for callers that still need a one-shot, already-closed history entry. */
suspend fun CalculationHistoryRepository.saveSuccessfulRun(
    formula: FormulaDefinition,
    inputValues: Map<String, String>,
    result: FormulaEvaluationResult.Success,
) {
    val timestamp = currentTimeMillis()
    saveHistory(
        buildSuccessfulRevision(
            id = "session_${formula.id}_$timestamp",
            formula = formula,
            inputValues = inputValues,
            result = result,
            createdAt = timestamp,
            updatedAt = timestamp,
        )
    )
}

private fun buildSuccessfulRevision(
    id: String,
    formula: FormulaDefinition,
    inputValues: Map<String, String>,
    result: FormulaEvaluationResult.Success,
    createdAt: Long,
    updatedAt: Long,
): CalculationHistory = CalculationHistory(
    id = id,
    formulaId = formula.id,
    formulaTitle = formula.title,
    formulaDescription = formula.description,
    formulaIsBuiltin = formula.isBuiltin,
    status = CalculationStatus.SUCCESS,
    inputs = formula.inputs.mapIndexed { index, input ->
        val rawValue = inputValues[input.key]
        CalculationHistoryInput(
            id = "${id}_input_${input.key}",
            key = input.key,
            label = input.label,
            rawValue = rawValue,
            numericValue = rawValue?.toDoubleOrNull(),
            unit = input.unit,
            required = input.required,
            sortOrder = index,
        )
    },
    outputs = result.outputs.mapIndexed { index, output -> output.toHistoryOutput(id, formula, index) },
    errorMessage = null,
    errorFieldKey = null,
    note = null,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun FormulaEvaluationOutput.toHistoryOutput(
    historyId: String,
    formula: FormulaDefinition,
    index: Int,
): CalculationHistoryOutput {
    val formulaOutput = formula.outputs.firstOrNull { it.key == key }
    return CalculationHistoryOutput(
        id = "${historyId}_output_$key",
        key = key,
        label = label,
        expression = formulaOutput?.expression.orEmpty(),
        value = value,
        formattedValue = formattedValue,
        unit = unit,
        precision = formulaOutput?.precision ?: 2,
        sortOrder = index,
    )
}
