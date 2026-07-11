package com.formuladock.core.domain.formula.editor

import com.formuladock.core.formula.engine.FormulaEngine
import com.formuladock.core.formula.engine.FormulaEvaluationResult

class BuildFormulaPreviewUseCase(
    private val formulaEngine: FormulaEngine,
    private val nowProvider: () -> Long,
) {
    operator fun invoke(draft: FormulaEditorDraft): FormulaPreviewState {
        val formula = try {
            draft.toFormulaDefinition(nowProvider())
        } catch (throwable: Throwable) {
            return FormulaPreviewState.Failure(throwable.message ?: "Preview failed.")
        }
        val inputValues = draft.inputs.associate { it.key.trim() to it.defaultValue.trim() }
        return when (val result = formulaEngine.evaluate(formula, inputValues)) {
            is FormulaEvaluationResult.Success -> FormulaPreviewState.Success(
                result.outputs.map {
                    FormulaPreviewOutput(
                        key = it.key,
                        label = it.label,
                        value = it.value,
                        formattedValue = it.formattedValue,
                        unit = it.unit,
                    )
                }
            )
            is FormulaEvaluationResult.Failure -> FormulaPreviewState.Failure(result.message)
        }
    }
}
