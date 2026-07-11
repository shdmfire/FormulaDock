package com.formuladock.core.data.formula

import com.formuladock.core.model.formula.model.BuiltinFormulas

class FormulaSeed(
    private val repository: FormulaRepository,
) {
    suspend fun seedIfEmpty(now: Long) {
        if (repository.countFormulas() == 0L) {
            BuiltinFormulas.allSeedFormulas(now).forEach { formula ->
                repository.saveFormula(formula)
            }
        }
    }
}

suspend fun FormulaRepository.seedBuiltinFormulasIfEmpty(now: Long) {
    FormulaSeed(this).seedIfEmpty(now)
}
