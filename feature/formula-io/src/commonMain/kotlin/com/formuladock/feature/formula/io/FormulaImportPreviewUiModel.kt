package com.formuladock.feature.formula.io

data class FormulaImportPreviewUiModel(
    val formulaCount: Int,
    val formulaTitles: List<String>,
    val warnings: List<String> = emptyList(),
)
