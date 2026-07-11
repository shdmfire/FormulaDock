package com.formuladock.core.domain.formula

import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.model.formula.model.BuiltinFormulas

class SeedBuiltinFormulasUseCase(
    private val repository: FormulaRepository,
    private val nowProvider: () -> Long,
) {
    suspend operator fun invoke() {
        if (repository.countFormulas() == 0L) {
            BuiltinFormulas.allSeedFormulas(nowProvider()).forEach { formula ->
                repository.saveFormula(formula)
            }
        }
    }
}
