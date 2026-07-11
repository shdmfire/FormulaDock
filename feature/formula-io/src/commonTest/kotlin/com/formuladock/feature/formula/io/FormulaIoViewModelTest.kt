package com.formuladock.feature.formula.io

import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.model.formula.model.FormulaDefinition
import com.formuladock.core.model.formula.model.FormulaInput
import com.formuladock.core.model.formula.model.FormulaOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class FormulaIoViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsExportFormulas() = runTest {
        val viewModel = FormulaIoViewModel(
            repository = FakeFormulaRepository(listOf(formula("user", false), formula("builtin", true))),
        )

        dispatcher.scheduler.runCurrent()

        assertIs<ExportPanelState.Ready>(viewModel.state.value.exportState)
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

class FakeFormulaRepository(
    private val formulas: List<FormulaDefinition>,
) : FormulaRepository {
    override suspend fun countFormulas(): Long = formulas.size.toLong()
    override suspend fun getAllFormulas(): List<FormulaDefinition> = formulas
    override suspend fun getFormula(id: String): FormulaDefinition? = formulas.firstOrNull { it.id == id }
    override suspend fun saveFormula(formula: FormulaDefinition) = Unit
    override suspend fun deleteFormula(id: String) = Unit
}
