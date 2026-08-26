package com.formuladock.formula

import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.data.history.CalculationHistoryRepository
import com.formuladock.core.model.formula.model.FormulaDefinition
import com.formuladock.core.model.history.model.CalculationHistory
import com.formuladock.core.model.history.model.CalculationRevision
import com.formuladock.core.model.history.model.CalculationSessionStatus

class InMemoryFormulaRepository : FormulaRepository {
    private val formulas = linkedMapOf<String, FormulaDefinition>()

    override suspend fun countFormulas(): Long = formulas.size.toLong()

    override suspend fun getAllFormulas(): List<FormulaDefinition> = formulas.values.toList()

    override suspend fun getFormula(id: String): FormulaDefinition? = formulas[id]

    override suspend fun saveFormula(formula: FormulaDefinition) {
        formulas[formula.id] = formula
    }

    override suspend fun deleteFormula(id: String) {
        formulas.remove(id)
    }
}

class InMemoryCalculationHistoryRepository : CalculationHistoryRepository {
    private val histories = linkedMapOf<String, CalculationHistory>()

    override suspend fun countHistories(): Long = histories.values.count { it.sessionStatus == CalculationSessionStatus.CLOSED }.toLong()

    override suspend fun getHistories(limit: Long, offset: Long): List<CalculationHistory> {
        return histories.values.filter { it.sessionStatus == CalculationSessionStatus.CLOSED }
            .sortedByDescending { it.updatedAt }
            .drop(offset.toInt())
            .take(limit.toInt())
    }

    override suspend fun getHistoriesByFormulaId(formulaId: String, limit: Long, offset: Long): List<CalculationHistory> {
        return histories.values.filter { it.formulaId == formulaId && it.sessionStatus == CalculationSessionStatus.CLOSED }
            .sortedByDescending { it.updatedAt }
            .drop(offset.toInt())
            .take(limit.toInt())
    }

    override suspend fun getHistory(id: String): CalculationHistory? = histories[id]

    override suspend fun saveHistory(history: CalculationHistory) {
        histories[history.id] = history.copy(sessionStatus = CalculationSessionStatus.CLOSED)
    }

    override suspend fun openOrResumeSession(
        formula: FormulaDefinition,
        now: Long,
        resumeTimeoutMillis: Long,
    ): String {
        closeExpiredSessions(now - resumeTimeoutMillis)
        val existing = histories.values
            .filter { it.formulaId == formula.id && it.sessionStatus != CalculationSessionStatus.CLOSED }
            .maxByOrNull { it.updatedAt }
        if (existing != null) {
            histories[existing.id] = existing.copy(sessionStatus = CalculationSessionStatus.ACTIVE, updatedAt = now)
            return existing.id
        }
        val id = "session_${formula.id}_$now"
        histories[id] = CalculationHistory(
            id = id,
            formulaId = formula.id,
            formulaTitle = formula.title,
            formulaDescription = formula.description,
            formulaIsBuiltin = formula.isBuiltin,
            status = com.formuladock.core.model.history.model.CalculationStatus.SUCCESS,
            inputs = emptyList(),
            outputs = emptyList(),
            errorMessage = null,
            errorFieldKey = null,
            note = null,
            createdAt = now,
            updatedAt = now,
            sessionStatus = CalculationSessionStatus.ACTIVE,
            startedAt = now,
            endedAt = null,
            revisionCount = 0,
        )
        return id
    }

    override suspend fun appendRevision(
        sessionId: String,
        history: CalculationHistory,
        revisionNo: Int,
        changedKeys: Set<String>,
    ) {
        val session = histories[sessionId] ?: return
        val revision = CalculationRevision(
            id = history.id,
            sessionId = sessionId,
            revisionNo = revisionNo,
            status = history.status,
            inputs = history.inputs,
            outputs = history.outputs,
            changedKeys = changedKeys,
            errorMessage = history.errorMessage,
            errorFieldKey = history.errorFieldKey,
            createdAt = history.createdAt,
            updatedAt = history.updatedAt,
        )
        histories[sessionId] = session.copy(
            status = history.status,
            inputs = history.inputs,
            outputs = history.outputs,
            errorMessage = history.errorMessage,
            errorFieldKey = history.errorFieldKey,
            updatedAt = history.updatedAt,
            sessionStatus = CalculationSessionStatus.ACTIVE,
            revisionCount = revisionNo,
            revisions = listOf(revision) + session.revisions,
        )
    }

    override suspend fun getLatestRevision(sessionId: String): CalculationRevision? =
        histories[sessionId]?.revisions?.maxByOrNull { it.revisionNo }

    override suspend fun pauseSession(id: String, now: Long) {
        histories[id]?.let { histories[id] = it.copy(sessionStatus = CalculationSessionStatus.PAUSED, updatedAt = now) }
    }

    override suspend fun closeSession(id: String, now: Long) {
        histories[id]?.let {
            histories[id] = it.copy(sessionStatus = CalculationSessionStatus.CLOSED, updatedAt = now, endedAt = now)
        }
    }

    override suspend fun closeExpiredSessions(cutoff: Long) {
        histories.entries.forEach { (id, history) ->
            if (history.sessionStatus != CalculationSessionStatus.CLOSED && history.updatedAt < cutoff) {
                histories[id] = history.copy(
                    sessionStatus = CalculationSessionStatus.CLOSED,
                    endedAt = history.updatedAt,
                )
            }
        }
    }

    override suspend fun updateNote(id: String, note: String?, updatedAt: Long) {
        histories[id]?.let { histories[id] = it.copy(note = note, updatedAt = updatedAt) }
    }

    override suspend fun deleteHistory(id: String) {
        histories.remove(id)
    }

    override suspend fun deleteAllHistories() {
        histories.clear()
    }
}

