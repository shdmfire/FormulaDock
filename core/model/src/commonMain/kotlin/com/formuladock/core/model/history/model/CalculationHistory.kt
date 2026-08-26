package com.formuladock.core.model.history.model

data class CalculationHistory(
    val id: String,
    val formulaId: String,
    val formulaTitle: String,
    val formulaDescription: String?,
    val formulaIsBuiltin: Boolean,
    val status: CalculationStatus,
    val inputs: List<CalculationHistoryInput>,
    val outputs: List<CalculationHistoryOutput>,
    val errorMessage: String?,
    val errorFieldKey: String?,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val sessionStatus: CalculationSessionStatus = CalculationSessionStatus.CLOSED,
    val startedAt: Long = createdAt,
    val endedAt: Long? = updatedAt,
    val revisionCount: Int = 1,
    val revisions: List<CalculationRevision> = emptyList(),
)
