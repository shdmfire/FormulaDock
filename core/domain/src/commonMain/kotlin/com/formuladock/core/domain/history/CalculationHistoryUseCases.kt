package com.formuladock.core.domain.history

import com.formuladock.core.data.history.CalculationHistoryRepository
import com.formuladock.core.model.history.model.CalculationHistory

class GetCalculationHistoryListUseCase(
    private val repository: CalculationHistoryRepository,
) {
    suspend operator fun invoke(
        formulaId: String? = null,
        limit: Long = 100,
        offset: Long = 0,
    ): List<CalculationHistory> {
        return if (formulaId.isNullOrBlank()) {
            repository.getHistories(limit = limit, offset = offset)
        } else {
            repository.getHistoriesByFormulaId(formulaId = formulaId, limit = limit, offset = offset)
        }
    }
}

class GetCalculationHistoryUseCase(
    private val repository: CalculationHistoryRepository,
) {
    suspend operator fun invoke(id: String): CalculationHistory? {
        return repository.getHistory(id)
    }
}

class DeleteCalculationHistoryUseCase(
    private val repository: CalculationHistoryRepository,
) {
    suspend operator fun invoke(id: String) {
        repository.deleteHistory(id)
    }
}

class UpdateCalculationHistoryNoteUseCase(
    private val repository: CalculationHistoryRepository,
) {
    suspend operator fun invoke(id: String, note: String?, updatedAt: Long) {
        repository.updateNote(id = id, note = note?.takeIf { it.isNotBlank() }, updatedAt = updatedAt)
    }
}
