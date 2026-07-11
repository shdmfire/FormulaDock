@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.formuladock.feature.formula.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.formuladock.core.designsystem.theme.FdDimensions
import com.formuladock.feature.formula.editor.FormulaEditorAction
import com.formuladock.core.domain.formula.editor.FormulaEditorMode
import com.formuladock.core.domain.formula.editor.toFormulaDefinition
import com.formuladock.core.domain.formula.editor.FormulaEditorSection
import com.formuladock.feature.formula.editor.FormulaEditorState
import com.formuladock.feature.formula.editor.FormulaEditorViewModel
import com.formuladock.feature.formula.io.FormulaClipboardService
import com.formuladock.feature.formula.io.LocalFormulaShareService
import com.formuladock.feature.formula.io.SingleFormulaExportSheet
import kotlinx.coroutines.launch
import kotlin.time.Clock

@Composable
fun FormulaEditorScreen(
    viewModel: FormulaEditorViewModel,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    FormulaEditorScreen(
        state = state,
        onAction = { action ->
            if (action == FormulaEditorAction.Back) onBackClick()
            viewModel.onAction(action)
        },
        modifier = modifier,
    )
}

@Composable
fun FormulaEditorScreen(
    state: FormulaEditorState,
    onAction: (FormulaEditorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expressionHelper = rememberFormulaExpressionHelperController()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showExportSheet by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    LaunchedEffect(imeBottomPx) {
        expressionHelper.reportImeBottomPx(imeBottomPx)
    }

    val helperReservedBottomSpace = with(density) {
        expressionHelper.reservedBottomSpacePx.toDp()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { FormulaEditorTopBar(state, onAction, onExportClick = { showExportSheet = true }) },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { innerPadding ->
            if (state.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Loading formula...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(bottom = helperReservedBottomSpace)
                        .padding(horizontal = FdDimensions.SpaceS)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(FdDimensions.SpaceS),
                ) {
                    FormulaMetaDataSection(state, onAction)
                    FormulaInputsSection(state, onAction)
                    FormulaConstantsSection(state, onAction)
                    FormulaOutputsSection(
                        state = state,
                        onAction = onAction,
                        expressionHelper = expressionHelper,
                    )
                    FormulaPreviewSection(state, onAction)

                    state.saveError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = FdDimensions.SpaceXs)
                        )
                    }

                    Spacer(Modifier.height(FdDimensions.SpaceL))
                }
            }
        }

        FormulaExpressionHelperOverlay(
            controller = expressionHelper,
            modifier = Modifier.fillMaxSize(),
        )

        if (showExportSheet) {
            // TODO: LocalClipboardManager
            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
            val clipboardService = remember(clipboardManager) {
                FormulaClipboardService { text ->
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(text))
                    true
                }
            }
            SingleFormulaExportSheet(
                formula = state.draft.toFormulaDefinition(Clock.System.now().toEpochMilliseconds()),
                clipboard = clipboardService,
                shareService = LocalFormulaShareService.current,
                nowProvider = { Clock.System.now().toEpochMilliseconds() },
                onDismiss = { showExportSheet = false },
                onMessage = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormulaEditorTopBar(
    state: FormulaEditorState,
    onAction: (FormulaEditorAction) -> Unit,
    onExportClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = when (state.mode) {
                    FormulaEditorMode.Create -> "New Formula"
                    is FormulaEditorMode.Edit -> "Edit Formula"
                    is FormulaEditorMode.Duplicate -> "Duplicate Formula"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        },
        navigationIcon = {
            IconButton(onClick = { onAction(FormulaEditorAction.Back) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.mode is FormulaEditorMode.Edit && !state.validation.hasErrors) {
                    IconButton(onClick = onExportClick) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "导出公式",
                        )
                    }
                }
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 12.dp).size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Button(
                    onClick = { onAction(FormulaEditorAction.Save) },
                    enabled = state.canSave,
                    modifier = Modifier.padding(end = 12.dp),
                ) {
                    Text("Save")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}
