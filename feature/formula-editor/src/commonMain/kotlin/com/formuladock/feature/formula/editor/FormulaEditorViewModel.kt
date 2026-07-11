package com.formuladock.feature.formula.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.domain.formula.editor.BuildFormulaPreviewUseCase
import com.formuladock.core.domain.formula.editor.FormulaConstantDraft
import com.formuladock.core.domain.formula.editor.FormulaEditorDraft
import com.formuladock.core.domain.formula.editor.FormulaEditorMode
import com.formuladock.core.domain.formula.editor.FormulaInputDraft
import com.formuladock.core.domain.formula.editor.FormulaOutputDraft
import com.formuladock.core.domain.formula.editor.FormulaPreviewState
import com.formuladock.core.domain.formula.editor.LoadFormulaEditorDraftUseCase
import com.formuladock.core.domain.formula.editor.SaveFormulaDraftUseCase
import com.formuladock.core.domain.formula.editor.ValidateFormulaDraftUseCase
import com.formuladock.core.domain.formula.editor.toEditorDraft
import com.formuladock.core.formula.engine.DefaultFormulaEngine
import com.formuladock.core.formula.engine.FormulaEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FormulaEditorViewModel(
    private val repository: FormulaRepository,
    private val mode: FormulaEditorMode = FormulaEditorMode.Create,
    private val formulaEngine: FormulaEngine = DefaultFormulaEngine(),
    private val nowProvider: () -> Long = { 0L },
) : ViewModel() {
    private var rowIdSeed: Long = 0L
    private val loadDraft = LoadFormulaEditorDraftUseCase(repository)
    private val saveDraft = SaveFormulaDraftUseCase(repository, nowProvider)
    private val validateDraft = ValidateFormulaDraftUseCase()
    private val buildPreviewUseCase = BuildFormulaPreviewUseCase(formulaEngine, nowProvider)

    private val _state = MutableStateFlow(
        FormulaEditorState(
            mode = mode,
            isLoading = mode !is FormulaEditorMode.Create,
        )
    )
    val state: StateFlow<FormulaEditorState> = _state.asStateFlow()

    init {
        viewModelScope.launch { initialize() }
    }

    fun onAction(action: FormulaEditorAction) {
        when (action) {
            FormulaEditorAction.Back -> Unit
            FormulaEditorAction.Save -> save()
            is FormulaEditorAction.UpdateTitle -> updateDraft { copy(title = action.title) }
            is FormulaEditorAction.UpdateDescription -> updateDraft { copy(description = action.description) }
            FormulaEditorAction.AddInput -> updateDraft { copy(inputs = inputs + FormulaInputDraft(rowId = nextRowId("input"))) }
            is FormulaEditorAction.RemoveInput -> updateDraft { copy(inputs = inputs.filterNot { it.rowId == action.rowId }) }
            is FormulaEditorAction.UpdateInputKey -> updateInput(action.rowId) { copy(key = action.key) }
            is FormulaEditorAction.UpdateInputLabel -> updateInput(action.rowId) { copy(label = action.label) }
            is FormulaEditorAction.UpdateInputDefaultValue -> updateInput(action.rowId) { copy(defaultValue = action.defaultValue) }
            is FormulaEditorAction.UpdateInputUnit -> updateInput(action.rowId) { copy(unit = action.unit) }
            is FormulaEditorAction.UpdateInputRequired -> updateInput(action.rowId) { copy(required = action.required) }
            is FormulaEditorAction.UpdateInput -> updateInput(action.rowId) { action.input.copy(rowId = rowId) }
            FormulaEditorAction.AddConstant -> updateDraft { copy(constants = constants + FormulaConstantDraft(rowId = nextRowId("constant"))) }
            is FormulaEditorAction.RemoveConstant -> updateDraft { copy(constants = constants.filterNot { it.rowId == action.rowId }) }
            is FormulaEditorAction.UpdateConstantKey -> updateConstant(action.rowId) { copy(key = action.key) }
            is FormulaEditorAction.UpdateConstantLabel -> updateConstant(action.rowId) { copy(label = action.label) }
            is FormulaEditorAction.UpdateConstantValue -> updateConstant(action.rowId) { copy(value = action.value) }
            is FormulaEditorAction.UpdateConstantUnit -> updateConstant(action.rowId) { copy(unit = action.unit) }
            is FormulaEditorAction.UpdateConstant -> updateConstant(action.rowId) { action.constant.copy(rowId = rowId) }
            FormulaEditorAction.AddOutput -> updateDraft { copy(outputs = outputs + FormulaOutputDraft(rowId = nextRowId("output"))) }
            is FormulaEditorAction.RemoveOutput -> updateDraft { copy(outputs = outputs.filterNot { it.rowId == action.rowId }) }
            is FormulaEditorAction.UpdateOutputKey -> updateOutput(action.rowId) { copy(key = action.key) }
            is FormulaEditorAction.UpdateOutputLabel -> updateOutput(action.rowId) { copy(label = action.label) }
            is FormulaEditorAction.UpdateOutputExpression -> updateOutput(action.rowId) { copy(expression = action.expression) }
            is FormulaEditorAction.UpdateOutputUnit -> updateOutput(action.rowId) { copy(unit = action.unit) }
            is FormulaEditorAction.UpdateOutputPrecision -> updateOutput(action.rowId) { copy(precision = action.precision) }
            is FormulaEditorAction.UpdateOutput -> updateOutput(action.rowId) { action.output.copy(rowId = rowId) }
            is FormulaEditorAction.ToggleSection -> _state.update { it.copy(expandedSections = it.expandedSections.toggled(action.section)) }
        }
    }

    private suspend fun initialize() {
        applyDraft(draft = loadDraft(mode), isDirty = false, isLoading = false)
    }

    private fun save() {
        viewModelScope.launch {
            val currentDraft = _state.value.draft
            val validation = validateDraft(currentDraft)
            if (validation.hasErrors) {
                _state.update { it.copy(validation = validation, preview = FormulaPreviewState.Empty) }
                return@launch
            }
            _state.update { it.copy(isSaving = true, saveError = null, validation = validation) }
            try {
                val savedFormula = saveDraft(currentDraft)
                applyDraft(draft = savedFormula.toEditorDraft(), isDirty = false, isLoading = false)
                _state.update { it.copy(isSaving = false) }
            } catch (throwable: Throwable) {
                _state.update { it.copy(isSaving = false, saveError = throwable.message ?: "Save failed.") }
            }
        }
    }

    private fun updateInput(rowId: String, transform: FormulaInputDraft.() -> FormulaInputDraft) {
        updateDraft { copy(inputs = inputs.map { if (it.rowId == rowId) it.transform() else it }) }
    }

    private fun updateConstant(rowId: String, transform: FormulaConstantDraft.() -> FormulaConstantDraft) {
        updateDraft { copy(constants = constants.map { if (it.rowId == rowId) it.transform() else it }) }
    }

    private fun updateOutput(rowId: String, transform: FormulaOutputDraft.() -> FormulaOutputDraft) {
        updateDraft { copy(outputs = outputs.map { if (it.rowId == rowId) it.transform() else it }) }
    }

    private fun updateDraft(transform: FormulaEditorDraft.() -> FormulaEditorDraft) {
        applyDraft(_state.value.draft.transform(), isDirty = true)
    }

    private fun applyDraft(
        draft: FormulaEditorDraft,
        isDirty: Boolean,
        isLoading: Boolean = _state.value.isLoading,
    ) {
        val validation = validateDraft(draft)
        val preview = if (validation.hasErrors) {
            FormulaPreviewState.Empty
        } else {
            buildPreview(draft)
        }
        _state.update {
            it.copy(
                draft = draft,
                validation = validation,
                preview = preview,
                isDirty = isDirty,
                isLoading = isLoading,
                saveError = null,
            )
        }
    }

    private fun buildPreview(draft: FormulaEditorDraft): FormulaPreviewState {
        return buildPreviewUseCase(draft)
    }

    private fun nextRowId(prefix: String): String = "${prefix}_${++rowIdSeed}"
}
