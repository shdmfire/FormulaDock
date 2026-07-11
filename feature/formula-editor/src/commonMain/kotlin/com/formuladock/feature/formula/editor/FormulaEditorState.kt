package com.formuladock.feature.formula.editor

import com.formuladock.core.domain.formula.editor.FormulaEditorDraft
import com.formuladock.core.domain.formula.editor.FormulaEditorMode
import com.formuladock.core.domain.formula.editor.FormulaEditorSection
import com.formuladock.core.domain.formula.editor.FormulaPreviewState
import com.formuladock.core.domain.formula.editor.FormulaValidationReport

data class ExpandedSections(
    val metaData: Boolean = true,
    val inputs: Boolean = true,
    val constants: Boolean = true,
    val outputs: Boolean = true,
    val preview: Boolean = true,
) {
    fun isExpanded(section: FormulaEditorSection): Boolean = when (section) {
        FormulaEditorSection.MetaData -> metaData
        FormulaEditorSection.Inputs -> inputs
        FormulaEditorSection.Constants -> constants
        FormulaEditorSection.Outputs -> outputs
        FormulaEditorSection.Preview -> preview
    }

    fun toggled(section: FormulaEditorSection): ExpandedSections = when (section) {
        FormulaEditorSection.MetaData -> copy(metaData = !metaData)
        FormulaEditorSection.Inputs -> copy(inputs = !inputs)
        FormulaEditorSection.Constants -> copy(constants = !constants)
        FormulaEditorSection.Outputs -> copy(outputs = !outputs)
        FormulaEditorSection.Preview -> copy(preview = !preview)
    }
}

data class FormulaEditorState(
    val mode: FormulaEditorMode = FormulaEditorMode.Create,
    val draft: FormulaEditorDraft = FormulaEditorDraft(),
    val expandedSections: ExpandedSections = ExpandedSections(),
    val validation: FormulaValidationReport = FormulaValidationReport(),
    val preview: FormulaPreviewState = FormulaPreviewState.Empty,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDirty: Boolean = false,
    val saveError: String? = null,
) {
    val canSave: Boolean get() = !isSaving && !validation.hasErrors
}
