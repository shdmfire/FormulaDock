package com.formuladock.core.domain.formula.editor

import com.formuladock.core.formula.engine.FormulaExpressionNames

private val FormulaKeyRegex = Regex("[a-zA-Z_][a-zA-Z0-9_]*")
private val FormulaIdentifierRegex = Regex("[a-zA-Z_][a-zA-Z0-9_]*")

object FormulaEditorValidator {
    fun validate(draft: FormulaEditorDraft): FormulaValidationReport {
        val issues = buildList {
            validateTitle(draft)
            validateInputs(draft)
            validateConstants(draft)
            validateOutputs(draft)
            validateDuplicateKeys(draft)
            validateOutputExpressions(draft)
        }
        return FormulaValidationReport(issues)
    }

    private fun MutableList<FormulaValidationIssue>.validateTitle(draft: FormulaEditorDraft) {
        if (draft.title.isBlank()) {
            addError(
                message = "Title must not be empty.",
                section = FormulaEditorSection.MetaData,
                field = "title",
            )
        }
    }

    private fun MutableList<FormulaValidationIssue>.validateInputs(draft: FormulaEditorDraft) {
        draft.inputs.forEach { input ->
            validateKey(input.key, FormulaEditorSection.Inputs, input.rowId, "key")
            if (input.defaultValue.isNotBlank()) {
                validateNumber(
                    value = input.defaultValue,
                    message = "Input default value must be a finite number.",
                    section = FormulaEditorSection.Inputs,
                    rowId = input.rowId,
                    field = "defaultValue",
                )
            }
        }
    }

    private fun MutableList<FormulaValidationIssue>.validateConstants(draft: FormulaEditorDraft) {
        draft.constants.forEach { constant ->
            validateKey(constant.key, FormulaEditorSection.Constants, constant.rowId, "key")
            validateNumber(
                value = constant.value,
                message = "Constant value must be a finite number.",
                section = FormulaEditorSection.Constants,
                rowId = constant.rowId,
                field = "value",
            )
        }
    }

    private fun MutableList<FormulaValidationIssue>.validateOutputs(draft: FormulaEditorDraft) {
        draft.outputs.forEach { output ->
            validateKey(output.key, FormulaEditorSection.Outputs, output.rowId, "key")
            if (output.expression.isBlank()) {
                addError(
                    message = "Output expression must not be empty.",
                    section = FormulaEditorSection.Outputs,
                    rowId = output.rowId,
                    field = "expression",
                )
            }
            val precision = output.precision.trim().toIntOrNull()
            if (precision == null || precision < 0) {
                addError(
                    message = "Output precision must be a non-negative integer.",
                    section = FormulaEditorSection.Outputs,
                    rowId = output.rowId,
                    field = "precision",
                )
            }
            if (output.expression.contains("NaN") || output.expression.contains("Infinity")) {
                addError(
                    message = "Output expression result must not be NaN or Infinity.",
                    section = FormulaEditorSection.Outputs,
                    rowId = output.rowId,
                    field = "expression",
                )
            }
        }
    }

    private fun MutableList<FormulaValidationIssue>.validateDuplicateKeys(draft: FormulaEditorDraft) {
        val keyOwners = buildList {
            draft.inputs.forEach { add(KeyOwner(it.key.trim(), FormulaEditorSection.Inputs, it.rowId)) }
            draft.constants.forEach { add(KeyOwner(it.key.trim(), FormulaEditorSection.Constants, it.rowId)) }
            draft.outputs.forEach { add(KeyOwner(it.key.trim(), FormulaEditorSection.Outputs, it.rowId)) }
        }.filter { it.key.isNotEmpty() }

        keyOwners.groupBy { it.key }.filterValues { it.size > 1 }.values.flatten().forEach { owner ->
            addError(
                message = "Key '${owner.key}' is duplicated.",
                section = owner.section,
                rowId = owner.rowId,
                field = "key",
            )
        }
    }

    private fun MutableList<FormulaValidationIssue>.validateOutputExpressions(draft: FormulaEditorDraft) {
        val availableKeys = mutableSetOf<String>()
        availableKeys += draft.inputs.mapNotNull { it.key.trim().takeIf(FormulaKeyRegex::matches) }
        availableKeys += draft.constants.mapNotNull { it.key.trim().takeIf(FormulaKeyRegex::matches) }

        draft.outputs.forEach { output ->
            val outputKey = output.key.trim()
            val referencedKeys = FormulaIdentifierRegex.findAll(output.expression).map { it.value }.toSet()
            referencedKeys
                .filterNot { it in availableKeys }
                .filterNot { it in FormulaExpressionNames.reservedNames }
                .forEach { missingKey ->
                addError(
                    message = "Expression references unavailable variable '$missingKey'.",
                    section = FormulaEditorSection.Outputs,
                    rowId = output.rowId,
                    field = "expression",
                )
            }
            if (FormulaKeyRegex.matches(outputKey)) {
                availableKeys += outputKey
            }
        }
    }

    private fun MutableList<FormulaValidationIssue>.validateKey(
        key: String,
        section: FormulaEditorSection,
        rowId: String,
        field: String,
    ) {
        val trimmedKey = key.trim()
        when {
            trimmedKey.isEmpty() -> addError("Key must not be empty.", section, rowId, field)
            !FormulaKeyRegex.matches(trimmedKey) -> addError(
                message = "Key must match [a-zA-Z_][a-zA-Z0-9_]*.",
                section = section,
                rowId = rowId,
                field = field,
            )
            trimmedKey in FormulaExpressionNames.reservedNames -> addError(
                message = "Key '$trimmedKey' is reserved by the expression engine.",
                section = section,
                rowId = rowId,
                field = field,
            )
        }
    }

    private fun MutableList<FormulaValidationIssue>.validateNumber(
        value: String,
        message: String,
        section: FormulaEditorSection,
        rowId: String,
        field: String,
    ) {
        val number = value.trim().toDoubleOrNull()
        if (number == null || !number.isFinite()) {
            addError(message, section, rowId, field)
        }
    }

    private fun MutableList<FormulaValidationIssue>.addError(
        message: String,
        section: FormulaEditorSection? = null,
        rowId: String? = null,
        field: String? = null,
    ) {
        add(
            FormulaValidationIssue(
                severity = FormulaValidationSeverity.Error,
                message = message,
                location = FormulaValidationLocation(section, rowId, field),
            )
        )
    }

    private data class KeyOwner(
        val key: String,
        val section: FormulaEditorSection,
        val rowId: String,
    )
}

data class FormulaValidationReport(
    val issues: List<FormulaValidationIssue> = emptyList(),
) {
    val hasErrors: Boolean get() = issues.any { it.severity == FormulaValidationSeverity.Error }

    fun issuesForRow(rowId: String): List<FormulaValidationIssue> =
        issues.filter { it.location.rowId == rowId }
}

data class FormulaValidationIssue(
    val severity: FormulaValidationSeverity,
    val message: String,
    val location: FormulaValidationLocation = FormulaValidationLocation(),
)

enum class FormulaValidationSeverity {
    Error,
    Warning,
}

data class FormulaValidationLocation(
    val section: FormulaEditorSection? = null,
    val rowId: String? = null,
    val field: String? = null,
)
