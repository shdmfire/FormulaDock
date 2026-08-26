package com.formuladock.core.data.history

import com.formuladock.core.database.DriverFactory
import com.formuladock.core.database.createDatabase
import com.formuladock.core.model.formula.model.FormulaDefinition
import com.formuladock.core.model.history.model.CalculationHistory
import com.formuladock.core.model.history.model.CalculationStatus
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CalculationSessionRepositoryTest {
    @Test
    fun sessionCollectsMultipleRevisionsAndExposesFinalState() {
        val repository = SqlDelightCalculationHistoryRepository(
            createDatabase(DriverFactory(":memory:"))
        )
        val formula = FormulaDefinition(
            id = "loan",
            title = "Loan",
            description = null,
            inputs = emptyList(),
            constants = emptyList(),
            outputs = emptyList(),
            isBuiltin = true,
            sortOrder = 0,
            createdAt = 0,
            updatedAt = 0,
        )

        runSuspend {
            val sessionId = repository.openOrResumeSession(formula, now = 1_000, resumeTimeoutMillis = 300_000)
            repository.appendRevision(sessionId, revision("r1", 1_100), 1, setOf("rate"))
            repository.appendRevision(sessionId, revision("r2", 1_200), 2, setOf("principal"))
            repository.replaceLatestRevision(sessionId, revision("r2", 1_250), 2, setOf("principal"))
            repository.closeSession(sessionId, now = 1_300)

            val summary = repository.getHistories(limit = 10, offset = 0).single()
            assertEquals(sessionId, summary.id)
            assertEquals(2, summary.revisionCount)
            assertEquals(emptyList(), summary.revisions)

            val detail = repository.getHistory(sessionId)
            assertEquals(listOf(2, 1), detail?.revisions?.map { it.revisionNo })
            assertEquals(1_250, repository.getLatestRevision(sessionId)?.updatedAt)
            assertNotNull(repository.getLatestRevision(sessionId))
        }
    }

    private fun runSuspend(block: suspend () -> Unit) {
        var outcome: Result<Unit>? = null
        block.startCoroutine(object : Continuation<Unit> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) {
                outcome = result
            }
        })
        outcome?.getOrThrow() ?: error("Test coroutine did not complete synchronously")
    }

    private fun revision(id: String, timestamp: Long) = CalculationHistory(
        id = id,
        formulaId = "loan",
        formulaTitle = "Loan",
        formulaDescription = null,
        formulaIsBuiltin = true,
        status = CalculationStatus.SUCCESS,
        inputs = emptyList(),
        outputs = emptyList(),
        errorMessage = null,
        errorFieldKey = null,
        note = null,
        createdAt = timestamp,
        updatedAt = timestamp,
    )
}
