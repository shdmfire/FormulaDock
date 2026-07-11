package com.formuladock.core.formula.io

import com.formuladock.core.model.formula.model.FormulaDefinition
import com.formuladock.core.model.formula.model.FormulaInput
import com.formuladock.core.model.formula.model.FormulaOutput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FormulaJsonCodecTest {
    @Test
    fun encodeFiltersBuiltinsByDefault() {
        val json = FormulaJsonCodec.encode(
            formulas = listOf(formula("user", false), formula("builtin", true)),
            now = 100L,
        )

        assertTrue(json.contains("\"schemaVersion\": 1"))
        assertTrue(json.contains("\"exportedAt\": 100"))
        assertTrue(json.contains("user"))
        assertFalse(json.contains("builtin"))
    }

    @Test
    fun decodeImportsAsUserFormula() {
        val content = FormulaJsonCodec.encode(listOf(formula("old", true)), now = 100L, includeBuiltins = true)
        val result = FormulaJsonCodec.decode(
            content = content,
            options = FormulaImportOptions(
                now = 200L,
                includeBuiltinsFromFile = true,
            ),
        )

        val success = assertIs<FormulaImportResult.Success>(result)
        assertEquals("old", success.formulas.single().id)
        assertFalse(success.formulas.single().isBuiltin)
        assertEquals(200L, success.formulas.single().createdAt)
    }

    @Test
    fun includeBuiltinsTrueAllowsExportingBuiltins() {
        val json = FormulaJsonCodec.encode(
            formulas = listOf(formula("user", false), formula("builtin", true)),
            now = 100L,
            includeBuiltins = true,
        )

        assertTrue(json.contains("user"))
        assertTrue(json.contains("builtin"))
    }

    @Test
    fun decodeFailsForInvalidContent() {
        assertIs<FormulaImportResult.Failure>(FormulaJsonCodec.decode("", options()))
        assertIs<FormulaImportResult.Failure>(FormulaJsonCodec.decode("not json", options()))
        assertIs<FormulaImportResult.Failure>(FormulaJsonCodec.decode("""{"schemaVersion":2,"app":"FormulaDock","exportedAt":1,"formulas":[]}""", options()))
        assertIs<FormulaImportResult.Failure>(FormulaJsonCodec.decode("""{"schemaVersion":1,"app":"FormulaDock","exportedAt":1,"formulas":[]}""", options()))
    }

    @Test
    fun decodeSucceedsWithUnknownFields() {
        val content = """
            {
              "schemaVersion": 1,
              "app": "FormulaDock",
              "exportedAt": 100,
              "someExtraField": "extra",
              "formulas": [
                {
                  "id": "f1",
                  "title": "Formula 1",
                  "inputs": [{"id":"i1","key":"x","label":"X","defaultValue":"1"}],
                  "outputs": [{"id":"o1","key":"y","label":"Y","expression":"x","precision":2}],
                  "extraFormulaField": 42,
                  "createdAt": 1,
                  "updatedAt": 1
                }
              ]
            }
        """.trimIndent()
        val result = FormulaJsonCodec.decode(content, options())
        assertIs<FormulaImportResult.Success>(result)
    }

    private fun options() = FormulaImportOptions(1L)

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
