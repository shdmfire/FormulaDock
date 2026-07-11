package com.formuladock.feature.formula.io

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.navigation.AppRoute
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object FormulaIoNavKey : AppRoute

fun EntryProviderScope<NavKey>.formulaIoEntry(
    repository: FormulaRepository,
    filePicker: FormulaFilePicker = NoOpFormulaFilePicker,
    clipboard: FormulaClipboardService = NoOpFormulaClipboardService,
    clipboardReader: FormulaClipboardReader = NoOpFormulaClipboardReader,
    nowProvider: () -> Long = { 0L },
    onBack: () -> Unit,
) {
    entry<FormulaIoNavKey> {
        FormulaIoRoute(repository, filePicker = filePicker, clipboard = clipboard, clipboardReader = clipboardReader, nowProvider = nowProvider, onBack = onBack)
    }
}

@Composable
fun FormulaIoRoute(
    repository: FormulaRepository,
    modifier: Modifier = Modifier,
    filePicker: FormulaFilePicker = NoOpFormulaFilePicker,
    clipboard: FormulaClipboardService = NoOpFormulaClipboardService,
    clipboardReader: FormulaClipboardReader = NoOpFormulaClipboardReader,
    nowProvider: () -> Long = { 0L },
    onBack: () -> Unit,
) {
    val viewModel = remember(repository, filePicker, clipboard, clipboardReader, nowProvider) {
        FormulaIoViewModel(repository, filePicker, clipboard, clipboardReader, nowProvider)
    }
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    var pendingExport by remember { mutableStateOf<FormulaExportPayload?>(null) }
    val fileSaverLauncher = rememberFileSaverLauncher(
        dialogSettings = FileKitDialogSettings.createDefault(),
    ) { file ->
        val payload = pendingExport ?: return@rememberFileSaverLauncher
        pendingExport = null
        if (file == null) {
            viewModel.showExportMessage("已取消保存")
            return@rememberFileSaverLauncher
        }
        scope.launch {
            val message = runCatching {
                file.writeString(payload.content)
                "已导出 ${payload.count} 个公式为文件"
            }.getOrElse { throwable ->
                throwable.printStackTrace()
                "保存文件失败"
            }
            viewModel.showExportMessage(message)
        }
    }

    FormulaIoScreen(
        state = state,
        onTabClick = viewModel::selectTab,
        onImportFileClick = viewModel::importFromFile,
        onImportClipboardClick = viewModel::importFromClipboard,
        onToggleImportItem = viewModel::toggleImportItem,
        onConfirmImportClick = viewModel::confirmImport,
        onResetImportClick = viewModel::resetImport,
        onExportQueryChange = viewModel::updateExportQuery,
        onToggleExportItem = viewModel::toggleExportFormula,
        onSelectAllExport = viewModel::selectAllExportFormulas,
        onSaveSelectedClick = {
            val payload = viewModel.selectedExportPayload() ?: return@FormulaIoScreen
            pendingExport = payload
            val extension = payload.fileName.substringAfterLast(".", "json")
            fileSaverLauncher.launch(
                suggestedName = payload.fileName.substringBeforeLast("."),
                defaultExtension = extension,
                allowedExtensions = setOf(extension),
            )
        },
        onCopySelectedClick = viewModel::copySelectedFormulas,
        onBack = onBack,
        modifier = modifier,
    )
}
