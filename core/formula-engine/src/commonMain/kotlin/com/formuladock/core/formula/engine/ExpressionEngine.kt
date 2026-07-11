package com.formuladock.core.formula.engine

import com.github.murzagalin.evaluator.Constant
import com.github.murzagalin.evaluator.DefaultConstants
import com.github.murzagalin.evaluator.DefaultFunctions
import com.github.murzagalin.evaluator.Evaluator
import com.github.murzagalin.evaluator.Function
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

interface ExpressionEngine {
    fun evaluateDouble(
        expression: String,
        variables: Map<String, Double>,
    ): Double
}

object MultiplatformExpressionEngine : ExpressionEngine {
    private val evaluator = Evaluator(
        constants = DefaultConstants.ALL +
            Constant("PI", PI) +
            Constant("E", E),
        functions = DefaultFunctions.ALL +
            listOf(
                SqrtFunction,
                PowFunction,
                Log10Function,
                PercentAddFunction,
                PercentSubtractFunction,
                PercentOfFunction,
            ),
    )

    override fun evaluateDouble(
        expression: String,
        variables: Map<String, Double>,
    ): Double {
        val normalizedExpression = expression.trim()
        require(normalizedExpression.isNotEmpty()) {
            "Expression must not be empty."
        }

        val value = evaluator.evaluateDouble(normalizedExpression, variables)

        require(value.isFinite()) {
            "Expression result must be finite."
        }

        return value
    }
}

object FormulaExpressionNames {
    private val identifierRegex = Regex("[A-Za-z_][A-Za-z0-9_]*")

    val builtinConstantNames: Set<String> =
        DefaultConstants.ALL
            .mapTo(mutableSetOf()) { it.name } +
                setOf("PI", "E")

    val builtinFunctionNames: Set<String> =
        DefaultFunctions.ALL
            .mapTo(mutableSetOf()) { it.name }

    val customFunctionNames = setOf(
        "sqrt",
        "pow",
        "log10",
        "pctAdd",
        "pctSub",
        "pctOf",
    )

    val reservedNames: Set<String> =
        builtinConstantNames +
                builtinFunctionNames +
                customFunctionNames +
                setOf("true", "false")

    fun isValidIdentifier(name: String): Boolean {
        return identifierRegex.matches(name)
    }

    fun validateIdentifierOrNull(
        name: String,
        kind: String,
    ): String? {
        val trimmedName = name.trim()

        if (trimmedName.isEmpty()) {
            return "$kind name must not be empty."
        }

        if (name != trimmedName) {
            return "$kind name must not contain surrounding whitespace."
        }

        if (!identifierRegex.matches(trimmedName)) {
            return "$kind '$trimmedName' must match [A-Za-z_][A-Za-z0-9_]*."
        }

        if (trimmedName in reservedNames) {
            return "$kind '$trimmedName' is reserved by the expression engine."
        }

        return null
    }
}

private object SqrtFunction : Function("sqrt", 1) {
    override fun invoke(vararg args: Any): Any {
        val x = args.doubleArg(0, name, "x")

        require(x >= 0.0) {
            "sqrt(x) requires x >= 0."
        }

        return sqrt(x)
    }
}

private object PowFunction : Function("pow", 2) {
    override fun invoke(vararg args: Any): Any {
        val base = args.doubleArg(0, name, "base")
        val exponent = args.doubleArg(1, name, "exponent")
        val result = base.pow(exponent)

        require(result.isFinite()) {
            "pow(base, exponent) result must be finite."
        }

        return result
    }
}

private object Log10Function : Function("log10", 1) {
    override fun invoke(vararg args: Any): Any {
        val x = args.doubleArg(0, name, "x")

        require(x > 0.0) {
            "log10(x) requires x > 0."
        }

        return log10(x)
    }
}

private object PercentAddFunction : Function("pctAdd", 2) {
    override fun invoke(vararg args: Any): Any {
        val base = args.doubleArg(0, name, "base")
        val percent = args.doubleArg(1, name, "percent")
        val result = base * (1.0 + percent / 100.0)

        require(result.isFinite()) {
            "pctAdd(base, percent) result must be finite."
        }

        return result
    }
}

private object PercentSubtractFunction : Function("pctSub", 2) {
    override fun invoke(vararg args: Any): Any {
        val base = args.doubleArg(0, name, "base")
        val percent = args.doubleArg(1, name, "percent")
        val result = base * (1.0 - percent / 100.0)

        require(result.isFinite()) {
            "pctSub(base, percent) result must be finite."
        }

        return result
    }
}

private object PercentOfFunction : Function("pctOf", 2) {
    override fun invoke(vararg args: Any): Any {
        val percent = args.doubleArg(0, name, "percent")
        val base = args.doubleArg(1, name, "base")
        val result = base * percent / 100.0

        require(result.isFinite()) {
            "pctOf(percent, base) result must be finite."
        }

        return result
    }
}

private fun Array<out Any>.doubleArg(
    index: Int,
    functionName: String,
    argumentName: String,
): Double {
    return getOrNull(index) as? Double
        ?: throw IllegalArgumentException("$functionName argument '$argumentName' must be a number.")
}
