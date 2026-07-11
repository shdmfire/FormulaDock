package com.formuladock.core.formula.io

data class FormulaImportOptions(
    val now: Long,
    val includeBuiltinsFromFile: Boolean = false,
)
