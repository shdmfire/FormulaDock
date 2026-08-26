package com.formuladock.core.data.history

import com.formuladock.core.data.history.CalculationHistoryMapper.toLongFlag
import com.formuladock.core.data.history.CalculationHistoryMapper.toStorageValue
import com.formuladock.core.database.Calculation_session
import com.formuladock.core.database.FormulaDockDatabase
import com.formuladock.core.model.formula.model.FormulaDefinition
import com.formuladock.core.model.history.model.CalculationHistory
import com.formuladock.core.model.history.model.CalculationRevision
import com.formuladock.core.model.history.model.CalculationSessionStatus

interface CalculationHistoryRepository {
    suspend fun countHistories(): Long
    suspend fun getHistories(limit: Long, offset: Long): List<CalculationHistory>
    suspend fun getHistoriesByFormulaId(formulaId: String, limit: Long, offset: Long): List<CalculationHistory>
    suspend fun getHistory(id: String): CalculationHistory?
    suspend fun saveHistory(history: CalculationHistory)
    suspend fun openOrResumeSession(formula: FormulaDefinition, now: Long, resumeTimeoutMillis: Long): String
    suspend fun appendRevision(
        sessionId: String,
        history: CalculationHistory,
        revisionNo: Int,
        changedKeys: Set<String>,
    )
    suspend fun replaceLatestRevision(
        sessionId: String,
        history: CalculationHistory,
        revisionNo: Int,
        changedKeys: Set<String>,
    )
    suspend fun getLatestRevision(sessionId: String): CalculationRevision?
    suspend fun pauseSession(id: String, now: Long)
    suspend fun closeSession(id: String, now: Long)
    suspend fun closeExpiredSessions(cutoff: Long)
    suspend fun updateNote(id: String, note: String?, updatedAt: Long)
    suspend fun deleteHistory(id: String)
    suspend fun deleteAllHistories()
}

