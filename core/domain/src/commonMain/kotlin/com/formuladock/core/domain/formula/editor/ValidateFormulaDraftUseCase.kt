package com.formuladock.core.domain.formula.editor

class ValidateFormulaDraftUseCase {
    operator fun invoke(draft: FormulaEditorDraft): FormulaValidationReport {
        return FormulaEditorValidator.validate(draft)
    }
}
