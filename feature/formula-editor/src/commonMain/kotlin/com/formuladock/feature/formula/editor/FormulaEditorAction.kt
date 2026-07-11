package com.formuladock.feature.formula.editor

import com.formuladock.core.domain.formula.editor.FormulaConstantDraft
import com.formuladock.core.domain.formula.editor.FormulaEditorSection
import com.formuladock.core.domain.formula.editor.FormulaInputDraft
import com.formuladock.core.domain.formula.editor.FormulaOutputDraft

sealed interface FormulaEditorAction {
    data object Back : FormulaEditorAction
    data object Save : FormulaEditorAction

    data class UpdateTitle(val title: String) : FormulaEditorAction
    data class UpdateDescription(val description: String) : FormulaEditorAction

    data object AddInput : FormulaEditorAction
    data class RemoveInput(val rowId: String) : FormulaEditorAction
    data class UpdateInputKey(val rowId: String, val key: String) : FormulaEditorAction
    data class UpdateInputLabel(val rowId: String, val label: String) : FormulaEditorAction
    data class UpdateInputDefaultValue(val rowId: String, val defaultValue: String) : FormulaEditorAction
    data class UpdateInputUnit(val rowId: String, val unit: String) : FormulaEditorAction
    data class UpdateInputRequired(val rowId: String, val required: Boolean) : FormulaEditorAction
    data class UpdateInput(val rowId: String, val input: FormulaInputDraft) : FormulaEditorAction

    data object AddConstant : FormulaEditorAction
    data class RemoveConstant(val rowId: String) : FormulaEditorAction
    data class UpdateConstantKey(val rowId: String, val key: String) : FormulaEditorAction
    data class UpdateConstantLabel(val rowId: String, val label: String) : FormulaEditorAction
    data class UpdateConstantValue(val rowId: String, val value: String) : FormulaEditorAction
    data class UpdateConstantUnit(val rowId: String, val unit: String) : FormulaEditorAction
    data class UpdateConstant(val rowId: String, val constant: FormulaConstantDraft) : FormulaEditorAction

    data object AddOutput : FormulaEditorAction
    data class RemoveOutput(val rowId: String) : FormulaEditorAction
    data class UpdateOutputKey(val rowId: String, val key: String) : FormulaEditorAction
    data class UpdateOutputLabel(val rowId: String, val label: String) : FormulaEditorAction
    data class UpdateOutputExpression(val rowId: String, val expression: String) : FormulaEditorAction
    data class UpdateOutputUnit(val rowId: String, val unit: String) : FormulaEditorAction
    data class UpdateOutputPrecision(val rowId: String, val precision: String) : FormulaEditorAction
    data class UpdateOutput(val rowId: String, val output: FormulaOutputDraft) : FormulaEditorAction

    data class ToggleSection(val section: FormulaEditorSection) : FormulaEditorAction
}
