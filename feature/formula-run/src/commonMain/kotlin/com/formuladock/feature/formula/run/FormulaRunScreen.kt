package com.formuladock.feature.formula.run

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.formuladock.core.designsystem.component.LocalAppInForeground
import com.formuladock.core.domain.formula.EvaluateFormulaUseCase
import com.formuladock.core.model.formula.model.FormulaDefinition
import com.formuladock.core.formula.engine.FormulaEvaluationResult
import com.formuladock.core.data.history.CalculationHistoryRepository
import com.formuladock.core.model.history.model.CalculationHistory
import com.formuladock.core.model.history.model.CalculationRevision
import com.formuladock.feature.formula.io.FormulaClipboardService
import com.formuladock.feature.formula.io.LocalFormulaShareService
import com.formuladock.feature.formula.io.SingleFormulaExportSheet
import org.jetbrains.compose.resources.stringResource
import formuladock.feature.formula_run.generated.resources.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FormulaRunScreen(
    formula: FormulaDefinition,
    evaluateFormula: EvaluateFormulaUseCase,
    historyRepository: CalculationHistoryRepository,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    initialInputs: Map<String, String>? = null,
) {
    // 状态管理由外层或解耦内容层持有，以便复制分享功能获取当前最新数据
    var inputValues by remember(formula.id, initialInputs) {
        mutableStateOf(
            formula.inputs.associate { input ->
                input.key to (initialInputs?.get(input.key) ?: input.defaultValue ?: "")
            }
        )
    }
    val result = remember(formula, inputValues) { evaluateFormula(formula, inputValues) }

    // TODO: LocalClipboardManager
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showSessionHistorySheet by remember { mutableStateOf(false) }
    var sessionHistory by remember { mutableStateOf<CalculationHistory?>(null) }
    val sessionRecorder = remember(historyRepository, formula.id) {
        CalculationSessionRecorder(historyRepository)
    }
    var sessionReady by remember(historyRepository, formula.id) { mutableStateOf(false) }
    val appInForeground = LocalAppInForeground.current

    LaunchedEffect(sessionRecorder, formula.id, appInForeground) {
        if (appInForeground) {
            sessionRecorder.startOrResume(formula)
            sessionReady = true
        } else if (sessionReady) {
            (result as? FormulaEvaluationResult.Success)?.let {
                sessionRecorder.commit(formula, inputValues, it)
            }
            sessionRecorder.pause()
        }
    }

    LaunchedEffect(formula, inputValues, result, sessionReady, appInForeground) {
        if (appInForeground && sessionReady && result is FormulaEvaluationResult.Success) {
            delay(1000L.milliseconds)
            sessionRecorder.commit(formula, inputValues, result)
        }
    }

    val commitCurrentDraft: () -> Unit = {
        if (sessionReady) {
            scope.launch {
                (result as? FormulaEvaluationResult.Success)?.let {
                    sessionRecorder.commit(formula, inputValues, it)
                }
            }
        }
    }
    val pauseAndLeave: () -> Unit = {
        scope.launch {
            (result as? FormulaEvaluationResult.Success)?.let {
                sessionRecorder.commit(formula, inputValues, it)
            }
            sessionRecorder.pause()
            onBack()
        }
    }
    val finishAndLeave: () -> Unit = {
        scope.launch {
            (result as? FormulaEvaluationResult.Success)?.let {
                sessionRecorder.commit(formula, inputValues, it)
            }
            sessionRecorder.close()
            onBack()
        }
    }

    val shareTitleLabel = stringResource(Res.string.run_share_title)
    val inputParamsLabel = stringResource(Res.string.run_input_parameters)
    val calcResultLabel = stringResource(Res.string.run_calculation_result)
    val copiedMessage = stringResource(Res.string.run_share_copied)

    val handleShare: () -> Unit = {
        clipboardManager.setText(AnnotatedString(buildFormulaShareText(
            formula = formula,
            inputValues = inputValues,
            result = result,
            shareTitleLabel = shareTitleLabel,
            inputParamsLabel = inputParamsLabel,
            calcResultLabel = calcResultLabel
        )))
        scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            // 保留传统的系统级 TopAppBar
            TopAppBar(
                title = {
                    Text(
                        text = formula.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = pauseAndLeave) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    if (!formula.isBuiltin) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    IconButton(onClick = {
                        scope.launch {
                            if (sessionReady) {
                                (result as? FormulaEvaluationResult.Success)?.let {
                                    sessionRecorder.commit(formula, inputValues, it)
                                }
                            }
                            sessionHistory = sessionRecorder.loadSessionHistory()
                            showSessionHistorySheet = true
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = stringResource(Res.string.run_action_session_history),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = handleShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    val showMoreMenu = true
                    if (showMoreMenu) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.run_menu_finish)) },
                                    onClick = {
                                        showMenu = false
                                        finishAndLeave()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Finish calculation"
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.run_menu_duplicate)) },
                                    onClick = {
                                        showMenu = false
                                        onDuplicate()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Duplicate"
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.run_menu_export)) },
                                    onClick = {
                                        showMenu = false
                                        showExportSheet = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Export"
                                        )
                                    }
                                )
                                if (!formula.isBuiltin) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.run_menu_delete), color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            showMenu = false
                                            showDeleteConfirmation = true
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        // 使用解耦并高度极限压缩后的核心计算组件填装在主体区域中，且外层加上纵向滚动
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp), // 压缩屏幕内边距
            verticalArrangement = Arrangement.spacedBy(8.dp)   // 压缩组件纵向间距
        ) {
            // Description
            formula.description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // 调用可两处无缝复用的纯净内容组件
            CompactCalculatorContent(
                formula = formula,
                inputValues = inputValues,
                result = result,
                onInputValuesChange = { inputValues = it },
                onCommitRequested = commitCurrentDraft,
            )
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(Res.string.run_delete_confirm_title)) },
            text = { Text(stringResource(Res.string.run_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        scope.launch {
                            (result as? FormulaEvaluationResult.Success)?.let {
                                sessionRecorder.commit(formula, inputValues, it)
                            }
                            sessionRecorder.close()
                            onDelete()
                        }
                    }
                ) {
                    Text(stringResource(Res.string.run_delete_confirm_action), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(Res.string.run_delete_dismiss_action))
                }
            }
        )
    }

    if (showExportSheet) {
        val clipboardService = remember(clipboardManager) {
            FormulaClipboardService { text ->
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(text))
                true
            }
        }
        SingleFormulaExportSheet(
            formula = formula,
            clipboard = clipboardService,
            shareService = LocalFormulaShareService.current,
            nowProvider = { Clock.System.now().toEpochMilliseconds() },
            onDismiss = { showExportSheet = false },
            onMessage = { msg ->
                scope.launch { snackbarHostState.showSnackbar(msg) }
            }
        )
    }

    if (showSessionHistorySheet) {
        val appliedMessageTemplate = stringResource(Res.string.run_session_history_applied_message)
        SessionHistoryBottomSheet(
            sessionHistory = sessionHistory,
            currentInputs = inputValues,
            onSelectRevision = { revision ->
                inputValues = formula.inputs.associate { input ->
                    input.key to (revision.inputs.firstOrNull { it.key == input.key }?.rawValue
                        ?: revision.inputs.firstOrNull { it.key == input.key }?.numericValue?.toString()
                        ?: "")
                }
                showSessionHistorySheet = false
                scope.launch {
                    val message = runCatching {
                        appliedMessageTemplate.replace("%1\$d", revision.revisionNo.toString())
                    }.getOrDefault("Applied revision ${revision.revisionNo}")
                    snackbarHostState.showSnackbar(message)
                }
            },
            onDismissRequest = { showSessionHistorySheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionHistoryBottomSheet(
    sessionHistory: CalculationHistory?,
    currentInputs: Map<String, String>,
    onSelectRevision: (CalculationRevision) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val revisions = remember(sessionHistory) {
        sessionHistory?.revisions?.sortedByDescending { it.revisionNo }.orEmpty()
    }
    val maxRevNo = remember(revisions) { revisions.maxOfOrNull { it.revisionNo } ?: 0 }
    val minRevNo = remember(revisions) { revisions.minOfOrNull { it.revisionNo } ?: 0 }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.run_session_history_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (revisions.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.run_session_history_count, revisions.size),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            if (revisions.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.run_session_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(Res.string.run_session_history_empty_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(revisions, key = { it.id }) { revision ->
                        val isLatest = revision.revisionNo == maxRevNo
                        val isInitial = revision.revisionNo == minRevNo && revisions.size > 1
                        val isCurrentMatch = remember(revision, currentInputs) {
                            val revInputMap = revision.inputs.associate {
                                it.key to (it.rawValue ?: it.numericValue?.toString() ?: "")
                            }
                            currentInputs.all { (k, v) -> revInputMap[k] == v } &&
                                revInputMap.all { (k, v) -> currentInputs[k] == v }
                        }

                        val inputPreview = remember(revision.inputs) {
                            revision.inputs.sortedBy { it.sortOrder }
                                .joinToString(" · ") { "${it.label}: ${it.rawValue ?: it.numericValue?.toString() ?: "—"}${it.unit.orEmpty()}" }
                        }
                        val outputPreview = remember(revision.outputs) {
                            revision.outputs.sortedBy { it.sortOrder }
                                .joinToString(" · ") { "${it.label}: ${it.formattedValue}${it.unit.orEmpty()}" }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectRevision(revision) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentMatch) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                }
                            ),
                            border = BorderStroke(
                                width = if (isCurrentMatch) 1.dp else 0.5.dp,
                                color = if (isCurrentMatch) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                }
                            ),
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stringResource(Res.string.run_session_history_revision_number, revision.revisionNo),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    )
                                    Spacer(Modifier.width(6.dp))

                                    if (isLatest) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp),
                                        ) {
                                            Text(
                                                text = stringResource(Res.string.run_session_history_tag_latest),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            )
                                        }
                                        Spacer(Modifier.width(4.dp))
                                    } else if (isInitial) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(4.dp),
                                        ) {
                                            Text(
                                                text = stringResource(Res.string.run_session_history_tag_initial),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            )
                                        }
                                        Spacer(Modifier.width(4.dp))
                                    }

                                    if (isCurrentMatch) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(4.dp),
                                        ) {
                                            Text(
                                                text = stringResource(Res.string.run_session_history_applied),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            )
                                        }
                                    }

                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = formatEpochMillis(revision.updatedAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    )
                                }

                                if (revision.changedKeys.isNotEmpty()) {
                                    Spacer(Modifier.height(3.dp))
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "Δ ",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            text = revision.changedKeys.joinToString(", "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }

                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = inputPreview,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )

                                if (outputPreview.isNotBlank()) {
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = outputPreview,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }

                                Spacer(Modifier.height(6.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    FilledTonalButton(
                                        onClick = { onSelectRevision(revision) },
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        shape = RoundedCornerShape(6.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Undo,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(Res.string.run_session_history_apply),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatEpochMillis(epochMillis: Long): String {
    val dateTime = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dateTime.hour.pad()}:${dateTime.minute.pad()}:${dateTime.second.pad()}"
}

private fun Int.pad(): String = toString().padStart(2, '0')