class SqlDelightCalculationHistoryRepository(
    private val database: FormulaDockDatabase,
) : CalculationHistoryRepository {
    private val queries = database.formulaQueries

    override suspend fun countHistories(): Long = queries.countCalculationHistories().executeAsOne()

    override suspend fun getHistories(limit: Long, offset: Long): List<CalculationHistory> =
        queries.selectCalculationHistories(limit, offset).executeAsList().mapNotNull { it.toModelWithDetails(loadAllRevisions = false) }

    override suspend fun getHistoriesByFormulaId(
        formulaId: String,
        limit: Long,
        offset: Long,
    ): List<CalculationHistory> =
        queries.selectCalculationHistoriesByFormulaId(formulaId, limit, offset)
            .executeAsList()
            .mapNotNull { it.toModelWithDetails(loadAllRevisions = false) }

    override suspend fun getHistory(id: String): CalculationHistory? =
        queries.selectCalculationHistoryById(id).executeAsOneOrNull()?.toModelWithDetails(loadAllRevisions = true)

    /** Compatibility entry point: an old one-shot history becomes a closed one-revision session. */
    override suspend fun saveHistory(history: CalculationHistory) {
        val sessionId = history.id
        val revisionId = "revision_${history.id}"
        database.transaction {
            queries.insertCalculationSession(
                id = sessionId,
                formula_id = history.formulaId,
                formula_title = history.formulaTitle,
                formula_description = history.formulaDescription,
                formula_is_builtin = history.formulaIsBuiltin.toLongFlag(),
                status = CalculationSessionStatus.CLOSED.name,
                started_at = history.createdAt,
                last_active_at = history.updatedAt,
                ended_at = history.updatedAt,
                final_revision_id = revisionId,
                revision_count = 1,
                note = history.note,
            )
            insertRevision(
                sessionId = sessionId,
                history = history.copy(id = revisionId),
                revisionNo = 1,
                changedKeys = history.inputs.mapTo(linkedSetOf()) { it.key },
            )
        }
    }

    override suspend fun openOrResumeSession(
        formula: FormulaDefinition,
        now: Long,
        resumeTimeoutMillis: Long,
    ): String {
        val cutoff = now - resumeTimeoutMillis
        return database.transactionWithResult {
            queries.closeExpiredCalculationSessions(cutoff)
            val existing = queries.selectResumableCalculationSession(formula.id, cutoff).executeAsOneOrNull()
            if (existing != null) {
                queries.activateCalculationSession(last_active_at = now, id = existing.id)
                existing.id
            } else {
                val id = "session_${formula.id}_$now"
                queries.insertCalculationSession(
                    id = id,
                    formula_id = formula.id,
                    formula_title = formula.title,
                    formula_description = formula.description,
                    formula_is_builtin = formula.isBuiltin.toLongFlag(),
                    status = CalculationSessionStatus.ACTIVE.name,
                    started_at = now,
                    last_active_at = now,
                    ended_at = null,
                    final_revision_id = null,
                    revision_count = 0,
                    note = null,
                )
                id
            }
        }
    }

    override suspend fun appendRevision(
        sessionId: String,
        history: CalculationHistory,
        revisionNo: Int,
        changedKeys: Set<String>,
    ) {
        database.transaction {
            insertRevision(sessionId, history, revisionNo, changedKeys)
            queries.updateCalculationSessionAfterRevision(
                final_revision_id = history.id,
                revision_count = revisionNo.toLong(),
                last_active_at = history.updatedAt,
                id = sessionId,
            )
        }
    }

    override suspend fun replaceLatestRevision(
        sessionId: String,
        history: CalculationHistory,
        revisionNo: Int,
        changedKeys: Set<String>,
    ) {
        database.transaction {
            insertRevision(sessionId, history, revisionNo, changedKeys)
            queries.updateCalculationSessionAfterRevision(
                final_revision_id = history.id,
                revision_count = revisionNo.toLong(),
                last_active_at = history.updatedAt,
                id = sessionId,
            )
        }
    }

    override suspend fun getLatestRevision(sessionId: String): CalculationRevision? =
        queries.selectLatestCalculationRevision(sessionId).executeAsOneOrNull()?.toRevisionWithDetails()

    override suspend fun pauseSession(id: String, now: Long) {
        queries.pauseCalculationSession(last_active_at = now, id = id)
    }

    override suspend fun closeSession(id: String, now: Long) {
        queries.closeCalculationSession(last_active_at = now, ended_at = now, id = id)
    }

    override suspend fun closeExpiredSessions(cutoff: Long) {
        queries.closeExpiredCalculationSessions(cutoff)
    }

    override suspend fun updateNote(id: String, note: String?, updatedAt: Long) {
        queries.updateCalculationHistoryNote(note = note, last_active_at = updatedAt, id = id)
    }

    override suspend fun deleteHistory(id: String) {
        queries.deleteCalculationHistoryById(id)
    }

    override suspend fun deleteAllHistories() {
        queries.deleteAllCalculationHistories()
    }

    private fun insertRevision(
        sessionId: String,
        history: CalculationHistory,
        revisionNo: Int,
        changedKeys: Set<String>,
    ) {
        queries.insertCalculationHistory(
            id = history.id,
            session_id = sessionId,
            revision_no = revisionNo.toLong(),
            changed_keys = changedKeys.toStorageValue(),
            formula_id = history.formulaId,
            formula_title = history.formulaTitle,
            formula_description = history.formulaDescription,
            formula_is_builtin = history.formulaIsBuiltin.toLongFlag(),
            status = history.status.name,
            error_message = history.errorMessage,
            error_field_key = history.errorFieldKey,
            note = null,
            created_at = history.createdAt,
            updated_at = history.updatedAt,
        )

        queries.deleteCalculationHistoryInputs(history.id)
        queries.deleteCalculationHistoryOutputs(history.id)

        history.inputs.forEach { input ->
            queries.insertCalculationHistoryInput(
                id = "${history.id}_input_${input.key}",
                history_id = history.id,
                key = input.key,
                label = input.label,
                raw_value = input.rawValue,
                numeric_value = input.numericValue,
                unit = input.unit,
                required = input.required.toLongFlag(),
                sort_order = input.sortOrder.toLong(),
            )
        }

        history.outputs.forEach { output ->
            queries.insertCalculationHistoryOutput(
                id = "${history.id}_output_${output.key}",
                history_id = history.id,
                key = output.key,
                label = output.label,
                expression = output.expression,
                value_ = output.value,
                formatted_value = output.formattedValue,
                unit = output.unit,
                precision = output.precision.toLong(),
                sort_order = output.sortOrder.toLong(),
            )
        }
    }

    private fun Calculation_session.toModelWithDetails(loadAllRevisions: Boolean): CalculationHistory? {
        val revisions = if (loadAllRevisions) {
            queries.selectCalculationRevisionsBySessionId(id).executeAsList()
                .map { it.toRevisionWithDetails() }
        } else {
            emptyList()
        }
        val finalRevision = if (loadAllRevisions) {
            final_revision_id
                ?.let { finalId -> revisions.firstOrNull { it.id == finalId } }
                ?: revisions.firstOrNull()
        } else {
            final_revision_id
                ?.let { finalId -> queries.selectCalculationRevisionById(finalId).executeAsOneOrNull() }
                ?.toRevisionWithDetails()
                ?: queries.selectLatestCalculationRevision(id).executeAsOneOrNull()?.toRevisionWithDetails()
        } ?: return null

        return CalculationHistory(
            id = id,
            formulaId = formula_id,
            formulaTitle = formula_title,
            formulaDescription = formula_description,
            formulaIsBuiltin = formula_is_builtin != 0L,
            status = finalRevision.status,
            inputs = finalRevision.inputs,
            outputs = finalRevision.outputs,
            errorMessage = finalRevision.errorMessage,
            errorFieldKey = finalRevision.errorFieldKey,
            note = note,
            createdAt = started_at,
            updatedAt = last_active_at,
            sessionStatus = CalculationSessionStatus.valueOf(status),
            startedAt = started_at,
            endedAt = ended_at,
            revisionCount = revision_count.toInt(),
            revisions = revisions,
        )
    }

    private fun com.formuladock.core.database.Calculation_history.toRevisionWithDetails(): CalculationRevision =
        CalculationHistoryMapper.toRevision(
            history = this,
            inputs = queries.selectCalculationHistoryInputs(id).executeAsList(),
            outputs = queries.selectCalculationHistoryOutputs(id).executeAsList(),
        )
}
