package com.formuladock.core.model.history.model

data class CalculationHistoryInput(
    val id: String,
    val key: String,
    val label: String,
    val rawValue: String?,
    val numericValue: Double?,
    val unit: String?,
    val required: Boolean,
    val sortOrder: Int,
)
