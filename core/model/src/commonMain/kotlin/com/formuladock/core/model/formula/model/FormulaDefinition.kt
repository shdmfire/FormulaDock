package com.formuladock.core.model.formula.model

/**
 * Runtime formula definition.
 *
 * This model is intentionally independent from database entities, editor state,
 * UI state, repositories, and expression evaluation.
 */
data class FormulaDefinition(
    val id: String,
    val title: String,
    val description: String?,
    val inputs: List<FormulaInput>,
    val constants: List<FormulaConstant>,
    val outputs: List<FormulaOutput>,
    val isBuiltin: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
