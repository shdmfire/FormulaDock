package com.formuladock.core.domain.formula.editor

import com.formuladock.core.data.formula.FormulaRepository

class LoadFormulaEditorDraftUseCase(
    private val repository: FormulaRepository,
) {
    suspend operator fun invoke(mode: FormulaEditorMode): FormulaEditorDraft {
        return when (mode) {
            FormulaEditorMode.Create -> createEmptyFormulaDraft()
            is FormulaEditorMode.Edit -> repository.getFormula(mode.formulaId)?.toEditorDraft()
                ?: createEmptyFormulaDraft()
            is FormulaEditorMode.Duplicate -> repository.getFormula(mode.sourceFormulaId)
                ?.toEditorDraft(clearIdentity = true)
                ?.copy(isBuiltin = false, createdAt = null)
                ?: createEmptyFormulaDraft()
        }
    }
}
