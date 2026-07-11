package com.formuladock.core.formula.engine

import com.formuladock.core.model.formula.model.FormulaDefinition
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

interface FormulaEngine {
    fun evaluate(
        formula: FormulaDefinition,
        inputValues: Map<String, String>,
    ): FormulaEvaluationResult
}

class DefaultFormulaEngine(
    private val expressionEngine: ExpressionEngine = MultiplatformExpressionEngine,
) : FormulaEngine {
    override fun evaluate(
        formula: FormulaDefinition,
        inputValues: Map<String, String>,
    ): FormulaEvaluationResult {
        validateFormulaSymbols(formula)?.let { return it }

        val variables = mutableMapOf<String, Double>()

        formula.inputs.sortedBy { it.sortOrder }.forEach { input ->
            val key = input.key.trim()
            val rawValue = inputValues[input.key]?.trim().orEmpty()
                .ifEmpty { input.defaultValue?.trim().orEmpty() }

            if (rawValue.isEmpty()) {
                if (input.required) {
                    return FormulaEvaluationResult.Failure(
                        message = "Missing required input '${input.label}'.",
                        fieldKey = input.key,
                    )
                }
                return@forEach
            }

            val value = rawValue.toDoubleOrNull()
            if (value == null || !value.isFinite()) {
                return FormulaEvaluationResult.Failure(
                    message = "Input '${input.label}' must be a finite number.",
                    fieldKey = input.key,
                )
            }

            variables[key] = value
        }

        formula.constants.sortedBy { it.sortOrder }.forEach { constant ->
            val key = constant.key.trim()
            val value = constant.value.trim().toDoubleOrNull()

            if (value == null || !value.isFinite()) {
                return FormulaEvaluationResult.Failure(
                    message = "Constant '${constant.label}' must be a finite number.",
                    fieldKey = constant.key,
                )
            }

            variables[key] = value
        }

        val outputs = mutableListOf<FormulaEvaluationOutput>()

        formula.outputs.sortedBy { it.sortOrder }.forEach { output ->
            val outputKey = output.key.trim()

            val value = try {
                expressionEngine.evaluateDouble(output.expression, variables)
            } catch (exception: Exception) {
                return FormulaEvaluationResult.Failure(
                    message = exception.message ?: "Failed to evaluate expression.",
                    fieldKey = output.key,
                )
            }

            if (!value.isFinite()) {
                return FormulaEvaluationResult.Failure(
                    message = "Output '${output.label}' result must be finite.",
                    fieldKey = output.key,
                )
            }

            variables[outputKey] = value

            outputs += FormulaEvaluationOutput(
                key = output.key,
                label = output.label,
                value = value,
                formattedValue = value.format(output.precision),
                unit = output.unit,
            )
        }

        return FormulaEvaluationResult.Success(outputs)
    }
}

private fun validateFormulaSymbols(
    formula: FormulaDefinition,
): FormulaEvaluationResult.Failure? {
    val usedNames = mutableSetOf<String>()

    fun validateSymbol(
        key: String,
        kind: String,
        fieldKey: String,
    ): FormulaEvaluationResult.Failure? {
        val name = key

        FormulaExpressionNames.validateIdentifierOrNull(
            name = name,
            kind = kind,
        )?.let { message ->
            return FormulaEvaluationResult.Failure(
                message = message,
                fieldKey = fieldKey,
            )
        }

        if (!usedNames.add(name)) {
            return FormulaEvaluationResult.Failure(
                message = "Duplicate symbol '$name'.",
                fieldKey = fieldKey,
            )
        }

        return null
    }

    formula.inputs.forEach { input ->
        validateSymbol(
            key = input.key,
            kind = "Input",
            fieldKey = input.key,
        )?.let { return it }
    }

    formula.constants.forEach { constant ->
        validateSymbol(
            key = constant.key,
            kind = "Constant",
            fieldKey = constant.key,
        )?.let { return it }
    }

    formula.outputs.forEach { output ->
        validateSymbol(
            key = output.key,
            kind = "Output",
            fieldKey = output.key,
        )?.let { return it }

        if (output.precision !in 0..15) {
            return FormulaEvaluationResult.Failure(
                message = "Output precision must be between 0 and 15.",
                fieldKey = output.key,
            )
        }
    }

    return null
}

private fun Double.format(precision: Int): String {
    require(precision in 0..15) {
        "Output precision must be between 0 and 15."
    }

    val scale = 10.0.pow(precision)

    // At this magnitude a Double has no fractional precision. Avoid Long conversion
    // and scale multiplication, both of which are unsafe for very large values.
    if (abs(this) >= Long.MAX_VALUE.toDouble() || !abs(this * scale).isFinite()) {
        val whole = toExactIntegerString()
        return if (precision == 0) whole else "$whole.${"0".repeat(precision)}"
    }

    val rounded = round(this * scale) / scale

    if (precision == 0) {
        return rounded.toLong().toString()
    }

    val sign = if (rounded < 0) "-" else ""
    val absolute = abs(rounded)
    val wholeNumber = absolute.toLong()
    val whole = wholeNumber.toString()
    val fractionalNumber = round((absolute - wholeNumber) * scale).toLong()
    val fractional = fractionalNumber.toString().padStart(precision, '0')

    return "$sign$whole.$fractional"
}

private fun Double.toExactIntegerString(): String {
    val bits = toBits()
    val negative = bits < 0
    val exponent = ((bits ushr 52) and 0x7ffL).toInt() - 1023
    val significand = (bits and 0x000f_ffff_ffff_ffffL) or (1L shl 52)
    val binaryShift = exponent - 52

    check(binaryShift >= 0) {
        "Large formatted values must have an integral Double representation."
    }

    var decimal = significand.toString()
    repeat(binaryShift) {
        decimal = decimal.multiplyDecimalByTwo()
    }

    return if (negative) "-$decimal" else decimal
}

private fun String.multiplyDecimalByTwo(): String {
    val result = StringBuilder(length + 1)
    var carry = 0

    for (index in lastIndex downTo 0) {
        val doubled = (this[index] - '0') * 2 + carry
        result.append(('0'.code + doubled % 10).toChar())
        carry = doubled / 10
    }

    if (carry != 0) result.append(carry)
    return result.reverse().toString()
}
