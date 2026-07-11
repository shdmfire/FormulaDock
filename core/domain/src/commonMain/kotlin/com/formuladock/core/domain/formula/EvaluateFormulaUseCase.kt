package com.formuladock.core.domain.formula

import com.formuladock.core.formula.engine.FormulaEngine
import com.formuladock.core.formula.engine.FormulaEvaluationResult
import com.formuladock.core.model.formula.model.FormulaDefinition

class EvaluateFormulaUseCase(
    private val engine: FormulaEngine,
) {
    operator fun invoke(
        formula: FormulaDefinition,
        inputValues: Map<String, String>,
    ): FormulaEvaluationResult = engine.evaluate(formula, inputValues)
}
