package com.formuladock.feature.formula.io

import com.formuladock.core.model.formula.model.FormulaDefinition
import com.formuladock.core.model.formula.model.FormulaInput
import com.formuladock.core.model.formula.model.FormulaOutput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FormulaExportUseCaseTest {
    @Test
    fun exportsOnlyUserFormulas() = kotlinx.coroutines.test.runTest {
        val useCase = FormulaExportUseCase(
            FakeFormulaRepository(listOf(formula("user", false), formula("builtin", true)))
        )

        val payload = assertNotNull(useCase(now = 10L))

        assertEquals(1, payload.count)
        assertTrue(payload.content.contains("user"))
        assertTrue(!payload.content.contains("builtin"))
    }

    @Test
    fun returnsNullWhenNoUserFormulas() = kotlinx.coroutines.test.runTest {
        val useCase = FormulaExportUseCase(FakeFormulaRepository(listOf(formula("builtin", true))))

        assertNull(useCase(now = 10L))
    }

    private fun formula(id: String, isBuiltin: Boolean) = FormulaDefinition(
        id = id,
        title = id,
        description = null,
        inputs = listOf(FormulaInput("input", "x", "X", "1", null, true, 0)),
        constants = emptyList(),
        outputs = listOf(FormulaOutput("output", "y", "Y", "x + 1", null, 2, 0)),
        isBuiltin = isBuiltin,
        sortOrder = 0,
        createdAt = 1L,
        updatedAt = 1L,
    )
}

