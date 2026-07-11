package com.formuladock.feature.formula.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.domain.formula.editor.FormulaEditorMode

@Composable
fun FormulaEditorRoute(
    repository: FormulaRepository,
    mode: FormulaEditorMode,
    refreshKey: Any? = null,
    modifier: Modifier = Modifier,
    nowProvider: () -> Long = { 0L },
    onBackClick: () -> Unit = {},
) {
    val viewModel = remember(mode, refreshKey) {
        FormulaEditorViewModel(
            repository = repository,
            mode = mode,
            nowProvider = nowProvider,
        )
    }
    FormulaEditorScreen(
        viewModel = viewModel,
        modifier = modifier,
        onBackClick = onBackClick,
    )
}
