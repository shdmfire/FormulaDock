package com.formuladock.core.domain.formula

import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.domain.formula.editor.toEditorDraft
import com.formuladock.core.domain.formula.editor.toFormulaDefinition
import com.formuladock.core.model.formula.model.FormulaDefinition

class DuplicateFormulaUseCase(
    private val repository: FormulaRepository,
    private val nowProvider: () -> Long,
) {
    suspend operator fun invoke(sourceFormulaId: String): FormulaDefinition? {
        val source = repository.getFormula(sourceFormulaId) ?: return null
        val duplicated = source
            .toEditorDraft(clearIdentity = true)
            .copy(isBuiltin = false, createdAt = null)
            .toFormulaDefinition(nowProvider())
        repository.saveFormula(duplicated)
        return duplicated
    }
}
