package com.formuladock.core.data.history

import com.formuladock.core.database.Calculation_history
import com.formuladock.core.database.Calculation_history_input
import com.formuladock.core.database.Calculation_history_output
import com.formuladock.core.model.history.model.CalculationHistoryInput
import com.formuladock.core.model.history.model.CalculationHistoryOutput
import com.formuladock.core.model.history.model.CalculationRevision
import com.formuladock.core.model.history.model.CalculationStatus

internal object CalculationHistoryMapper {
    fun toRevision(
        history: Calculation_history,
        inputs: List<Calculation_history_input>,
        outputs: List<Calculation_history_output>,
    ): CalculationRevision {
        return CalculationRevision(
            id = history.id,
            sessionId = history.session_id.orEmpty(),
            revisionNo = history.revision_no.toInt(),
            status = CalculationStatus.valueOf(history.status),
            inputs = inputs.map { it.toModel() },
            outputs = outputs.map { it.toModel() },
            changedKeys = history.changed_keys.toChangedKeys(),
            errorMessage = history.error_message,
            errorFieldKey = history.error_field_key,
            createdAt = history.created_at,
            updatedAt = history.updated_at,
        )
    }

    fun Boolean.toLongFlag(): Long = if (this) 1L else 0L

    fun Set<String>.toStorageValue(): String? =
        takeIf { it.isNotEmpty() }?.sorted()?.joinToString(CHANGED_KEY_SEPARATOR)

    private fun String?.toChangedKeys(): Set<String> =
        this?.split(CHANGED_KEY_SEPARATOR)?.filterTo(linkedSetOf()) { it.isNotBlank() }.orEmpty()

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

    private const val CHANGED_KEY_SEPARATOR = "\u001F"
}
