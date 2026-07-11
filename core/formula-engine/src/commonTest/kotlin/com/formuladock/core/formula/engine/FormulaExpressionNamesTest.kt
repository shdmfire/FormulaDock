package com.formuladock.core.formula.engine

import com.github.murzagalin.evaluator.DefaultConstants
import com.github.murzagalin.evaluator.DefaultFunctions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FormulaExpressionNamesTest {

    @Test
    fun builtinConstantNamesAreComplete() {
        assertEquals(
            expected = setOf(
                "pi",
                "e",
                "PI",
                "E",
            ),
            actual = FormulaExpressionNames.builtinConstantNames,
        )
    }

    @Test
    fun builtinFunctionNamesAreComplete() {
        assertEquals(
            expected = setOf(
                "abs",
                "acos",
                "asin",
                "atan",
                "avg",
                "ceil",
                "cos",
                "cosh",
                "floor",
                "ln",
                "log",
                "max",
                "min",
                "round",
                "sin",
                "sinh",
                "sum",
                "tan",
                "tanh",
            ),
            actual = FormulaExpressionNames.builtinFunctionNames,
        )
    }

    @Test
    fun customFunctionNamesAreComplete() {
        assertEquals(
            expected = setOf(
                "sqrt",
                "pow",
                "log10",
                "pctAdd",
                "pctSub",
                "pctOf",
            ),
            actual = FormulaExpressionNames.customFunctionNames,
        )
    }

    @Test
    fun builtinFunctionNamesMatchEvaluatorDependency() {
        val dependencyNames = DefaultFunctions.ALL
            .mapTo(mutableSetOf()) { function ->
                function.name
            }

        assertEquals(
            expected = dependencyNames,
            actual = FormulaExpressionNames.builtinFunctionNames,
            message = """
                FormulaExpressionNames.builtinFunctionNames 与表达式依赖不一致。
                依赖升级后，请同步检查新增、删除或重命名的函数。
            """.trimIndent(),
        )
    }

    @Test
    fun builtinConstantNamesMatchEvaluatorDependencyAndCustomAliases() {
        val dependencyNames = DefaultConstants.ALL
            .mapTo(mutableSetOf()) { constant ->
                constant.name
            }

        val expectedNames = dependencyNames + setOf(
            "PI",
            "E",
        )

        assertEquals(
            expected = expectedNames,
            actual = FormulaExpressionNames.builtinConstantNames,
            message = """
                builtinConstantNames 应包含依赖自带常量，
                以及 MultiplatformExpressionEngine 添加的 PI/E。
            """.trimIndent(),
        )
    }

    @Test
    fun reservedNamesAreExactUnionOfAllEngineNamesAndBooleanLiterals() {
        val expected = FormulaExpressionNames.builtinConstantNames +
                FormulaExpressionNames.builtinFunctionNames +
                FormulaExpressionNames.customFunctionNames +
                setOf("true", "false")

        assertEquals(
            expected = expected,
            actual = FormulaExpressionNames.reservedNames,
        )
    }

    @Test
    fun nameGroupsDoNotOverlapUnexpectedly() {
        assertTrue(
            FormulaExpressionNames.builtinConstantNames
                .intersect(FormulaExpressionNames.builtinFunctionNames)
                .isEmpty(),
        )

        assertTrue(
            FormulaExpressionNames.builtinConstantNames
                .intersect(FormulaExpressionNames.customFunctionNames)
                .isEmpty(),
        )

        assertTrue(
            FormulaExpressionNames.builtinFunctionNames
                .intersect(FormulaExpressionNames.customFunctionNames)
                .isEmpty(),
        )
    }

    @Test
    fun acceptsValidIdentifiers() {
        val validNames = listOf(
            "x",
            "_x",
            "_",
            "value1",
            "price_in_usd",
            "ABC",
            "result_2026",
            "_123",
        )

        validNames.forEach { name ->
            assertTrue(
                actual = FormulaExpressionNames.isValidIdentifier(name),
                message = "'$name' 应当是合法标识符。",
            )

            assertNull(
                actual = FormulaExpressionNames.validateIdentifierOrNull(
                    name = name,
                    kind = "Input",
                ),
                message = "'$name' 应当通过标识符验证。",
            )
        }
    }

    @Test
    fun rejectsInvalidIdentifiers() {
        val invalidNames = listOf(
            "",
            " ",
            "1value",
            "123",
            "price-value",
            "price value",
            "price.value",
            "value%",
            "a+b",
            "中文",
            "é",
            "value\nname",
        )

        invalidNames.forEach { name ->
            assertFalse(
                actual = FormulaExpressionNames.isValidIdentifier(name),
                message = "'$name' 不应是合法标识符。",
            )
        }
    }

    @Test
    fun returnsEmptyNameError() {
        assertEquals(
            expected = "Input name must not be empty.",
            actual = FormulaExpressionNames.validateIdentifierOrNull(
                name = "   ",
                kind = "Input",
            ),
        )
    }

    @Test
    fun returnsPatternError() {
        assertEquals(
            expected = "Output 'total-price' must match [A-Za-z_][A-Za-z0-9_]*.",
            actual = FormulaExpressionNames.validateIdentifierOrNull(
                name = "total-price",
                kind = "Output",
            ),
        )
    }

    @Test
    fun rejectsEveryReservedName() {
        FormulaExpressionNames.reservedNames.forEach { name ->
            val error = FormulaExpressionNames.validateIdentifierOrNull(
                name = name,
                kind = "Symbol",
            )

            assertNotNull(
                actual = error,
                message = "'$name' 应当是保留名称。",
            )

            assertEquals(
                expected = "Symbol '$name' is reserved by the expression engine.",
                actual = error,
            )
        }
    }

    @Test
    fun everyReservedNameIsAlsoSyntacticallyValid() {
        FormulaExpressionNames.reservedNames.forEach { name ->
            assertTrue(
                actual = FormulaExpressionNames.isValidIdentifier(name),
                message = "保留名称 '$name' 本身应符合标识符语法。",
            )
        }
    }

    @Test
    fun validationRejectsSurroundingWhitespace() {
        assertEquals(
            expected = "Input name must not contain surrounding whitespace.",
            actual = FormulaExpressionNames.validateIdentifierOrNull(
                name = "  amount  ",
                kind = "Input",
            ),
        )
    }

    @Test
    fun reservedNameValidationIsCaseSensitive() {
        // 当前实现中只有精确匹配才视为保留名称。
        // 因此 Pi、Abs、TRUE 都是可使用的变量名。
        listOf(
            "Pi",
            "Abs",
            "TRUE",
            "False",
            "PctAdd",
        ).forEach { name ->
            assertNull(
                actual = FormulaExpressionNames.validateIdentifierOrNull(
                    name = name,
                    kind = "Input",
                ),
                message = "'$name' 在当前大小写敏感规则下应当合法。",
            )
        }
    }
}