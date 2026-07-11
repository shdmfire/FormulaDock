package com.formuladock.formula

import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.data.history.CalculationHistoryRepository
import com.formuladock.core.model.formula.model.FormulaDefinition
import com.formuladock.core.model.history.model.CalculationHistory

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

    override suspend fun countHistories(): Long = histories.size.toLong()

    override suspend fun getHistories(limit: Long, offset: Long): List<CalculationHistory> {
        return histories.values.sortedByDescending { it.createdAt }
            .drop(offset.toInt())
            .take(limit.toInt())
    }

    override suspend fun getHistoriesByFormulaId(formulaId: String, limit: Long, offset: Long): List<CalculationHistory> {
        return histories.values.filter { it.formulaId == formulaId }
            .sortedByDescending { it.createdAt }
            .drop(offset.toInt())
            .take(limit.toInt())
    }

    override suspend fun getHistory(id: String): CalculationHistory? = histories[id]

    override suspend fun saveHistory(history: CalculationHistory) {
        histories[history.id] = history
    }

    override suspend fun updateNote(id: String, note: String?, updatedAt: Long) {
        histories[id]?.let {
            histories[id] = it.copy(note = note, updatedAt = updatedAt)
        }
    }

    override suspend fun deleteHistory(id: String) {
        histories.remove(id)
    }

    override suspend fun deleteAllHistories() {
        histories.clear()
    }
}

