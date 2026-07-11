package com.formuladock.core.formula.io

import kotlinx.serialization.Serializable

@Serializable
data class FormulaDefinitionDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val inputs: List<FormulaInputDto>,
    val constants: List<FormulaConstantDto> = emptyList(),
    val outputs: List<FormulaOutputDto>,
    val isBuiltin: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class FormulaInputDto(
    val id: String,
    val key: String,
    val label: String,
    val defaultValue: String? = null,
    val unit: String? = null,
    val required: Boolean = true,
    val sortOrder: Int = 0,
)

@Serializable
data class FormulaConstantDto(
    val id: String,
    val key: String,
    val label: String,
    val value: String,
    val unit: String? = null,
    val sortOrder: Int = 0,
)

@Serializable
data class FormulaOutputDto(
    val id: String,
    val key: String,
    val label: String,
    val expression: String,
    val unit: String? = null,
    val precision: Int = 2,
    val sortOrder: Int = 0,
)
