package com.formuladock.feature.formula.run

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.formuladock.core.domain.formula.EvaluateFormulaUseCase
import com.formuladock.core.model.formula.model.FormulaDefinition
import com.formuladock.core.formula.engine.FormulaEvaluationResult
import com.formuladock.core.data.history.CalculationHistoryRepository
import com.formuladock.feature.formula.io.FormulaClipboardService
import com.formuladock.feature.formula.io.LocalFormulaShareService
import com.formuladock.feature.formula.io.SingleFormulaExportSheet
import org.jetbrains.compose.resources.stringResource
import formuladock.feature.formula_run.generated.resources.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

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

    LaunchedEffect(formula, inputValues, result) {
        if (result is FormulaEvaluationResult.Success) {
            delay(1000L.milliseconds)
            historyRepository.saveSuccessfulRun(formula, inputValues, result)
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
                    IconButton(onClick = onBack) {
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
                        onDelete()
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
}