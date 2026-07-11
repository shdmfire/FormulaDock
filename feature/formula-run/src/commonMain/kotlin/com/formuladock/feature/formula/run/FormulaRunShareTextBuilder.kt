package com.formuladock.feature.formula.run

import com.formuladock.core.formula.engine.FormulaEvaluationResult
import com.formuladock.core.model.formula.model.FormulaDefinition

fun buildFormulaShareText(
    formula: FormulaDefinition,
    inputValues: Map<String, String>,
    result: FormulaEvaluationResult,
    shareTitleLabel: String,
    inputParamsLabel: String,
    calcResultLabel: String,
): String = buildString {
    appendLine("【FormulaDock】$shareTitleLabel${formula.title}")
    formula.description?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
    if (formula.inputs.isNotEmpty()) {
        appendLine("\n--- $inputParamsLabel ---")
        formula.inputs.forEach { input ->
            appendLine("- ${input.label.ifBlank { input.key }}: ${inputValues[input.key].orEmpty()} ${input.unit.orEmpty()}".trim())
        }
    }
    if (result is FormulaEvaluationResult.Success) {
        appendLine("\n--- $calcResultLabel ---")
        result.outputs.forEach { output ->
            appendLine("- ${output.label.ifBlank { output.key }}: ${output.formattedValue} ${output.unit.orEmpty()}".trim())
        }
    }
}

fun String.asDecimalInput(): String = filterIndexed { index, char ->
    char.isDigit() || char == '.' || char == '-' && index == 0
}
