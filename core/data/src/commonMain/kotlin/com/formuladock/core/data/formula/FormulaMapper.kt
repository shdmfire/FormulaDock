package com.formuladock.core.data.formula

import com.formuladock.core.database.Formula
import com.formuladock.core.database.Formula_symbol
import com.formuladock.core.model.formula.model.FormulaConstant
import com.formuladock.core.model.formula.model.FormulaDefinition
import com.formuladock.core.model.formula.model.FormulaInput
import com.formuladock.core.model.formula.model.FormulaOutput

internal object FormulaMapper {
    private const val KIND_INPUT = "INPUT"
    private const val KIND_CONSTANT = "CONSTANT"
    private const val KIND_OUTPUT = "OUTPUT"

    fun toDefinition(
        formula: Formula,
        symbols: List<Formula_symbol>,
    ): FormulaDefinition {
        return FormulaDefinition(
            id = formula.id,
            title = formula.title,
            description = formula.description,
            inputs = symbols.filter { it.kind == KIND_INPUT }.map { it.toInput() },
            constants = symbols.filter { it.kind == KIND_CONSTANT }.map { it.toConstant() },
            outputs = symbols.filter { it.kind == KIND_OUTPUT }.map { it.toOutput() },
            isBuiltin = formula.is_builtin.toBooleanFlag(),
            sortOrder = formula.sort_order.toInt(),
            createdAt = formula.created_at,
            updatedAt = formula.updated_at,
        )
    }

    fun FormulaInput.toSymbolInsert(formulaId: String): FormulaSymbolInsert {
        return FormulaSymbolInsert(
            id = id,
            formulaId = formulaId,
            kind = KIND_INPUT,
            key = key,
            label = label,
            defaultValue = defaultValue,
            constantValue = null,
            expression = null,
            unit = unit,
            precision = 2,
            required = required,
            sortOrder = sortOrder,
        )
    }

    fun FormulaConstant.toSymbolInsert(formulaId: String): FormulaSymbolInsert {
        return FormulaSymbolInsert(
            id = id,
            formulaId = formulaId,
            kind = KIND_CONSTANT,
            key = key,
            label = label,
            defaultValue = null,
            constantValue = value,
            expression = null,
            unit = unit,
            precision = 2,
            required = true,
            sortOrder = sortOrder,
        )
    }

    fun FormulaOutput.toSymbolInsert(formulaId: String): FormulaSymbolInsert {
        return FormulaSymbolInsert(
            id = id,
            formulaId = formulaId,
            kind = KIND_OUTPUT,
            key = key,
            label = label,
            defaultValue = null,
            constantValue = null,
            expression = expression,
            unit = unit,
            precision = precision,
            required = true,
            sortOrder = sortOrder,
        )
    }

    fun Boolean.toLongFlag(): Long = if (this) 1L else 0L

    private fun Long.toBooleanFlag(): Boolean = this != 0L

    private fun Formula_symbol.toInput(): FormulaInput {
        return FormulaInput(
            id = id,
            key = key,
            label = label,
            defaultValue = default_value,
            unit = unit,
            required = required.toBooleanFlag(),
            sortOrder = sort_order.toInt(),
        )
    }

    private fun Formula_symbol.toConstant(): FormulaConstant {
        return FormulaConstant(
            id = id,
            key = key,
            label = label,
            value = requireNotNull(constant_value) { "CONSTANT symbol $id must have constant_value" },
            unit = unit,
            sortOrder = sort_order.toInt(),
        )
    }

    private fun Formula_symbol.toOutput(): FormulaOutput {
        return FormulaOutput(
            id = id,
            key = key,
            label = label,
            expression = requireNotNull(expression) { "OUTPUT symbol $id must have expression" },
            unit = unit,
            precision = precision.toInt(),
            sortOrder = sort_order.toInt(),
        )
    }
}

internal data class FormulaSymbolInsert(
    val id: String,
    val formulaId: String,
    val kind: String,
    val key: String,
    val label: String,
    val defaultValue: String?,
    val constantValue: String?,
    val expression: String?,
    val unit: String?,
    val precision: Int,
    val required: Boolean,
    val sortOrder: Int,
)
