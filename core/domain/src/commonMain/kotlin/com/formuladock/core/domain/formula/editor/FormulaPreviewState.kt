package com.formuladock.core.domain.formula.editor

sealed interface FormulaPreviewState {
    data object Empty : FormulaPreviewState

    data class Success(
        val outputs: List<FormulaPreviewOutput>,
    ) : FormulaPreviewState

    data class Failure(
        val message: String,
    ) : FormulaPreviewState
}

data class FormulaPreviewOutput(
    val key: String,
    val label: String,
    val value: Double,
    val formattedValue: String,
    val unit: String?,
)
