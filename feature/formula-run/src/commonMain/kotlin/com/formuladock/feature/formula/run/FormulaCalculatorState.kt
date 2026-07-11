package com.formuladock.feature.formula.run

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.formuladock.core.domain.formula.EvaluateFormulaUseCase
import com.formuladock.core.formula.engine.FormulaEvaluationResult
import com.formuladock.core.model.formula.model.FormulaDefinition

class FormulaCalculatorState internal constructor(
    initialFormula: FormulaDefinition,
    private val evaluateFormula: EvaluateFormulaUseCase,
) {
    var formula by mutableStateOf(initialFormula)
        private set

    var inputValues by mutableStateOf(initialFormula.defaultInputValues())
        private set

    val result: FormulaEvaluationResult
        get() = evaluateFormula(formula, inputValues)

    fun updateFormula(nextFormula: FormulaDefinition) {
        if (nextFormula.id != formula.id) {
            formula = nextFormula
            inputValues = nextFormula.defaultInputValues()
        } else {
            formula = nextFormula
        }
    }

    fun updateInputValues(values: Map<String, String>) {
        inputValues = values
    }

    fun updateInputValue(key: String, value: String) {
        inputValues = inputValues + (key to value)
    }
}

@Composable
fun rememberFormulaCalculatorState(
    formula: FormulaDefinition,
    evaluateFormula: EvaluateFormulaUseCase,
): FormulaCalculatorState {
    val state = remember(evaluateFormula) { FormulaCalculatorState(formula, evaluateFormula) }
    state.updateFormula(formula)
    return state
}

private fun FormulaDefinition.defaultInputValues(): Map<String, String> =
    inputs.associate { it.key to (it.defaultValue ?: "") }
