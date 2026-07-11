package com.formuladock.feature.formula.io

import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.formula.io.FormulaJsonCodec
import com.formuladock.core.model.formula.model.FormulaDefinition
import com.formuladock.core.model.formula.model.FormulaInput
import com.formuladock.core.model.formula.model.FormulaOutput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class FormulaImportUseCaseTest {
    @Test
    fun previewsValidJsonWithNewIds() = kotlinx.coroutines.test.runTest {
        val source = formula("old", isBuiltin = true)
        val content = FormulaJsonCodec.encode(listOf(source), now = 1L, includeBuiltins = true)
        val useCase = FormulaImportUseCase(
            repository = FakeImportRepository(),
            idGenerator = CountingIdGenerator(),
        )

        val result = assertIs<FormulaImportPreviewResult.Success>(
            useCase.preview(FormulaPickedFile("formulas.json", content), now = 2L)
        )
        val imported = result.formulas.single()

        assertEquals(1, result.preview.formulaCount)
        assertFalse(imported.isBuiltin)
        assertNotEquals(source.id, imported.id)
        assertNotEquals(source.inputs.single().id, imported.inputs.single().id)
        assertNotEquals(source.outputs.single().id, imported.outputs.single().id)
    }

    @Test
    fun previewRenamesDuplicateTitle() = kotlinx.coroutines.test.runTest {
        val content = FormulaJsonCodec.encode(listOf(formula("BMI", isBuiltin = false)), now = 1L)
        val repository = FakeImportRepository().apply { saved += formula("BMI", isBuiltin = false) }
        val useCase = FormulaImportUseCase(repository, CountingIdGenerator())

        val result = assertIs<FormulaImportPreviewResult.Success>(
            useCase.preview(FormulaPickedFile("formulas.json", content), now = 2L)
        )

        assertEquals("BMI (导入)", result.formulas.single().title)
        assertEquals(listOf("BMI (导入)"), result.preview.formulaTitles)
    }

    @Test
    fun previewRejectsNonJsonFile() = kotlinx.coroutines.test.runTest {
        val useCase = FormulaImportUseCase(FakeImportRepository(), CountingIdGenerator())

        val result = assertIs<FormulaImportPreviewResult.Failure>(
            useCase.preview(FormulaPickedFile("formulas.txt", "{}"), now = 2L)
        )

        assertEquals(listOf("请选择 .json 文件"), result.errors)
    }

    @Test
    fun confirmImportSavesFormulas() = kotlinx.coroutines.test.runTest {
        val repository = FakeImportRepository()
        val useCase = FormulaImportUseCase(repository, CountingIdGenerator())
        val formulas = listOf(formula("new", isBuiltin = false))

        useCase.importFormulas(formulas)

        assertEquals(formulas, repository.saved)
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

private class CountingIdGenerator : FormulaIdGenerator {
    private var next = 0
    override fun newFormulaId(): String = nextId("formula")
    override fun newInputId(): String = nextId("input")
    override fun newConstantId(): String = nextId("constant")
    override fun newOutputId(): String = nextId("output")
    private fun nextId(prefix: String) = "${prefix}_${next++}"
}

private class FakeImportRepository : FormulaRepository {
    val saved = mutableListOf<FormulaDefinition>()

    override suspend fun countFormulas(): Long = saved.size.toLong()
    override suspend fun getAllFormulas(): List<FormulaDefinition> = saved
    override suspend fun getFormula(id: String): FormulaDefinition? = saved.firstOrNull { it.id == id }
    override suspend fun saveFormula(formula: FormulaDefinition) {
        saved += formula
    }
    override suspend fun deleteFormula(id: String) = Unit
}
