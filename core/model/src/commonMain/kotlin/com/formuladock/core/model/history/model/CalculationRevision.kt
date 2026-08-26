package com.formuladock.core.model.history.model

data class CalculationRevision(
    val id: String,
    val sessionId: String,
    val revisionNo: Int,
    val status: CalculationStatus,
    val inputs: List<CalculationHistoryInput>,
    val outputs: List<CalculationHistoryOutput>,
    val changedKeys: Set<String>,
    val errorMessage: String?,
    val errorFieldKey: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
