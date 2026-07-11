package com.formuladock.feature.formula.io

enum class FormulaIoTab { Import, Export }

data class FormulaIoUiState(
    val selectedTab: FormulaIoTab = FormulaIoTab.Import,
    val message: String? = null,
    val importState: ImportPanelState = ImportPanelState.Idle,
    val exportState: ExportPanelState = ExportPanelState.Loading,
)

sealed interface ImportPanelState {
    data object Idle : ImportPanelState
    data object Parsing : ImportPanelState
    data class Preview(
        val items: List<FormulaImportPreviewItemUiModel>,
        val warnings: List<String> = emptyList(),
    ) : ImportPanelState
    data class Success(val importedCount: Int) : ImportPanelState
    data class Error(val errors: List<String>) : ImportPanelState
}

data class FormulaImportPreviewItemUiModel(
    val index: Int,
    val title: String,
    val description: String?,
    val inputCount: Int,
    val outputCount: Int,
    val isSelected: Boolean = true,
    val warning: String? = null,
)

sealed interface ExportPanelState {
    data object Loading : ExportPanelState
    data object Empty : ExportPanelState
    data class Ready(
        val query: String = "",
        val items: List<FormulaExportItemUiModel>,
        val isExporting: Boolean = false,
    ) : ExportPanelState
    data class Error(val message: String) : ExportPanelState
}

data class FormulaExportItemUiModel(
    val id: String,
    val title: String,
    val description: String?,
    val isSelected: Boolean = true,
)
