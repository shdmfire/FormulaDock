package com.formuladock.core.domain.formula.panel

import com.formuladock.core.formula.engine.FormulaEngine
import com.formuladock.core.formula.engine.FormulaEvaluationResult
import com.formuladock.core.model.formula.model.FormulaDefinition

class CalculatePanelFormulaUseCase(
    private val formulaEngine: FormulaEngine,
) {
    operator fun invoke(
        formula: FormulaDefinition,
        inputValues: Map<String, String>,
    ): FormulaEvaluationResult = formulaEngine.evaluate(formula, inputValues)
}
