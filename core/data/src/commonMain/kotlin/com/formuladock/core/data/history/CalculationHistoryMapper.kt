package com.formuladock.core.data.history

import com.formuladock.core.database.Calculation_history
import com.formuladock.core.database.Calculation_history_input
import com.formuladock.core.database.Calculation_history_output
import com.formuladock.core.model.history.model.CalculationHistory
import com.formuladock.core.model.history.model.CalculationHistoryInput
import com.formuladock.core.model.history.model.CalculationHistoryOutput
import com.formuladock.core.model.history.model.CalculationStatus

internal object CalculationHistoryMapper {
    fun toHistory(
        history: Calculation_history,
        inputs: List<Calculation_history_input>,
        outputs: List<Calculation_history_output>,
    ): CalculationHistory {
        return CalculationHistory(
            id = history.id,
            formulaId = history.formula_id,
            formulaTitle = history.formula_title,
            formulaDescription = history.formula_description,
            formulaIsBuiltin = history.formula_is_builtin.toBooleanFlag(),
            status = CalculationStatus.valueOf(history.status),
            inputs = inputs.map { it.toModel() },
            outputs = outputs.map { it.toModel() },
            errorMessage = history.error_message,
            errorFieldKey = history.error_field_key,
            note = history.note,
            createdAt = history.created_at,
            updatedAt = history.updated_at,
        )
    }

    fun Boolean.toLongFlag(): Long = if (this) 1L else 0L

    private fun Long.toBooleanFlag(): Boolean = this != 0L

    private fun Calculation_history_input.toModel(): CalculationHistoryInput {
        return CalculationHistoryInput(
            id = id,
            key = key,
            label = label,
            rawValue = raw_value,
            numericValue = numeric_value,
            unit = unit,
            required = required.toBooleanFlag(),
            sortOrder = sort_order.toInt(),
        )
    }

    private fun Calculation_history_output.toModel(): CalculationHistoryOutput {
        return CalculationHistoryOutput(
            id = id,
            key = key,
            label = label,
            expression = expression,
            value = value_,
            formattedValue = formatted_value,
            unit = unit,
            precision = precision.toInt(),
            sortOrder = sort_order.toInt(),
        )
    }
}
