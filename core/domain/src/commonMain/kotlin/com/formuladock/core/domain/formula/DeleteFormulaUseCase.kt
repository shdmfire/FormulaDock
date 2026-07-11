package com.formuladock.core.domain.formula

import com.formuladock.core.data.formula.FormulaRepository

class DeleteFormulaUseCase(
    private val repository: FormulaRepository,
) {
    suspend operator fun invoke(id: String) = repository.deleteFormula(id)
}
