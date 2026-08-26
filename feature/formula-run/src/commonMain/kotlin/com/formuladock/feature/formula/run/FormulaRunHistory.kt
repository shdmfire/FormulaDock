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
        val nextRevisionNo = revisionNo + 1
        val revisionId = "${currentSessionId}_revision_${nextRevisionNo}_$timestamp"
        repository.appendRevision(
            sessionId = currentSessionId,
            history = buildSuccessfulRevision(
                id = revisionId,
                formula = formula,
                inputValues = inputValues,
                result = result,
                timestamp = timestamp,
            ),
            revisionNo = nextRevisionNo,
            changedKeys = changedKeys,
        )
        revisionNo = nextRevisionNo
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
            timestamp = timestamp,
        )
    )
}

private fun buildSuccessfulRevision(
    id: String,
    formula: FormulaDefinition,
    inputValues: Map<String, String>,
    result: FormulaEvaluationResult.Success,
    timestamp: Long,
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
    createdAt = timestamp,
    updatedAt = timestamp,
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
