package com.formuladock.core.domain.formula.editor

sealed interface FormulaEditorMode {
    data object Create : FormulaEditorMode
    data class Edit(val formulaId: String) : FormulaEditorMode
    data class Duplicate(val sourceFormulaId: String) : FormulaEditorMode
}
