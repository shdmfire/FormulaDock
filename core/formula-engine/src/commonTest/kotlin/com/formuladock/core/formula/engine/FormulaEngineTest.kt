package com.formuladock.core.formula.engine

import com.formuladock.core.model.formula.model.BuiltinFormulas
import com.formuladock.core.model.formula.model.FormulaDefinition
import com.formuladock.core.model.formula.model.FormulaInput
import com.formuladock.core.model.formula.model.FormulaOutput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FormulaEngineTest {
    private val engine = DefaultFormulaEngine()

    @Test
    fun evaluatesRoadTripCostPlanner() {
        val result = engine.evaluate(
            formula = BuiltinFormulas.roadTripCost(now = 1L),
            inputValues = emptyMap(), // Uses default values
        )

        val success = assertIs<FormulaEvaluationResult.Success>(result)
        val outputsMap = success.outputs.associate { it.key to it.formattedValue }
        assertEquals("37.50", outputsMap["fuel_needed"])
        assertEquals("110.02", outputsMap["planned_trip_cost"])
        assertEquals("55.01", outputsMap["cost_per_person"])
        assertEquals("86.62", outputsMap["estimated_co2"])
    }

    @Test
    fun evaluatesHomePaintEstimator() {
        val result = engine.evaluate(
            formula = BuiltinFormulas.homePaintEstimator(now = 1L),
            inputValues = emptyMap(), // Uses default values
        )

        val success = assertIs<FormulaEvaluationResult.Success>(result)
        val outputsMap = success.outputs.associate { it.key to it.formattedValue }
        assertEquals("50.40", outputsMap["gross_wall_area"])
        assertEquals("45.50", outputsMap["paintable_area"])
        assertEquals("10.01", outputsMap["required_liters"])
        assertEquals("3", outputsMap["cans_required"])
    }

    @Test
    fun evaluatesFreelanceProjectQuote() {
        val result = engine.evaluate(
            formula = BuiltinFormulas.freelanceProjectQuote(now = 1L),
            inputValues = emptyMap(), // Uses default values
        )

        val success = assertIs<FormulaEvaluationResult.Success>(result)
        val outputsMap = success.outputs.associate { it.key to it.formattedValue }
        assertEquals("2530.00", outputsMap["client_quote"])
        assertEquals("253.00", outputsMap["platform_fee"])
        assertEquals("1661.60", outputsMap["take_home_income"])
    }

    @Test
    fun evaluatesMonthlyHouseholdBudget() {
        val result = engine.evaluate(
            formula = BuiltinFormulas.monthlyHouseholdBudget(now = 1L),
            inputValues = emptyMap(), // Uses default values
        )

        val success = assertIs<FormulaEvaluationResult.Success>(result)
        val outputsMap = success.outputs.associate { it.key to it.formattedValue }
        assertEquals("6400.00", outputsMap["total_expenses"])
        assertEquals("1600.00", outputsMap["target_savings"])
        assertEquals("1600.00", outputsMap["actual_savings"])
        assertEquals("80.00", outputsMap["expense_ratio"])
    }

    @Test
    fun evaluatesHomeElectricityBill() {
        val result = engine.evaluate(
            formula = BuiltinFormulas.homeElectricityBill(now = 1L),
            inputValues = emptyMap(), // Uses default values
        )

        val success = assertIs<FormulaEvaluationResult.Success>(result)
        val outputsMap = success.outputs.associate { it.key to it.formattedValue }
        assertEquals("300.00", outputsMap["total_usage"])
        assertEquals("224.72", outputsMap["monthly_bill"])
        assertEquals("2696.64", outputsMap["annual_bill"])
        assertEquals("126.00", outputsMap["estimated_emissions"])
    }

    @Test
    fun laterOutputCanReferenceEarlierOutput() {
        val formula = FormulaDefinition(
            id = "f",
            title = "Formula",
            description = null,
            inputs = listOf(
                FormulaInput("i", "a", "A", null, null, true, 0),
            ),
            constants = emptyList(),
            outputs = listOf(
                FormulaOutput("o1", "first", "First", "a + 1", null, 0, 0),
                FormulaOutput("o2", "second", "Second", "first * 2", null, 0, 1),
            ),
            isBuiltin = false,
            sortOrder = 0,
            createdAt = 1L,
            updatedAt = 1L,
        )

        val success = assertIs<FormulaEvaluationResult.Success>(
            engine.evaluate(formula, mapOf("a" to "2"))
        )
        assertEquals("3", success.outputs[0].formattedValue)
        assertEquals("6", success.outputs[1].formattedValue)
    }

    @Test
    fun missingRequiredInputReturnsFailure() {
        val formula = FormulaDefinition(
            id = "f",
            title = "Formula",
            description = null,
            inputs = listOf(
                FormulaInput(
                    id = "i",
                    key = "required_field",
                    label = "Required Field",
                    defaultValue = null,
                    unit = null,
                    required = true,
                    sortOrder = 0,
                )
            ),
            constants = emptyList(),
            outputs = listOf(
                FormulaOutput("o", "result", "Result", "required_field * 2", null, 0, 0)
            ),
            isBuiltin = false,
            sortOrder = 0,
            createdAt = 1L,
            updatedAt = 1L,
        )
        val result = engine.evaluate(formula, emptyMap())

        val failure = assertIs<FormulaEvaluationResult.Failure>(result)
        assertEquals("required_field", failure.fieldKey)
    }

    @Test
    fun nonNumericInputReturnsFailureWithFieldKey() {
        val result = engine.evaluate(
            BuiltinFormulas.roadTripCost(now = 1L),
            mapOf("distance_km" to "abc"),
        )

        val failure = assertIs<FormulaEvaluationResult.Failure>(result)
        assertEquals("distance_km", failure.fieldKey)
    }

    @Test
    fun inputKeyWithSurroundingWhitespaceReturnsFailure() {
        val formula = formulaWithOutput(
            inputKey = " amount ",
            expression = "amount",
        )

        val failure = assertIs<FormulaEvaluationResult.Failure>(
            engine.evaluate(formula, mapOf("amount" to "10"))
        )
        assertEquals("Input name must not contain surrounding whitespace.", failure.message)
        assertEquals(" amount ", failure.fieldKey)
    }

    @Test
    fun formatsSupportedPrecisions() {
        val expectedByPrecision = mapOf(
            0 to "12",
            2 to "12.35",
            15 to "12.345678901234500",
        )

        expectedByPrecision.forEach { (precision, expected) ->
            val success = assertIs<FormulaEvaluationResult.Success>(
                engine.evaluate(
                    formulaWithOutput(expression = "12.3456789012345", precision = precision),
                    emptyMap(),
                )
            )
            assertEquals(expected, success.outputs.single().formattedValue)
        }
    }

    @Test
    fun rejectsPrecisionOutsideSupportedRange() {
        listOf(-1, 16).forEach { precision ->
            val failure = assertIs<FormulaEvaluationResult.Failure>(
                engine.evaluate(
                    formulaWithOutput(expression = "1", precision = precision),
                    emptyMap(),
                )
            )
            assertEquals("Output precision must be between 0 and 15.", failure.message)
            assertEquals("result", failure.fieldKey)
        }
    }

    @Test
    fun formatsValueLargerThanLongWithoutOverflow() {
        val success = assertIs<FormulaEvaluationResult.Success>(
            engine.evaluate(
                formulaWithOutput(
                    expression = "9223372036854775807.0",
                    precision = 2,
                ),
                emptyMap(),
            )
        )

        assertEquals("9223372036854775808.00", success.outputs.single().formattedValue)
    }

    @Test
    fun infiniteExpressionResultReturnsFailure() {
        val formula = FormulaDefinition(
            id = "f",
            title = "Formula",
            description = null,
            inputs = listOf(FormulaInput("i", "a", "A", null, null, true, 0)),
            constants = emptyList(),
            outputs = listOf(FormulaOutput("o", "result", "Result", "a / 0", null, 2, 0)),
            isBuiltin = false,
            sortOrder = 0,
            createdAt = 1L,
            updatedAt = 1L,
        )

        val failure = assertIs<FormulaEvaluationResult.Failure>(
            engine.evaluate(formula, mapOf("a" to "1"))
        )
        assertEquals("result", failure.fieldKey)
    }

    private fun formulaWithOutput(
        inputKey: String? = null,
        expression: String,
        precision: Int = 0,
    ) = FormulaDefinition(
        id = "f",
        title = "Formula",
        description = null,
        inputs = inputKey?.let {
            listOf(FormulaInput("i", it, "Amount", null, null, true, 0))
        }.orEmpty(),
        constants = emptyList(),
        outputs = listOf(
            FormulaOutput("o", "result", "Result", expression, null, precision, 0),
        ),
        isBuiltin = false,
        sortOrder = 0,
        createdAt = 1L,
        updatedAt = 1L,
    )
}
