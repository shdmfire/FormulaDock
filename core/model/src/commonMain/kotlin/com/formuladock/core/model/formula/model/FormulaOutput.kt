package com.formuladock.core.model.formula.model

/**
 * Runtime formula output.
 *
 * [key] is the expression variable name and may be referenced by later outputs.
 * [expression] is stored as text only; evaluation is not part of the runtime model.
 */
data class FormulaOutput(
    val id: String,
    val key: String,
    val label: String,
    val expression: String,
    val unit: String?,
    val precision: Int,
    val sortOrder: Int,
)
