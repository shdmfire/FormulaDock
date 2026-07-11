package com.formuladock.core.domain.formula

import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.model.formula.model.FormulaDefinition

class GetFormulaUseCase(
    private val repository: FormulaRepository,
) {
    suspend operator fun invoke(id: String): FormulaDefinition? = repository.getFormula(id)
}
