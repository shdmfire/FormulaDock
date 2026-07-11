package com.formuladock.core.domain.formula.editor

/**
 * Editable formula draft used by the editor UI.
 *
 * The draft is intentionally separate from the runtime FormulaDefinition model so
 * the UI can represent temporary invalid states such as an empty key, incomplete
 * expression, or blank precision.
 */
data class FormulaEditorDraft(
    val id: String? = null,
    val title: String = "",
    val description: String = "",
    val inputs: List<FormulaInputDraft> = emptyList(),
    val constants: List<FormulaConstantDraft> = emptyList(),
    val outputs: List<FormulaOutputDraft> = emptyList(),
    val isBuiltin: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long? = null,
)

data class FormulaInputDraft(
    /** Stable UI row identity. Must not be derived from or replaced by [key]. */
    val rowId: String,
    val id: String? = null,
    val key: String = "",
    val label: String = "",
    val defaultValue: String = "",
    val unit: String = "",
    val required: Boolean = true,
)

data class FormulaConstantDraft(
    /** Stable UI row identity. Must not be derived from or replaced by [key]. */
    val rowId: String,
    val id: String? = null,
    val key: String = "",
    val label: String = "",
    val value: String = "",
    val unit: String = "",
)

data class FormulaOutputDraft(
    /** Stable UI row identity. Must not be derived from or replaced by [key]. */
    val rowId: String,
    val id: String? = null,
    val key: String = "",
    val label: String = "",
    val expression: String = "",
    val unit: String = "",
    /** String because users may temporarily enter a blank or otherwise invalid value. */
    val precision: String = "2",
)
