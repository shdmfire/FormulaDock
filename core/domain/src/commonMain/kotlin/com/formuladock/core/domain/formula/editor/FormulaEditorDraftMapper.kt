package com.formuladock.core.domain.formula.editor

import com.formuladock.core.model.formula.model.FormulaConstant
import com.formuladock.core.model.formula.model.FormulaDefinition
import com.formuladock.core.model.formula.model.FormulaInput
import com.formuladock.core.model.formula.model.FormulaOutput

fun createEmptyFormulaDraft(rowIdPrefix: String = "new"): FormulaEditorDraft {
    return FormulaEditorDraft(
        title = "",
        description = "",
        inputs = listOf(
            FormulaInputDraft(
                rowId = "${rowIdPrefix}_input_0",
                key = "a",
                label = "a",
                required = true,
            )
        ),
        constants = emptyList(),
        outputs = listOf(
            FormulaOutputDraft(
                rowId = "${rowIdPrefix}_output_0",
                key = "result",
                label = "Result",
                expression = "a",
                precision = "2",
            )
        ),
    )
}

fun FormulaDefinition.toEditorDraft(clearIdentity: Boolean = false): FormulaEditorDraft {
    return FormulaEditorDraft(
        id = if (clearIdentity) null else id,
        title = title,
        description = description.orEmpty(),
        inputs = inputs.sortedBy { it.sortOrder }.map { input ->
            FormulaInputDraft(
                rowId = if (clearIdentity) "duplicate_input_${input.sortOrder}" else input.id,
                id = if (clearIdentity) null else input.id,
                key = input.key,
                label = input.label,
                defaultValue = input.defaultValue.orEmpty(),
                unit = input.unit.orEmpty(),
                required = input.required,
            )
        },
        constants = constants.sortedBy { it.sortOrder }.map { constant ->
            FormulaConstantDraft(
                rowId = if (clearIdentity) "duplicate_constant_${constant.sortOrder}" else constant.id,
                id = if (clearIdentity) null else constant.id,
                key = constant.key,
                label = constant.label,
                value = constant.value,
                unit = constant.unit.orEmpty(),
            )
        },
        outputs = outputs.sortedBy { it.sortOrder }.map { output ->
            FormulaOutputDraft(
                rowId = if (clearIdentity) "duplicate_output_${output.sortOrder}" else output.id,
                id = if (clearIdentity) null else output.id,
                key = output.key,
                label = output.label,
                expression = output.expression,
                unit = output.unit.orEmpty(),
                precision = output.precision.toString(),
            )
        },
        isBuiltin = if (clearIdentity) false else isBuiltin,
        sortOrder = if (clearIdentity) 0 else sortOrder,
        createdAt = if (clearIdentity) null else createdAt,
    )
}

fun FormulaEditorDraft.toFormulaDefinition(now: Long): FormulaDefinition {
    val validationReport = FormulaEditorValidator.validate(this)
    require(!validationReport.hasErrors) { "FormulaEditorDraft cannot be converted while validation has errors." }

    val formulaId = id ?: "formula_$now"

    return FormulaDefinition(
        id = formulaId,
        title = title.trim(),
        description = description.trim().ifEmpty { null },
        inputs = inputs.mapIndexed { index, input -> input.toFormulaInput(formulaId, index) },
        constants = constants.mapIndexed { index, constant -> constant.toFormulaConstant(formulaId, index) },
        outputs = outputs.mapIndexed { index, output -> output.toFormulaOutput(formulaId, index) },
        isBuiltin = isBuiltin,
        sortOrder = sortOrder,
        createdAt = createdAt ?: now,
        updatedAt = now,
    )
}

private fun FormulaInputDraft.toFormulaInput(formulaId: String, sortOrder: Int): FormulaInput {
    return FormulaInput(
        id = id ?: symbolId(formulaId, rowId),
        key = key.trim(),
        label = label.trim(),
        defaultValue = defaultValue.trim().ifEmpty { null },
        unit = unit.trim().ifEmpty { null },
        required = required,
        sortOrder = sortOrder,
    )
}

private fun FormulaConstantDraft.toFormulaConstant(formulaId: String, sortOrder: Int): FormulaConstant {
    return FormulaConstant(
        id = id ?: symbolId(formulaId, rowId),
        key = key.trim(),
        label = label.trim(),
        value = value.trim(),
        unit = unit.trim().ifEmpty { null },
        sortOrder = sortOrder,
    )
}

private fun FormulaOutputDraft.toFormulaOutput(formulaId: String, sortOrder: Int): FormulaOutput {
    return FormulaOutput(
        id = id ?: symbolId(formulaId, rowId),
        key = key.trim(),
        label = label.trim(),
        expression = expression.trim(),
        unit = unit.trim().ifEmpty { null },
        precision = precision.trim().toInt(),
        sortOrder = sortOrder,
    )
}

private fun symbolId(formulaId: String, rowId: String): String = "${formulaId}_$rowId"
