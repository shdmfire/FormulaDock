package com.formuladock.feature.formula.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import com.formuladock.core.designsystem.component.FdSearchBar
import com.formuladock.core.designsystem.component.LocalAppInForeground
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.model.formula.model.FormulaDefinition
import com.formuladock.feature.formula.run.CalculationSessionRecorder
import com.formuladock.feature.formula.run.CompactCalculatorContent
import com.formuladock.feature.formula.run.rememberFormulaCalculatorState
import com.formuladock.core.domain.formula.EvaluateFormulaUseCase
import com.formuladock.core.domain.formula.panel.LoadFormulaPanelUseCase
import com.formuladock.core.data.history.CalculationHistoryRepository
import com.formuladock.core.formula.engine.DefaultFormulaEngine
import com.formuladock.core.formula.engine.FormulaEvaluationResult
import com.formuladock.core.model.history.model.CalculationHistory
import com.formuladock.core.domain.history.GetCalculationHistoryListUseCase
import com.formuladock.feature.formula.history.CompactHistoryListContent
import com.formuladock.feature.formula.history.CompactHistoryDetailContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flowOf
import com.formuladock.core.preferences.FormulaDockPreferences
import kotlin.time.Duration.Companion.milliseconds
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
private sealed interface PanelRoute : NavKey {
    @Serializable
    data object Main : PanelRoute
    @Serializable
    data object Selector : PanelRoute
}

@Composable
fun FormulaCalculatorPanel(
    repository: FormulaRepository,
    modifier: Modifier = Modifier,
    defaultFormulaId: String? = null,
    fallbackFormula: FormulaDefinition? = null,
    historyRepository: CalculationHistoryRepository? = null,
    onClose: (() -> Unit)? = null,
    onContentReady: (() -> Unit)? = null
) {
    val preferences = remember { FormulaDockPreferences() }
    var allFormulas by remember { mutableStateOf(emptyList<FormulaDefinition>()) }
    var currentFormula by remember { mutableStateOf<FormulaDefinition?>(null) }

    val panelNavConfig = remember {
        androidx.savedstate.serialization.SavedStateConfiguration {
            serializersModule = kotlinx.serialization.modules.SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(PanelRoute.Main.serializer())
                    subclass(PanelRoute.Selector.serializer())
                }
            }
        }
    }
    val panelBackStack = rememberNavBackStack(panelNavConfig, PanelRoute.Main)

    LaunchedEffect(repository, defaultFormulaId, fallbackFormula) {
        val targetFlow = if (defaultFormulaId != null) {
            flowOf(defaultFormulaId)
        } else {
            preferences.defaultFormulaId
        }
        targetFlow.collect { targetId ->
            val loadResult = LoadFormulaPanelUseCase(
                repository = repository,
                defaultFormulaId = targetId,
            )()
            val defaultFormula = loadResult.selectedFormula ?: fallbackFormula

            allFormulas = loadResult.formulas.ifEmpty { fallbackFormula?.let(::listOf).orEmpty() }
            currentFormula = defaultFormula
        }
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ) {
        val formula = currentFormula

        if (formula == null) {
            // 不能让首次测量高度为 0
            Box(
                modifier = Modifier
                    .width(360.dp)
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            // formula 已经进入 Compose 布局后再通知窗口重新适配
            LaunchedEffect(formula.id) {
                withFrameNanos { }
                onContentReady?.invoke()
            }

            NavDisplay(
                backStack = panelBackStack,
                entryProvider = entryProvider {
                    entry<PanelRoute.Main> {
                        QuickCalculatorContent(
                            formula = formula,
                            onTitleClick = {
                                panelBackStack.add(PanelRoute.Selector)
                            },
                            historyRepository = historyRepository,
                            onClose = onClose
                        )
                    }

                    entry<PanelRoute.Selector> {
                        FormulaInlineSelector(
                            formulas = allFormulas,
                            onSelect = { selected ->
                                currentFormula = selected
                                panelBackStack.removeLastOrNull()
                            },
                            onBack = {
                                panelBackStack.removeLastOrNull()
                            }
                        )
                    }
                }
            )
        }
    }
}

