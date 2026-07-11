package com.formuladock.core.formula.io

import com.formuladock.core.model.formula.model.FormulaDefinition

sealed interface FormulaImportResult {
    data class Success(
        val formulas: List<FormulaDefinition>,
        val warnings: List<String> = emptyList(),
    ) : FormulaImportResult

    data class Failure(
        val errors: List<String>,
    ) : FormulaImportResult
}
