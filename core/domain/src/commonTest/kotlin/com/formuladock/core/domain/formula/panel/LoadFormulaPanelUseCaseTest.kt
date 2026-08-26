package com.formuladock.core.domain.formula.panel

import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.model.formula.model.FormulaDefinition
import com.formuladock.core.model.formula.model.FormulaInput
import com.formuladock.core.model.formula.model.FormulaOutput
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LoadFormulaPanelUseCaseTest {

    @Test
    fun loadsFormulasAndSelectsDefaultFormulaWhenProvided() = runTest {
        val formula1 = createFormula("id-1", "Formula 1")
        val formula2 = createFormula("id-2", "Formula 2")
        val repository = FakeFormulaRepository(listOf(formula1, formula2))

        val useCase = LoadFormulaPanelUseCase(repository, defaultFormulaId = "id-2")
        val result = useCase()

        assertEquals(2, result.formulas.size)
        assertEquals("id-2", result.selectedFormula?.id)
    }

    @Test
    fun loadsFormulasAndFallsBackToFirstFormulaWhenDefaultFormulaIdIsNull() = runTest {
        val formula1 = createFormula("id-1", "Formula 1")
        val formula2 = createFormula("id-2", "Formula 2")
        val repository = FakeFormulaRepository(listOf(formula1, formula2))

        val useCase = LoadFormulaPanelUseCase(repository, defaultFormulaId = null)
        val result = useCase()

        assertEquals(2, result.formulas.size)
        assertEquals("id-1", result.selectedFormula?.id)
    }

    @Test
    fun loadsFormulasAndFallsBackToFirstFormulaWhenDefaultFormulaIdNotFound() = runTest {
        val formula1 = createFormula("id-1", "Formula 1")
        val formula2 = createFormula("id-2", "Formula 2")
        val repository = FakeFormulaRepository(listOf(formula1, formula2))

        val useCase = LoadFormulaPanelUseCase(repository, defaultFormulaId = "non-existent")
        val result = useCase()

        assertEquals("id-1", result.selectedFormula?.id)
    }

    private fun createFormula(id: String, title: String) = FormulaDefinition(
        id = id,
        title = title,
        description = null,
        inputs = listOf(FormulaInput("in-1", "x", "X", "1", null, true, 0)),
        constants = emptyList(),
        outputs = listOf(FormulaOutput("out-1", "y", "Y", "x", null, 2, 0)),
        isBuiltin = false,
        sortOrder = 0,
        createdAt = 100L,
        updatedAt = 100L,
    )

    private class FakeFormulaRepository(
        private val formulas: List<FormulaDefinition>,
    ) : FormulaRepository {
        override suspend fun countFormulas(): Long = formulas.size.toLong()
        override suspend fun getAllFormulas(): List<FormulaDefinition> = formulas
        override suspend fun getFormula(id: String): FormulaDefinition? = formulas.firstOrNull { it.id == id }
        override suspend fun saveFormula(formula: FormulaDefinition) = Unit
        override suspend fun deleteFormula(id: String) = Unit
    }
}
