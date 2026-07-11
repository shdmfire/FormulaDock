package com.formuladock.feature.formula.io

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.formula.io.FormulaJsonCodec
import com.formuladock.core.model.formula.model.FormulaDefinition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FormulaIoViewModel(
    repository: FormulaRepository,
    private val filePicker: FormulaFilePicker = NoOpFormulaFilePicker,
    private val clipboard: FormulaClipboardService = NoOpFormulaClipboardService,
    private val clipboardReader: FormulaClipboardReader = NoOpFormulaClipboardReader,
    private val nowProvider: () -> Long = { 0L },
) : ViewModel() {
    private val exportUseCase = FormulaExportUseCase(repository)
    private val importUseCase = FormulaImportUseCase(repository, SimpleFormulaIdGenerator(nowProvider = nowProvider))
    private var pendingImport: List<FormulaDefinition> = emptyList()
    private var exportFormulas: List<FormulaDefinition> = emptyList()

    private val _state = MutableStateFlow(FormulaIoUiState())
    val state: StateFlow<FormulaIoUiState> = _state.asStateFlow()

    init {
        loadExportFormulas()
    }

    fun selectTab(tab: FormulaIoTab) = _state.update { it.copy(selectedTab = tab, message = null) }

    fun loadExportFormulas() {
        viewModelScope.launch {
            _state.update { it.copy(exportState = ExportPanelState.Loading) }
            runCatching { exportUseCase.loadUserFormulas() }
                .onSuccess { formulas ->
                    exportFormulas = formulas
                    _state.update { it.copy(exportState = formulas.toExportState()) }
                }
                .onFailure { throwable ->
                    _state.update { it.copy(exportState = ExportPanelState.Error(throwable.message ?: "加载公式失败")) }
                }
        }
    }

    fun updateExportQuery(query: String) = updateReadyExport { copy(query = query) }

    fun toggleExportFormula(id: String) = updateReadyExport {
        copy(items = items.map { if (it.id == id) it.copy(isSelected = !it.isSelected) else it })
    }

    fun selectAllExportFormulas(selected: Boolean) = updateReadyExport {
        copy(items = items.map { it.copy(isSelected = selected) })
    }

    fun selectedExportPayload(): FormulaExportPayload? {
        val ready = _state.value.exportState as? ExportPanelState.Ready ?: return null
        val selectedIds = ready.items.filter { it.isSelected }.map { it.id }.toSet()
        val formulas = exportFormulas.filter { it.id in selectedIds }
        if (formulas.isEmpty()) {
            showMessage("请选择要导出的公式")
            return null
        }
        val fileName = if (formulas.size == 1) formulaJsonFileName(formulas.single().title) else "formuladock-formulas.json"
        return FormulaExportPayload(
            fileName = fileName,
            content = FormulaJsonCodec.encode(formulas, nowProvider(), includeBuiltins = false),
            count = formulas.size,
        )
    }

    fun copySelectedFormulas() = exportSelected(
        action = { formulas ->
            if (clipboard.writeText(FormulaJsonCodec.encode(formulas, nowProvider(), includeBuiltins = false))) {
                "已复制 ${formulas.size} 个公式的 JSON"
            } else {
                "复制失败"
            }
        }
    )

    fun importFromFile() {
        viewModelScope.launch {
            val file = filePicker.pickJsonFile() ?: return@launch showMessage("已取消导入")
            previewImport(file)
        }
    }

    fun importFromClipboard() {
        viewModelScope.launch {
            val text = clipboardReader.readText()
            if (text.isNullOrBlank()) return@launch setImportError(listOf("剪贴板中没有文本"))
            previewImport(FormulaPickedFile("clipboard.json", text))
        }
    }

    fun toggleImportItem(index: Int) {
        val preview = _state.value.importState as? ImportPanelState.Preview ?: return
        _state.update {
            it.copy(importState = preview.copy(items = preview.items.map { item ->
                if (item.index == index) item.copy(isSelected = !item.isSelected) else item
            }))
        }
    }

    fun confirmImport() {
        val preview = _state.value.importState as? ImportPanelState.Preview ?: return
        val selectedIndexes = preview.items.filter { it.isSelected }.map { it.index }.toSet()
        val formulas = pendingImport.filterIndexed { index, _ -> index in selectedIndexes }
        if (formulas.isEmpty()) return showMessage("请选择要导入的公式")

        viewModelScope.launch {
            _state.update { it.copy(importState = ImportPanelState.Parsing, message = null) }
            runCatching { importUseCase.importFormulas(formulas) }
                .onSuccess {
                    pendingImport = emptyList()
                    _state.update { it.copy(importState = ImportPanelState.Success(formulas.size), message = null) }
                    loadExportFormulas()
                }
                .onFailure { throwable -> setImportError(listOf(throwable.message ?: "导入失败，请重试")) }
        }
    }

    fun resetImport() {
        pendingImport = emptyList()
        _state.update { it.copy(importState = ImportPanelState.Idle, message = null) }
    }

    private suspend fun previewImport(file: FormulaPickedFile) {
        _state.update { it.copy(importState = ImportPanelState.Parsing, message = null) }
        when (val result = importUseCase.preview(file, nowProvider())) {
            is FormulaImportPreviewResult.Failure -> setImportError(result.errors)
            is FormulaImportPreviewResult.Success -> {
                pendingImport = result.formulas
                _state.update {
                    it.copy(importState = ImportPanelState.Preview(
                        items = result.formulas.mapIndexed { index, formula ->
                            FormulaImportPreviewItemUiModel(
                                index = index,
                                title = formula.title,
                                description = formula.description,
                                inputCount = formula.inputs.size,
                                outputCount = formula.outputs.size,
                                warning = result.itemWarnings.getOrNull(index),
                            )
                        },
                        warnings = result.preview.warnings,
                    ))
                }
            }
        }
    }

    private fun exportSelected(
        action: suspend (List<FormulaDefinition>) -> String
    ) {
        val ready = _state.value.exportState as? ExportPanelState.Ready ?: return
        val selectedIds = ready.items.filter { it.isSelected }.map { it.id }.toSet()
        val formulas = exportFormulas.filter { it.id in selectedIds }
        if (formulas.isEmpty()) return showMessage("请选择要导出的公式")

        viewModelScope.launch {
            updateReadyExport { copy(isExporting = true) }
            val message = runCatching { action(formulas) }.getOrDefault("导出失败")
            updateReadyExport { copy(isExporting = false) }
            showMessage(message)
        }
    }

    private fun List<FormulaDefinition>.toExportState(): ExportPanelState =
        if (isEmpty()) ExportPanelState.Empty else ExportPanelState.Ready(items = map {
            FormulaExportItemUiModel(it.id, it.title, it.description)
        })

    private fun updateReadyExport(transform: ExportPanelState.Ready.() -> ExportPanelState.Ready) {
        val ready = _state.value.exportState as? ExportPanelState.Ready ?: return
        _state.update { it.copy(exportState = ready.transform()) }
    }

    private fun setImportError(errors: List<String>) {
        pendingImport = emptyList()
        _state.update { it.copy(importState = ImportPanelState.Error(errors)) }
    }

    fun showExportMessage(message: String) = showMessage(message)

    private fun showMessage(message: String) = _state.update { it.copy(message = message) }
}
