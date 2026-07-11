package com.formuladock.core.formula.engine

import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cosh
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.tanh
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MultiplatformExpressionEngineTest {

    private val engine: ExpressionEngine = MultiplatformExpressionEngine

    @Test
    fun evaluatesEveryBuiltinConstant() {
        val cases = listOf(
            CalculationCase(
                name = "pi",
                expression = "pi",
                expected = PI,
            ),
            CalculationCase(
                name = "e",
                expression = "e",
                expected = E,
            ),
            CalculationCase(
                name = "PI",
                expression = "PI",
                expected = PI,
            ),
            CalculationCase(
                name = "E",
                expression = "E",
                expected = E,
            ),
        )

        assertEquals(
            expected = FormulaExpressionNames.builtinConstantNames,
            actual = cases.mapTo(mutableSetOf()) { it.name },
            message = "测试案例必须覆盖全部 builtinConstantNames。",
        )

        cases.forEach(::assertCalculation)
    }

    @Test
    fun evaluatesEveryBuiltinFunction() {
        val cases = listOf(
            CalculationCase(
                name = "abs",
                expression = "abs(-12.5)",
                expected = 12.5,
            ),
            CalculationCase(
                name = "acos",
                expression = "acos(1)",
                expected = acos(1.0),
            ),
            CalculationCase(
                name = "asin",
                expression = "asin(1)",
                expected = asin(1.0),
            ),
            CalculationCase(
                name = "atan",
                expression = "atan(1)",
                expected = atan(1.0),
            ),
            CalculationCase(
                name = "avg",
                expression = "avg(2, 4, 6, 8)",
                expected = 5.0,
            ),
            CalculationCase(
                name = "ceil",
                expression = "ceil(1.2)",
                expected = 2.0,
            ),
            CalculationCase(
                name = "cos",
                expression = "cos(PI)",
                expected = -1.0,
            ),
            CalculationCase(
                name = "cosh",
                expression = "cosh(1)",
                expected = cosh(1.0),
            ),
            CalculationCase(
                name = "floor",
                expression = "floor(1.8)",
                expected = 1.0,
            ),
            CalculationCase(
                name = "ln",
                expression = "ln(E)",
                expected = ln(E),
            ),
            CalculationCase(
                name = "log",
                expression = "log(1000, 10)",
                expected = 3.0,
            ),
            CalculationCase(
                name = "max",
                expression = "max(-8, 12, 4, 9)",
                expected = 12.0,
            ),
            CalculationCase(
                name = "min",
                expression = "min(-8, 12, 4, 9)",
                expected = -8.0,
            ),
            CalculationCase(
                name = "round",
                expression = "round(2.6)",
                expected = 3.0,
            ),
            CalculationCase(
                name = "sin",
                expression = "sin(PI / 2)",
                expected = 1.0,
            ),
            CalculationCase(
                name = "sinh",
                expression = "sinh(1)",
                expected = sinh(1.0),
            ),
            CalculationCase(
                name = "sum",
                expression = "sum(1, 2, 3, 4, 5)",
                expected = 15.0,
            ),
            CalculationCase(
                name = "tan",
                expression = "tan(PI / 4)",
                expected = 1.0,
            ),
            CalculationCase(
                name = "tanh",
                expression = "tanh(1)",
                expected = tanh(1.0),
            ),
        )

        assertEquals(
            expected = FormulaExpressionNames.builtinFunctionNames,
            actual = cases.mapTo(mutableSetOf()) { it.name },
            message = "测试案例必须覆盖全部 builtinFunctionNames。",
        )

        cases.forEach(::assertCalculation)
    }

    @Test
    fun evaluatesEveryCustomFunction() {
        val cases = listOf(
            CalculationCase(
                name = "sqrt",
                expression = "sqrt(81)",
                expected = 9.0,
            ),
            CalculationCase(
                name = "pow",
                expression = "pow(2, 10)",
                expected = 1024.0,
            ),
            CalculationCase(
                name = "log10",
                expression = "log10(1000)",
                expected = 3.0,
            ),
            CalculationCase(
                name = "pctAdd",
                expression = "pctAdd(200, 15)",
                expected = 230.0,
            ),
            CalculationCase(
                name = "pctSub",
                expression = "pctSub(200, 15)",
                expected = 170.0,
            ),
            CalculationCase(
                name = "pctOf",
                expression = "pctOf(15, 200)",
                expected = 30.0,
            ),
        )

        assertEquals(
            expected = FormulaExpressionNames.customFunctionNames,
            actual = cases.mapTo(mutableSetOf()) { it.name },
            message = "测试案例必须覆盖全部 customFunctionNames。",
        )

        cases.forEach(::assertCalculation)
    }

    @Test
    fun evaluatesBasicOperatorsWithCorrectPrecedence() {
        val cases = listOf(
            CalculationCase(
                name = "addition",
                expression = "2 + 3",
                expected = 5.0,
            ),
            CalculationCase(
                name = "subtraction",
                expression = "10 - 4",
                expected = 6.0,
            ),
            CalculationCase(
                name = "multiplication precedence",
                expression = "2 + 3 * 4",
                expected = 14.0,
            ),
            CalculationCase(
                name = "parentheses",
                expression = "(2 + 3) * 4",
                expected = 20.0,
            ),
            CalculationCase(
                name = "division",
                expression = "20 / 4",
                expected = 5.0,
            ),
            CalculationCase(
                name = "modulo",
                expression = "17 % 5",
                expected = 2.0,
            ),
            CalculationCase(
                name = "power",
                expression = "2 ^ 10",
                expected = 1024.0,
            ),
            CalculationCase(
                name = "unary minus",
                expression = "-5 + 8",
                expected = 3.0,
            ),
        )

        cases.forEach(::assertCalculation)
    }

    @Test
    fun evaluatesVariables() {
        val result = engine.evaluateDouble(
            expression = "price * quantity + shipping",
            variables = mapOf(
                "price" to 19.99,
                "quantity" to 3.0,
                "shipping" to 5.0,
            ),
        )

        assertClose(
            expected = 64.97,
            actual = result,
            description = "price * quantity + shipping",
        )
    }

    @Test
    fun evaluatesPythagoreanCalculation() {
        val result = engine.evaluateDouble(
            expression = "sqrt(pow(x, 2) + pow(y, 2))",
            variables = mapOf(
                "x" to 3.0,
                "y" to 4.0,
            ),
        )

        assertClose(
            expected = 5.0,
            actual = result,
            description = "Pythagorean calculation",
        )
    }

    @Test
    fun evaluatesCircleArea() {
        val result = engine.evaluateDouble(
            expression = "PI * pow(radius, 2)",
            variables = mapOf(
                "radius" to 3.0,
            ),
        )

        assertClose(
            expected = PI * 9.0,
            actual = result,
            description = "Circle area",
        )
    }

    @Test
    fun evaluatesCompoundPercentageCalculation() {
        val result = engine.evaluateDouble(
            expression = "pctAdd(principal * pow(1 + rate / 100, years), feePercent)",
            variables = mapOf(
                "principal" to 1000.0,
                "rate" to 5.0,
                "years" to 2.0,
                "feePercent" to 10.0,
            ),
        )

        // 1000 × 1.05² = 1102.5
        // 再增加 10% = 1212.75
        assertClose(
            expected = 1212.75,
            actual = result,
            description = "Compound percentage calculation",
        )
    }

    @Test
    fun evaluatesTrigonometricIdentity() {
        val result = engine.evaluateDouble(
            expression = "pow(sin(angle), 2) + pow(cos(angle), 2)",
            variables = mapOf(
                "angle" to 0.731,
            ),
        )

        assertClose(
            expected = 1.0,
            actual = result,
            description = "sin²(x) + cos²(x)",
        )
    }

    @Test
    fun handlesNegativePercentage() {
        assertClose(
            expected = 90.0,
            actual = engine.evaluateDouble(
                expression = "pctAdd(100, -10)",
                variables = emptyMap(),
            ),
            description = "pctAdd with negative percentage",
        )

        assertClose(
            expected = 110.0,
            actual = engine.evaluateDouble(
                expression = "pctSub(100, -10)",
                variables = emptyMap(),
            ),
            description = "pctSub with negative percentage",
        )
    }

    @Test
    fun rejectsBlankExpression() {
        val exception = assertFailsWith<IllegalArgumentException> {
            engine.evaluateDouble(
                expression = "   ",
                variables = emptyMap(),
            )
        }

        assertEquals(
            expected = "Expression must not be empty.",
            actual = exception.message,
        )
    }

    @Test
    fun rejectsNonFiniteExpressionResults() {
        listOf(
            "1 / 0",
            "ln(0)",
            "asin(2)",
            "pow(-1, 0.5)",
        ).forEach { expression ->
            assertFails(
                message = "表达式应当失败：$expression",
            ) {
                engine.evaluateDouble(
                    expression = expression,
                    variables = emptyMap(),
                )
            }
        }
    }

    @Test
    fun rejectsNonFiniteVariableResult() {
        assertFailsWith<IllegalArgumentException> {
            engine.evaluateDouble(
                expression = "value",
                variables = mapOf(
                    "value" to Double.NaN,
                ),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            engine.evaluateDouble(
                expression = "value",
                variables = mapOf(
                    "value" to Double.POSITIVE_INFINITY,
                ),
            )
        }
    }

    @Test
    fun rejectsInvalidCustomFunctionDomains() {
        val sqrtException = assertFailsWith<IllegalArgumentException> {
            engine.evaluateDouble(
                expression = "sqrt(-1)",
                variables = emptyMap(),
            )
        }

        assertTrue(
            sqrtException.message.orEmpty().contains("x >= 0"),
        )

        val logException = assertFailsWith<IllegalArgumentException> {
            engine.evaluateDouble(
                expression = "log10(0)",
                variables = emptyMap(),
            )
        }

        assertTrue(
            logException.message.orEmpty().contains("x > 0"),
        )
    }

    @Test
    fun rejectsOverflowingCustomFunctions() {
        listOf(
            "pow(10, 10000)",
            "pctAdd(1e308, 100)",
            "pctSub(-1e308, 1000)",
            "pctOf(1000, 1e308)",
        ).forEach { expression ->
            assertFails(
                message = "溢出表达式应当失败：$expression",
            ) {
                engine.evaluateDouble(
                    expression = expression,
                    variables = emptyMap(),
                )
            }
        }
    }

    @Test
    fun rejectsUnknownVariables() {
        assertFails {
            engine.evaluateDouble(
                expression = "known + unknown",
                variables = mapOf(
                    "known" to 1.0,
                ),
            )
        }
    }

    @Test
    fun rejectsWrongFunctionArgumentCounts() {
        listOf(
            "sqrt()",
            "sqrt(4, 9)",
            "pow(2)",
            "log(10)",
            "avg(1)",
            "sum(1)",
            "min(1)",
            "max(1)",
            "pctAdd(100)",
            "pctOf(10)",
        ).forEach { expression ->
            assertFails(
                message = "参数数量错误的表达式应当失败：$expression",
            ) {
                engine.evaluateDouble(
                    expression = expression,
                    variables = emptyMap(),
                )
            }
        }
    }

    private fun assertCalculation(case: CalculationCase) {
        val actual = engine.evaluateDouble(
            expression = case.expression,
            variables = case.variables,
        )

        assertClose(
            expected = case.expected,
            actual = actual,
            description = "${case.name}: ${case.expression}",
        )
    }
}

private data class CalculationCase(
    val name: String,
    val expression: String,
    val expected: Double,
    val variables: Map<String, Double> = emptyMap(),
)

private fun assertClose(
    expected: Double,
    actual: Double,
    description: String,
) {
    val tolerance = TEST_EPSILON * max(1.0, abs(expected))
    val difference = abs(expected - actual)

    assertTrue(
        actual = difference <= tolerance,
        message = buildString {
            append(description)
            append(": expected=")
            append(expected)
            append(", actual=")
            append(actual)
            append(", difference=")
            append(difference)
            append(", tolerance=")
            append(tolerance)
        },
    )
}

private const val TEST_EPSILON = 1e-10