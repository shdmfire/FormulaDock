package com.formuladock.core.data.formula

import com.formuladock.core.data.formula.FormulaMapper.toLongFlag
import com.formuladock.core.data.formula.FormulaMapper.toSymbolInsert
import com.formuladock.core.database.FormulaDockDatabase
import com.formuladock.core.model.formula.model.FormulaDefinition

interface FormulaRepository {
    suspend fun countFormulas(): Long
    suspend fun getAllFormulas(): List<FormulaDefinition>
    suspend fun getFormula(id: String): FormulaDefinition?
    suspend fun saveFormula(formula: FormulaDefinition)
    suspend fun importFormulas(formulas: List<FormulaDefinition>) {
        formulas.forEach { saveFormula(it) }
    }
    suspend fun deleteFormula(id: String)
}

class SqlDelightFormulaRepository(
    private val database: FormulaDockDatabase,
) : FormulaRepository {
    private val queries = database.formulaQueries

    override suspend fun countFormulas(): Long {
        return queries.countFormulas().executeAsOne()
    }

    override suspend fun getAllFormulas(): List<FormulaDefinition> {
        return queries.selectAllFormulas().executeAsList().map { formula ->
            FormulaMapper.toDefinition(
                formula = formula,
                symbols = queries.selectSymbolsByFormulaId(formula.id).executeAsList(),
            )
        }
    }

    override suspend fun getFormula(id: String): FormulaDefinition? {
        val formula = queries.selectFormulaById(id).executeAsOneOrNull() ?: return null
        val symbols = queries.selectSymbolsByFormulaId(id).executeAsList()
        return FormulaMapper.toDefinition(formula, symbols)
    }

    override suspend fun saveFormula(formula: FormulaDefinition) {
        database.transaction {
            queries.insertFormula(
                id = formula.id,
                title = formula.title,
                description = formula.description,
                is_builtin = formula.isBuiltin.toLongFlag(),
                sort_order = formula.sortOrder.toLong(),
                created_at = formula.createdAt,
                updated_at = formula.updatedAt,
            )
            queries.deleteSymbolsByFormulaId(formula.id)
            formula.inputs.forEach { queries.insertSymbol(it.toSymbolInsert(formula.id)) }
            formula.constants.forEach { queries.insertSymbol(it.toSymbolInsert(formula.id)) }
            formula.outputs.forEach { queries.insertSymbol(it.toSymbolInsert(formula.id)) }
        }
    }

    override suspend fun importFormulas(formulas: List<FormulaDefinition>) {
        database.transaction {
            formulas.forEach { formula ->
                queries.insertFormula(
                    id = formula.id,
                    title = formula.title,
                    description = formula.description,
                    is_builtin = formula.isBuiltin.toLongFlag(),
                    sort_order = formula.sortOrder.toLong(),
                    created_at = formula.createdAt,
                    updated_at = formula.updatedAt,
                )
                queries.deleteSymbolsByFormulaId(formula.id)
                formula.inputs.forEach { queries.insertSymbol(it.toSymbolInsert(formula.id)) }
                formula.constants.forEach { queries.insertSymbol(it.toSymbolInsert(formula.id)) }
                formula.outputs.forEach { queries.insertSymbol(it.toSymbolInsert(formula.id)) }
            }
        }
    }

    override suspend fun deleteFormula(id: String) {
        queries.deleteFormulaById(id)
    }

    private fun com.formuladock.core.database.FormulaQueries.insertSymbol(symbol: FormulaSymbolInsert) {
        insertFormulaSymbol(
            id = symbol.id,
            formula_id = symbol.formulaId,
            kind = symbol.kind,
            key = symbol.key,
            label = symbol.label,
            default_value = symbol.defaultValue,
            constant_value = symbol.constantValue,
            expression = symbol.expression,
            unit = symbol.unit,
            precision = symbol.precision.toLong(),
            required = symbol.required.toLongFlag(),
            sort_order = symbol.sortOrder.toLong(),
        )
    }
}
