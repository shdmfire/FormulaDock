package com.formuladock.core.domain.formula.editor

import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.model.formula.model.FormulaDefinition

class SaveFormulaDraftUseCase(
    private val repository: FormulaRepository,
    private val nowProvider: () -> Long,
) {
    suspend operator fun invoke(draft: FormulaEditorDraft): FormulaDefinition {
        val validation = FormulaEditorValidator.validate(draft)
        require(!validation.hasErrors) { "FormulaEditorDraft cannot be saved while validation has errors." }
        val formula = draft.toFormulaDefinition(nowProvider())
        repository.saveFormula(formula)
        return formula
    }
}
