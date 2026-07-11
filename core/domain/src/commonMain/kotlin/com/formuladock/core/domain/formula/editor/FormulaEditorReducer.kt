package com.formuladock.core.domain.formula.editor

sealed interface FormulaEditorDraftCommand {
    data class UpdateTitle(val title: String) : FormulaEditorDraftCommand
    data class UpdateDescription(val description: String) : FormulaEditorDraftCommand
    data class AddInput(val rowId: String) : FormulaEditorDraftCommand
    data class RemoveInput(val rowId: String) : FormulaEditorDraftCommand
    data class UpdateInput(val rowId: String, val input: FormulaInputDraft) : FormulaEditorDraftCommand
    data class AddConstant(val rowId: String) : FormulaEditorDraftCommand
    data class RemoveConstant(val rowId: String) : FormulaEditorDraftCommand
    data class UpdateConstant(val rowId: String, val constant: FormulaConstantDraft) : FormulaEditorDraftCommand
    data class AddOutput(val rowId: String) : FormulaEditorDraftCommand
    data class RemoveOutput(val rowId: String) : FormulaEditorDraftCommand
    data class UpdateOutput(val rowId: String, val output: FormulaOutputDraft) : FormulaEditorDraftCommand
}

object FormulaEditorReducer {
    fun reduce(
        draft: FormulaEditorDraft,
        command: FormulaEditorDraftCommand,
    ): FormulaEditorDraft = when (command) {
        is FormulaEditorDraftCommand.UpdateTitle -> draft.copy(title = command.title)
        is FormulaEditorDraftCommand.UpdateDescription -> draft.copy(description = command.description)
        is FormulaEditorDraftCommand.AddInput -> draft.copy(inputs = draft.inputs + FormulaInputDraft(rowId = command.rowId))
        is FormulaEditorDraftCommand.RemoveInput -> draft.copy(inputs = draft.inputs.filterNot { it.rowId == command.rowId })
        is FormulaEditorDraftCommand.UpdateInput -> draft.copy(
            inputs = draft.inputs.map { if (it.rowId == command.rowId) command.input.copy(rowId = it.rowId) else it }
        )
        is FormulaEditorDraftCommand.AddConstant -> draft.copy(constants = draft.constants + FormulaConstantDraft(rowId = command.rowId))
        is FormulaEditorDraftCommand.RemoveConstant -> draft.copy(constants = draft.constants.filterNot { it.rowId == command.rowId })
        is FormulaEditorDraftCommand.UpdateConstant -> draft.copy(
            constants = draft.constants.map { if (it.rowId == command.rowId) command.constant.copy(rowId = it.rowId) else it }
        )
        is FormulaEditorDraftCommand.AddOutput -> draft.copy(outputs = draft.outputs + FormulaOutputDraft(rowId = command.rowId))
        is FormulaEditorDraftCommand.RemoveOutput -> draft.copy(outputs = draft.outputs.filterNot { it.rowId == command.rowId })
        is FormulaEditorDraftCommand.UpdateOutput -> draft.copy(
            outputs = draft.outputs.map { if (it.rowId == command.rowId) command.output.copy(rowId = it.rowId) else it }
        )
    }
}
