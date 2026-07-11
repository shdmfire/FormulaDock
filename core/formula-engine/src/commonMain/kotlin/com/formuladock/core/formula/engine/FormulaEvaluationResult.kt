package com.formuladock.core.formula.engine

sealed interface FormulaEvaluationResult {
    data class Success(
        val outputs: List<FormulaEvaluationOutput>,
    ) : FormulaEvaluationResult

    data class Failure(
        val message: String,
        val fieldKey: String? = null,
    ) : FormulaEvaluationResult
}

data class FormulaEvaluationOutput(
    val key: String,
    val label: String,
    val value: Double,
    val formattedValue: String,
    val unit: String?,
)