@Serializable
private sealed interface QuickCalcRoute : NavKey {
    @Serializable
    data object Calculator : QuickCalcRoute
    @Serializable
    data object HistoryList : QuickCalcRoute
    @Serializable
    data class HistoryDetail(val historyId: String) : QuickCalcRoute
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickCalculatorContent(
    formula: FormulaDefinition,
    onTitleClick: () -> Unit,
    modifier: Modifier = Modifier,
    historyRepository: CalculationHistoryRepository? = null,
    onClose: (() -> Unit)? = null
) {
    val evaluateFormula = remember { EvaluateFormulaUseCase(DefaultFormulaEngine()) }
    val calculatorState = rememberFormulaCalculatorState(formula, evaluateFormula)
    val scope = rememberCoroutineScope()
    val sessionRecorder = remember(historyRepository, formula.id) {
        historyRepository?.let(::CalculationSessionRecorder)
    }
    var sessionReady by remember(historyRepository, formula.id) { mutableStateOf(false) }
    val appInForeground = LocalAppInForeground.current

    LaunchedEffect(sessionRecorder, formula.id, appInForeground) {
        if (appInForeground) {
            sessionRecorder?.startOrResume(formula)
            sessionReady = sessionRecorder != null
        } else if (sessionReady) {
            val result = calculatorState.result
            if (result is FormulaEvaluationResult.Success) {
                sessionRecorder?.commit(formula, calculatorState.inputValues, result)
            }
            sessionRecorder?.pause()
        }
    }

    LaunchedEffect(formula, calculatorState.inputValues, calculatorState.result, sessionReady, appInForeground) {
        val result = calculatorState.result
        if (appInForeground && sessionReady && result is FormulaEvaluationResult.Success) {
            delay(1000L.milliseconds)
            sessionRecorder?.commit(formula, calculatorState.inputValues, result)
        }
    }

    val commitCurrentDraft: () -> Unit = {
        if (sessionReady) {
            scope.launch {
                val result = calculatorState.result
                if (result is FormulaEvaluationResult.Success) {
                    sessionRecorder?.commit(formula, calculatorState.inputValues, result)
                }
            }
        }
    }
    val pauseAndSwitch: () -> Unit = {
        scope.launch {
            val result = calculatorState.result
            if (result is FormulaEvaluationResult.Success) {
                sessionRecorder?.commit(formula, calculatorState.inputValues, result)
            }
            sessionRecorder?.pause()
            onTitleClick()
        }
    }
    val closeSessionAndPanel: (() -> Unit)? = onClose?.let { closePanel ->
        {
            scope.launch {
                val result = calculatorState.result
                if (result is FormulaEvaluationResult.Success) {
                    sessionRecorder?.commit(formula, calculatorState.inputValues, result)
                }
                sessionRecorder?.close()
                closePanel()
            }
        }
    }

    val subNavConfig = remember {
        androidx.savedstate.serialization.SavedStateConfiguration {
            serializersModule = kotlinx.serialization.modules.SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(QuickCalcRoute.Calculator.serializer())
                    subclass(QuickCalcRoute.HistoryList.serializer())
                    subclass(QuickCalcRoute.HistoryDetail.serializer())
                }
            }
        }
    }
    val subBackStack = rememberNavBackStack(subNavConfig, QuickCalcRoute.Calculator)
    val currentRoute = subBackStack.last() as? QuickCalcRoute ?: QuickCalcRoute.Calculator

    val isShowingHistory = currentRoute is QuickCalcRoute.HistoryList || currentRoute is QuickCalcRoute.HistoryDetail

    // 获取历史数据
    var histories by remember { mutableStateOf<List<CalculationHistory>>(emptyList()) }
    LaunchedEffect(formula.id, isShowingHistory, historyRepository) {
        if (isShowingHistory && historyRepository != null) {
            runCatching { GetCalculationHistoryListUseCase(historyRepository)(formulaId = formula.id) }
                .onSuccess { histories = it }
        }
    }

    LaunchedEffect(formula.id) {
        while (subBackStack.size > 1) {
            subBackStack.removeLastOrNull()
        }
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 顶部精简栏：将标题、描述、切换/返回按钮、关闭按钮融合在一行内，并通过双胶囊极大节省高度空间
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val subTitle = when (currentRoute) {
                    is QuickCalcRoute.HistoryDetail -> "历史详情"
                    is QuickCalcRoute.HistoryList -> "计算历史"
                    else -> formula.description
                }
                Text(
                    text = formula.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                subTitle?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (currentRoute is QuickCalcRoute.HistoryList || currentRoute is QuickCalcRoute.HistoryDetail) {
                    // A. 历史模式：显示微型“返回”胶囊
                    Surface(
                        onClick = {
                            subBackStack.removeLastOrNull()
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = "返回",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    // B. 计算器模式：显示“双胶囊并排”工具栏
                    if (historyRepository != null) {
                        // 1. “历史”胶囊按钮（辅助色调 + 迷你Icon）
                        Surface(
                            onClick = { subBackStack.add(QuickCalcRoute.HistoryList) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "历史记录",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "历史",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 2. “切换”胶囊按钮（主色调）
                    Surface(
                        onClick = pauseAndSwitch,
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    ) {
                        Text(
                            text = "切换",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // C. 极简关闭按钮（高度进一步微缩至 24.dp）
                if (closeSessionAndPanel != null) {
                    IconButton(
                        onClick = closeSessionAndPanel,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 面板中部的动态替换区
        NavDisplay(
            backStack = subBackStack,
            entryProvider = entryProvider {
                entry<QuickCalcRoute.Calculator> {
                    CompactCalculatorContent(
                        formula = formula,
                        inputValues = calculatorState.inputValues,
                        result = calculatorState.result,
                        onInputValuesChange = calculatorState::updateInputValues,
                        onCommitRequested = commitCurrentDraft,
                    )
                }
                entry<QuickCalcRoute.HistoryList> {
                    CompactHistoryListContent(
                        formulaId = formula.id,
                        histories = histories,
                        error = null,
                        onItemClick = { summary ->
                            scope.launch {
                                historyRepository?.getHistory(summary.id)?.let { detail ->
                                    histories = histories.map { if (it.id == detail.id) detail else it }
                                    subBackStack.add(QuickCalcRoute.HistoryDetail(detail.id))
                                }
                            }
                        },
                        listMaxHeight = 200.dp,
                        showSearchBar = false // 极小面板，关闭搜索框，实现极致简洁
                    )
                }
                entry<QuickCalcRoute.HistoryDetail> { route ->
                    val history = histories.find { it.id == route.historyId }
                    if (history != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 提供一个“一键反填”按钮，帮助用户将这条历史记录的输入覆盖当前计算器
                            OutlinedButton(
                                onClick = {
                                    val restoredInputs = history.inputs.associate { it.key to (it.rawValue ?: "") }
                                    calculatorState.updateInputValues(restoredInputs)
                                    while (subBackStack.size > 1) {
                                        subBackStack.removeLastOrNull()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(32.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("应用此组参数至计算器", style = MaterialTheme.typography.labelMedium)
                            }

                            // 纯净详情，最大高度限制为 180.dp，超过则内部自然滚动
                            CompactHistoryDetailContent(
                                history = history,
                                maxHeight = 180.dp,
                                onApplyRevision = { revision ->
                                    calculatorState.updateInputValues(
                                        revision.inputs.associate { it.key to (it.rawValue ?: "") }
                                    )
                                    while (subBackStack.size > 1) {
                                        subBackStack.removeLastOrNull()
                                    }
                                },
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun FormulaInlineSelector(
    formulas: List<FormulaDefinition>,
    onSelect: (FormulaDefinition) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredFormulas = remember(searchQuery, formulas) {
        if (searchQuery.isBlank()) formulas else {
            formulas.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        (it.description?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "选择计算公式",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                onClick = onBack,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "返回",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // 扁平化极窄搜索框
        FdSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "搜索公式...",
            clearContentDescription = "清除",
            modifier = Modifier.fillMaxWidth()
        )

        // 列表区域
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filteredFormulas, key = { it.id }) { formula ->
                Surface(
                    onClick = { onSelect(formula) },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = formula.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        formula.description?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
