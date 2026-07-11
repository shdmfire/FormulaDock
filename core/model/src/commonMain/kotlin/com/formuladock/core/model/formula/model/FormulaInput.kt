package com.formuladock.core.model.formula.model

/**
 * Runtime formula input.
 *
 * [key] is the expression variable name (for example: `wallet_received`).
 * [label] is the display name and may contain localized text.
 */
data class FormulaInput(
    val id: String,
    val key: String,
    val label: String,
    val defaultValue: String?,
    val unit: String?,
    val required: Boolean,
    val sortOrder: Int,
)
