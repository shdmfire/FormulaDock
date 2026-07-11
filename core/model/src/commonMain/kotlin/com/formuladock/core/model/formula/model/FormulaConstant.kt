package com.formuladock.core.model.formula.model

/**
 * Runtime formula constant.
 *
 * [key] is the expression variable name. [label] is the display name.
 */
data class FormulaConstant(
    val id: String,
    val key: String,
    val label: String,
    val value: String,
    val unit: String?,
    val sortOrder: Int,
)
