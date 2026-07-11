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

suspend fun CalculationHistoryRepository.saveSuccessfulRun(
    formula: FormulaDefinition,
    inputValues: Map<String, String>,
    result: FormulaEvaluationResult.Success,
) {
    val timestamp = currentTimeMillis()
    val historyId = "history_$timestamp"
    saveHistory(
        CalculationHistory(
            id = historyId,
            formulaId = formula.id,
            formulaTitle = formula.title,
            formulaDescription = formula.description,
            formulaIsBuiltin = formula.isBuiltin,
            status = CalculationStatus.SUCCESS,
            inputs = formula.inputs.mapIndexed { index, input ->
                val rawValue = inputValues[input.key]
                CalculationHistoryInput(
                    id = "${historyId}_input_${input.key}",
                    key = input.key,
                    label = input.label,
                    rawValue = rawValue,
                    numericValue = rawValue?.toDoubleOrNull(),
                    unit = input.unit,
                    required = input.required,
                    sortOrder = index,
                )
            },
            outputs = result.outputs.mapIndexed { index, output -> output.toHistoryOutput(historyId, formula, index) },
            errorMessage = null,
            errorFieldKey = null,
            note = null,
            createdAt = timestamp,
            updatedAt = timestamp,
        )
    )
}

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
