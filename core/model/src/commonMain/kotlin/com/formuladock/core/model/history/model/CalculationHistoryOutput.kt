package com.formuladock.core.model.history.model

data class CalculationHistoryOutput(
    val id: String,
    val key: String,
    val label: String,
    val expression: String,
    val value: Double,
    val formattedValue: String,
    val unit: String?,
    val precision: Int,
    val sortOrder: Int,
)
