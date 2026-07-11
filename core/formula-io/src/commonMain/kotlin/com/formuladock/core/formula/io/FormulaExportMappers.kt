package com.formuladock.core.formula.io

import com.formuladock.core.model.formula.model.FormulaConstant
import com.formuladock.core.model.formula.model.FormulaDefinition
import com.formuladock.core.model.formula.model.FormulaInput
import com.formuladock.core.model.formula.model.FormulaOutput

internal fun FormulaDefinition.toDto() = FormulaDefinitionDto(
    id = id,
    title = title,
    description = description,
    inputs = inputs.map { it.toDto() },
    constants = constants.map { it.toDto() },
    outputs = outputs.map { it.toDto() },
    isBuiltin = isBuiltin,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun FormulaInput.toDto() = FormulaInputDto(id, key, label, defaultValue, unit, required, sortOrder)

internal fun FormulaConstant.toDto() = FormulaConstantDto(id, key, label, value, unit, sortOrder)

internal fun FormulaOutput.toDto() = FormulaOutputDto(id, key, label, expression, unit, precision, sortOrder)

internal fun FormulaDefinitionDto.toModel(options: FormulaImportOptions) = FormulaDefinition(
    id = id,
    title = title.trim(),
    description = description,
    inputs = inputs.map { it.toModel() },
    constants = constants.map { it.toModel() },
    outputs = outputs.map { it.toModel() },
    isBuiltin = false,
    sortOrder = sortOrder,
    createdAt = options.now,
    updatedAt = options.now,
)

internal fun FormulaInputDto.toModel() = FormulaInput(id, key.trim(), label, defaultValue, unit, required, sortOrder)

internal fun FormulaConstantDto.toModel() = FormulaConstant(id, key.trim(), label, value, unit, sortOrder)

internal fun FormulaOutputDto.toModel() = FormulaOutput(id, key.trim(), label, expression, unit, precision, sortOrder)
