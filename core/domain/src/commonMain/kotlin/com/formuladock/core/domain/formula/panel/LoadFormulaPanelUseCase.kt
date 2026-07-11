package com.formuladock.core.domain.formula.panel

import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.model.formula.model.FormulaDefinition

data class FormulaPanelLoadResult(
    val formulas: List<FormulaDefinition>,
    val selectedFormula: FormulaDefinition?,
)

class LoadFormulaPanelUseCase(
    private val repository: FormulaRepository,
    private val defaultFormulaId: String? = null,
) {
    suspend operator fun invoke(): FormulaPanelLoadResult {
        val formulas = repository.getAllFormulas()
        val selectedFormula = defaultFormulaId
            ?.let { id -> formulas.firstOrNull { it.id == id } }
            ?: formulas.firstOrNull()
        return FormulaPanelLoadResult(
            formulas = formulas,
            selectedFormula = selectedFormula,
        )
    }
}
