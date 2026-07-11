package com.formuladock.core.data.history

import com.formuladock.core.data.history.CalculationHistoryMapper.toLongFlag
import com.formuladock.core.database.FormulaDockDatabase
import com.formuladock.core.model.history.model.CalculationHistory

interface CalculationHistoryRepository {
    suspend fun countHistories(): Long
    suspend fun getHistories(limit: Long, offset: Long): List<CalculationHistory>
    suspend fun getHistoriesByFormulaId(formulaId: String, limit: Long, offset: Long): List<CalculationHistory>
    suspend fun getHistory(id: String): CalculationHistory?
    suspend fun saveHistory(history: CalculationHistory)
    suspend fun updateNote(id: String, note: String?, updatedAt: Long)
    suspend fun deleteHistory(id: String)
    suspend fun deleteAllHistories()
}

class SqlDelightCalculationHistoryRepository(
    private val database: FormulaDockDatabase,
) : CalculationHistoryRepository {
    private val queries = database.formulaQueries

    override suspend fun countHistories(): Long {
        return queries.countCalculationHistories().executeAsOne()
    }

    override suspend fun getHistories(limit: Long, offset: Long): List<CalculationHistory> {
        return queries.selectCalculationHistories(limit, offset).executeAsList().map { history ->
            history.toModelWithDetails()
        }
    }

    override suspend fun getHistoriesByFormulaId(
        formulaId: String,
        limit: Long,
        offset: Long,
    ): List<CalculationHistory> {
        return queries.selectCalculationHistoriesByFormulaId(formulaId, limit, offset)
            .executeAsList()
            .map { history -> history.toModelWithDetails() }
    }

    override suspend fun getHistory(id: String): CalculationHistory? {
        return queries.selectCalculationHistoryById(id).executeAsOneOrNull()?.toModelWithDetails()
    }

    override suspend fun saveHistory(history: CalculationHistory) {
        database.transaction {
            queries.insertCalculationHistory(
                id = history.id,
                formula_id = history.formulaId,
                formula_title = history.formulaTitle,
                formula_description = history.formulaDescription,
                formula_is_builtin = history.formulaIsBuiltin.toLongFlag(),
                status = history.status.name,
                error_message = history.errorMessage,
                error_field_key = history.errorFieldKey,
                note = history.note,
                created_at = history.createdAt,
                updated_at = history.updatedAt,
            )

            queries.deleteCalculationHistoryInputs(history.id)
            queries.deleteCalculationHistoryOutputs(history.id)

            history.inputs.forEach { input ->
                queries.insertCalculationHistoryInput(
                    id = input.id,
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
                    id = output.id,
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
    }

    override suspend fun updateNote(id: String, note: String?, updatedAt: Long) {
        queries.updateCalculationHistoryNote(note = note, updated_at = updatedAt, id = id)
    }

    override suspend fun deleteHistory(id: String) {
        queries.deleteCalculationHistoryById(id)
    }

    override suspend fun deleteAllHistories() {
        queries.deleteAllCalculationHistories()
    }

    private fun com.formuladock.core.database.Calculation_history.toModelWithDetails(): CalculationHistory {
        return CalculationHistoryMapper.toHistory(
            history = this,
            inputs = queries.selectCalculationHistoryInputs(id).executeAsList(),
            outputs = queries.selectCalculationHistoryOutputs(id).executeAsList(),
        )
    }
}
