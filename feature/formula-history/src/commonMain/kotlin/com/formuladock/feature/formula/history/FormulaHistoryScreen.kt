@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.formuladock.feature.formula.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.SwapVert

import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import com.formuladock.core.designsystem.component.FdSearchBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.formuladock.core.data.history.CalculationHistoryRepository
import com.formuladock.core.domain.history.DeleteCalculationHistoryUseCase
import com.formuladock.core.domain.history.GetCalculationHistoryListUseCase
import com.formuladock.core.model.history.model.CalculationHistory
import com.formuladock.core.model.history.model.CalculationHistoryInput
import com.formuladock.core.model.history.model.CalculationHistoryOutput
import com.formuladock.core.model.history.model.CalculationRevision
import com.formuladock.core.model.history.model.CalculationStatus
import kotlinx.coroutines.launch
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import com.formuladock.core.data.currentTimeMillis
import org.jetbrains.compose.resources.stringResource
import formuladock.feature.formula_history.generated.resources.*

@Composable
fun FormulaHistoryScreen(
    repository: CalculationHistoryRepository,
    formulaId: String?,
    formulaTitle: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEditCalculation: ((formulaId: String, inputs: Map<String, String>) -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    var histories by remember { mutableStateOf<List<CalculationHistory>>(emptyList()) }
    var selectedHistory by remember { mutableStateOf<CalculationHistory?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            runCatching { GetCalculationHistoryListUseCase(repository)(formulaId = formulaId) }
                .onSuccess {
                    histories = it
                    error = null
                }
                .onFailure { error = it.message ?: "LOAD_FAILED" }
        }
    }

    LaunchedEffect(repository, formulaId) { reload() }

    selectedHistory?.let { history ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.history_detail_title), style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = { IconButton(onClick = { selectedHistory = null }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back)) } },
                    actions = {
                        IconButton(onClick = {
                            val inputs = history.inputs.associate { it.key to (it.rawValue ?: "") }
                            onEditCalculation?.invoke(history.formulaId, inputs)
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.action_edit))
                        }
                        IconButton(onClick = {
                            scope.launch {
                                DeleteCalculationHistoryUseCase(repository)(history.id)
                                selectedHistory = null
                                reload()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.action_delete))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                )
            },
            modifier = modifier
        ) { innerPadding ->
            CompactHistoryDetailContent(
                history = history,
                modifier = Modifier.padding(innerPadding).padding(horizontal = 8.dp),
                onApplyRevision = onEditCalculation?.let { editCalculation ->
                    { revision ->
                        editCalculation(
                            history.formulaId,
                            revision.inputs.associate { it.key to (it.rawValue ?: "") },
                        )
                    }
                },
            )
        }
    } ?: Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(formulaTitle, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(Res.string.history_records_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            )
        },
        modifier = modifier
    ) { innerPadding ->
        CompactHistoryListContent(
            formulaId = formulaId,
            histories = histories,
            error = error,
            onItemClick = { summary ->
                scope.launch {
                    runCatching { repository.getHistory(summary.id) }
                        .onSuccess { detail -> selectedHistory = detail }
                        .onFailure { error = it.message ?: "LOAD_FAILED" }
                }
            },
            modifier = Modifier.padding(innerPadding).padding(horizontal = 8.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompactHistoryListContent(
    formulaId: String?,
    histories: List<CalculationHistory>,
    error: String?,
    onItemClick: (CalculationHistory) -> Unit,
    modifier: Modifier = Modifier,
    listMaxHeight: Dp = Dp.Unspecified,
    showSearchBar: Boolean = true
) {
    var filterState by remember { mutableStateOf(HistoryFilterState()) }
    var showBottomSheet by remember { mutableStateOf(false) }

    val filteredHistories = remember(histories, filterState) {
        val now = currentTimeMillis()
        histories.filter(filterState, now)
    }

    Column(modifier = modifier) {
        if (showSearchBar) {
            FdSearchBar(
                query = filterState.searchText,
                onQueryChange = { filterState = filterState.copy(searchText = it) },
                placeholder = stringResource(Res.string.history_search_placeholder),
                clearContentDescription = stringResource(Res.string.clear_search),
                filterContentDescription = stringResource(Res.string.history_advanced_filter),
                modifier = Modifier.padding(vertical = 6.dp),
                onFilterClick = { showBottomSheet = true },
                hasActiveFilters = filterState.hasActiveFilters
            )

            if (filterState.hasActiveFilters) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (filterState.status != CalculationStatusFilter.ALL) {
                        item {
                            MicroFilterChip(
                                label = filterState.status.label,
                                onDismiss = { filterState = filterState.copy(status = CalculationStatusFilter.ALL) }
                            )
                        }
                    }
                    if (filterState.timeRange != TimeRangeFilter.ALL) {
                        item {
                            MicroFilterChip(
                                label = filterState.timeRange.label,
                                onDismiss = { filterState = filterState.copy(timeRange = TimeRangeFilter.ALL) }
                            )
                        }
                    }
                    if (filterState.hasNoteOnly) {
                        item {
                            MicroFilterChip(
                                label = stringResource(Res.string.filter_note_only),
                                onDismiss = { filterState = filterState.copy(hasNoteOnly = false) }
                            )
                        }
                    }
                    if (formulaId == null && filterState.formulaType != FormulaTypeFilter.ALL) {
                        item {
                            MicroFilterChip(
                                label = filterState.formulaType.label,
                                onDismiss = { filterState = filterState.copy(formulaType = FormulaTypeFilter.ALL) }
                            )
                        }
                    }
                }
            }
        }

        val listModifier = Modifier.fillMaxWidth().then(
            if (listMaxHeight != Dp.Unspecified) {
                Modifier.heightIn(max = listMaxHeight)
            } else {
                Modifier.weight(1f)
            }
        )

        LazyColumn(
            modifier = listModifier,
            contentPadding = PaddingValues(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            error?.let {
                item {
                    val displayError = if (it == "LOAD_FAILED") stringResource(Res.string.history_load_failed) else it
                    Text(displayError, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
                }
            }
            if (filteredHistories.isEmpty() && error == null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (filterState.hasActiveFilters) stringResource(Res.string.history_no_matching_records) else stringResource(Res.string.history_no_records),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            items(filteredHistories, key = { it.id }) { history ->
                HistoryListItem(history = history, onClick = { onItemClick(history) })
            }
        }
    }

    if (showBottomSheet) {
        HistoryFilterBottomSheet(
            formulaId = formulaId,
            initialFilterState = filterState,
            onDismissRequest = { showBottomSheet = false },
            onApplyFilters = {
                filterState = it
                showBottomSheet = false
            }
        )
    }
}

@Composable
fun CompactHistoryDetailContent(
    history: CalculationHistory,
    modifier: Modifier = Modifier,
    maxHeight: Dp = Dp.Unspecified,
    onApplyRevision: ((CalculationRevision) -> Unit)? = null,
) {
    var baseRevisionId by remember(history.id) { mutableStateOf<String?>(null) }
    var targetRevisionId by remember(history.id) { mutableStateOf<String?>(null) }
    var showOnlyDiff by remember(history.id) { mutableStateOf(false) }

    val sortedRevisions = remember(history.revisions) {
        history.revisions.sortedByDescending { it.revisionNo }
    }
    val baseRevision = history.revisions.firstOrNull { it.id == baseRevisionId }
    val targetRevision = history.revisions.firstOrNull { it.id == targetRevisionId }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .then(if (maxHeight != Dp.Unspecified) Modifier.heightIn(max = maxHeight) else Modifier),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        item { HeaderSection(history) }
        if (!history.note.isNullOrBlank()) item { NoteSection(history.note!!) }
        item {
            Text(
                stringResource(Res.string.history_final_result),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
            )
        }
        if (history.inputs.isNotEmpty()) item { InputsSection(history.inputs) }
        if (history.status == CalculationStatus.SUCCESS) {
            if (history.outputs.isNotEmpty()) item { OutputsSection(history.outputs) }
        } else {
            item { ErrorSection(history.errorMessage ?: "未知错误", history.errorFieldKey) }
        }

        if (baseRevision != null && targetRevision != null && baseRevision.id != targetRevision.id) {
            item {
                RevisionDiffInspector(
                    baseRevision = baseRevision,
                    targetRevision = targetRevision,
                    showOnlyDiff = showOnlyDiff,
                    onToggleShowOnlyDiff = { showOnlyDiff = !showOnlyDiff },
                    onSwap = {
                        val temp = baseRevisionId
                        baseRevisionId = targetRevisionId
                        targetRevisionId = temp
                    },
                    onClose = {
                        baseRevisionId = null
                        targetRevisionId = null
                    },
                )
            }
        }

        if (sortedRevisions.isNotEmpty()) {
            item {
                RevisionTimelineSection(
                    revisions = sortedRevisions,
                    baseRevisionId = baseRevisionId,
                    targetRevisionId = targetRevisionId,
                    onSelectRevisionForCompare = { revId ->
                        when {
                            baseRevisionId == null && targetRevisionId == null -> {
                                baseRevisionId = revId
                            }
                            baseRevisionId == revId -> {
                                baseRevisionId = null
                            }
                            targetRevisionId == revId -> {
                                targetRevisionId = null
                            }
                            baseRevisionId != null && targetRevisionId == null -> {
                                targetRevisionId = revId
                            }
                            else -> {
                                targetRevisionId = revId
                            }
                        }
                    },
                    onQuickComparePrev = {
                        val latest = sortedRevisions.firstOrNull()
                        val prev = sortedRevisions.getOrNull(1)
                        if (latest != null && prev != null) {
                            baseRevisionId = prev.id
                            targetRevisionId = latest.id
                        }
                    },
                    onQuickCompareFirst = {
                        val latest = sortedRevisions.firstOrNull()
                        val first = sortedRevisions.lastOrNull()
                        if (latest != null && first != null && latest.id != first.id) {
                            baseRevisionId = first.id
                            targetRevisionId = latest.id
                        }
                    },
                    onApplyRevision = onApplyRevision,
                )
            }
        }
    }
}

data class HistoryFilterState(
    val searchText: String = "",
    val status: CalculationStatusFilter = CalculationStatusFilter.ALL,
    val timeRange: TimeRangeFilter = TimeRangeFilter.ALL,
    val hasNoteOnly: Boolean = false,
    val formulaType: FormulaTypeFilter = FormulaTypeFilter.ALL
) {
    val hasActiveFilters: Boolean
        get() = searchText.isNotEmpty() ||
                status != CalculationStatusFilter.ALL ||
                timeRange != TimeRangeFilter.ALL ||
                hasNoteOnly ||
                formulaType != FormulaTypeFilter.ALL
}

enum class CalculationStatusFilter {
    ALL, SUCCESS, FAILURE
}

val CalculationStatusFilter.label: String
    @Composable
    get() = when (this) {
        CalculationStatusFilter.ALL -> stringResource(Res.string.filter_status_all)
        CalculationStatusFilter.SUCCESS -> stringResource(Res.string.filter_status_success)
        CalculationStatusFilter.FAILURE -> stringResource(Res.string.filter_status_failure)
    }

enum class TimeRangeFilter {
    ALL, TODAY, LAST_7_DAYS, LAST_30_DAYS
}

val TimeRangeFilter.label: String
    @Composable
    get() = when (this) {
        TimeRangeFilter.ALL -> stringResource(Res.string.filter_time_all)
        TimeRangeFilter.TODAY -> stringResource(Res.string.filter_time_today)
        TimeRangeFilter.LAST_7_DAYS -> stringResource(Res.string.filter_time_7_days)
        TimeRangeFilter.LAST_30_DAYS -> stringResource(Res.string.filter_time_30_days)
    }

enum class FormulaTypeFilter {
    ALL, BUILTIN, CUSTOM
}

val FormulaTypeFilter.label: String
    @Composable
    get() = when (this) {
        FormulaTypeFilter.ALL -> stringResource(Res.string.filter_type_all)
        FormulaTypeFilter.BUILTIN -> stringResource(Res.string.filter_type_builtin)
        FormulaTypeFilter.CUSTOM -> stringResource(Res.string.filter_type_custom)
    }

fun List<CalculationHistory>.filter(state: HistoryFilterState, currentTimeMillis: Long): List<CalculationHistory> {
    return this.filter { history ->
        if (state.searchText.isNotBlank()) {
            val text = state.searchText.trim()
            val matchesTitle = history.formulaTitle.contains(text, ignoreCase = true)
            val matchesNote = history.note?.contains(text, ignoreCase = true) == true
            val matchesInputs = history.inputs.any { it.label.contains(text, ignoreCase = true) || it.rawValue?.contains(text, ignoreCase = true) == true }
            val matchesOutputs = history.outputs.any { it.label.contains(text, ignoreCase = true) || it.formattedValue.contains(text, ignoreCase = true) }
            val matchesError = history.errorMessage?.contains(text, ignoreCase = true) == true

            if (!matchesTitle && !matchesNote && !matchesInputs && !matchesOutputs && !matchesError) {
                return@filter false
            }
        }

        when (state.status) {
            CalculationStatusFilter.SUCCESS -> if (history.status != CalculationStatus.SUCCESS) return@filter false
            CalculationStatusFilter.FAILURE -> if (history.status == CalculationStatus.SUCCESS) return@filter false
            CalculationStatusFilter.ALL -> {}
        }

        val ageMillis = currentTimeMillis - history.createdAt
        val oneDayMillis = 24 * 60 * 60 * 1000L
        when (state.timeRange) {
            TimeRangeFilter.TODAY -> if (ageMillis > oneDayMillis) return@filter false
            TimeRangeFilter.LAST_7_DAYS -> if (ageMillis > 7 * oneDayMillis) return@filter false
            TimeRangeFilter.LAST_30_DAYS -> if (ageMillis > 30 * oneDayMillis) return@filter false
            TimeRangeFilter.ALL -> {}
        }

        if (state.hasNoteOnly && history.note.isNullOrBlank()) {
            return@filter false
        }

        when (state.formulaType) {
            FormulaTypeFilter.BUILTIN -> if (!history.formulaIsBuiltin) return@filter false
            FormulaTypeFilter.CUSTOM -> if (history.formulaIsBuiltin) return@filter false
            FormulaTypeFilter.ALL -> {}
        }

        true
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryFilterBottomSheet(
    formulaId: String?,
    initialFilterState: HistoryFilterState,
    onDismissRequest: () -> Unit,
    onApplyFilters: (HistoryFilterState) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var tempState by remember { mutableStateOf(initialFilterState) }

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
            Text(
                text = stringResource(Res.string.history_advanced_filter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            FilterSectionHeader(stringResource(Res.string.filter_status))
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalculationStatusFilter.entries.forEach { statusOption ->
                    FilterOptionChip(
                        selected = tempState.status == statusOption,
                        onClick = { tempState = tempState.copy(status = statusOption) },
                        label = statusOption.label
                    )
                }
            }

            FilterSectionHeader(stringResource(Res.string.history_filter_time_range))
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeRangeFilter.entries.forEach { timeOption ->
                    FilterOptionChip(
                        selected = tempState.timeRange == timeOption,
                        onClick = { tempState = tempState.copy(timeRange = timeOption) },
                        label = timeOption.label
                    )
                }
            }

            if (formulaId == null) {
                FilterSectionHeader(stringResource(Res.string.history_filter_formula_type))
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FormulaTypeFilter.entries.forEach { typeOption ->
                        FilterOptionChip(
                            selected = tempState.formulaType == typeOption,
                            onClick = { tempState = tempState.copy(formulaType = typeOption) },
                            label = typeOption.label
                        )
                    }
                }
            }

            FilterSectionHeader(stringResource(Res.string.history_filter_other_options))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { tempState = tempState.copy(hasNoteOnly = !tempState.hasNoteOnly) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(Res.string.history_show_notes_only), style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = tempState.hasNoteOnly,
                    onCheckedChange = { tempState = tempState.copy(hasNoteOnly = it) }
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { tempState = HistoryFilterState(searchText = tempState.searchText) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.action_reset))
                }
                Button(
                    onClick = { onApplyFilters(tempState) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.action_confirm))
                }
            }
        }
    }
}

@Composable
private fun FilterSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun FilterOptionChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
private fun HistoryListItem(history: CalculationHistory, onClick: () -> Unit) {
    val isSuccess = history.status == CalculationStatus.SUCCESS
    val statusColor = if (isSuccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
    val noOutputLabel = stringResource(Res.string.history_no_output)
    val unknownErrorLabel = stringResource(Res.string.history_unknown_error)
    val preview = remember(history, noOutputLabel, unknownErrorLabel) {
        if (isSuccess) {
            history.outputs.joinToString(", ") { "${it.label}: ${it.formattedValue}${it.unit ?: ""}" }.ifBlank { noOutputLabel }
        } else {
            history.errorMessage ?: unknownErrorLabel
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isSuccess) Icons.Default.Check else Icons.Default.Warning, null, tint = statusColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${formatEpochMillis(history.startedAt)} – ${formatEpochMillis(history.updatedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!history.note.isNullOrBlank()) Icon(Icons.Default.Edit, stringResource(Res.string.history_has_note), modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(2.dp))
                Text(preview, style = MaterialTheme.typography.bodySmall, color = if (isSuccess) Color.Unspecified else MaterialTheme.colorScheme.error, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(Res.string.history_revision_count, history.revisionCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun HeaderSection(history: CalculationHistory) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), shape = MaterialTheme.shapes.small) {
        Column(Modifier.padding(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(history.formulaTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                StatusBadge(history.status)
            }
            if (!history.formulaDescription.isNullOrBlank()) Text(history.formulaDescription!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (history.formulaIsBuiltin) stringResource(Res.string.badge_builtin) else stringResource(Res.string.badge_custom), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatEpochMillis(history.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private enum class DeltaType {
    POSITIVE, NEGATIVE, NEUTRAL
}

private data class FieldDiffItem(
    val label: String,
    val key: String,
    val valueBase: String,
    val valueTarget: String,
    val deltaText: String?,
    val deltaPercentText: String?,
    val isModified: Boolean,
    val deltaType: DeltaType,
    val subText: String? = null,
)

private fun formatNumberDelta(delta: Double): String {
    val sign = if (delta > 0) "+" else if (delta < 0) "-" else "±"
    val absVal = kotlin.math.abs(delta)
    val formatted = if (absVal == kotlin.math.floor(absVal) && absVal < 1e9) {
        absVal.toLong().toString()
    } else {
        val factor = 100.0
        val rounded = kotlin.math.round(absVal * factor) / factor
        rounded.toString()
    }
    return "$sign$formatted"
}

private fun formatPercentDelta(percent: Double): String {
    val sign = if (percent > 0) "+" else if (percent < 0) "-" else "±"
    val absVal = kotlin.math.abs(percent)
    val rounded = kotlin.math.round(absVal * 10.0) / 10.0
    return "$sign$rounded%"
}

private fun buildInputsDiff(
    inputsA: List<CalculationHistoryInput>,
    inputsB: List<CalculationHistoryInput>,
): List<FieldDiffItem> {
    val mapA = inputsA.associateBy { it.key }
    val mapB = inputsB.associateBy { it.key }
    val keys = (inputsA.sortedBy { it.sortOrder }.map { it.key } +
        inputsB.sortedBy { it.sortOrder }.map { it.key }).distinct()

    return keys.map { key ->
        val itemA = mapA[key]
        val itemB = mapB[key]
        val label = itemA?.label ?: itemB?.label ?: key
        val valAStr = itemA?.displayComparisonValue().orEmpty().ifBlank { "—" }
        val valBStr = itemB?.displayComparisonValue().orEmpty().ifBlank { "—" }
        val isModified = valAStr != valBStr

        val numA = itemA?.numericValue ?: itemA?.rawValue?.toDoubleOrNull()
        val numB = itemB?.numericValue ?: itemB?.rawValue?.toDoubleOrNull()

        var deltaText: String? = null
        var deltaPercentText: String? = null
        var deltaType = DeltaType.NEUTRAL

        if (numA != null && numB != null) {
            val diff = numB - numA
            if (diff != 0.0) {
                deltaText = formatNumberDelta(diff) + (itemB?.unit ?: itemA?.unit)?.let { " $it" }.orEmpty()
                deltaPercentText = if (numA != 0.0) formatPercentDelta((diff / kotlin.math.abs(numA)) * 100.0) else null
                deltaType = if (diff > 0) DeltaType.POSITIVE else DeltaType.NEGATIVE
            }
        }

        FieldDiffItem(
            label = label,
            key = key,
            valueBase = valAStr,
            valueTarget = valBStr,
            deltaText = deltaText,
            deltaPercentText = deltaPercentText,
            isModified = isModified,
            deltaType = deltaType,
        )
    }
}

private fun buildOutputsDiff(
    outputsA: List<CalculationHistoryOutput>,
    outputsB: List<CalculationHistoryOutput>,
): List<FieldDiffItem> {
    val mapA = outputsA.associateBy { it.key }
    val mapB = outputsB.associateBy { it.key }
    val keys = (outputsA.sortedBy { it.sortOrder }.map { it.key } +
        outputsB.sortedBy { it.sortOrder }.map { it.key }).distinct()

    return keys.map { key ->
        val itemA = mapA[key]
        val itemB = mapB[key]
        val label = itemA?.label ?: itemB?.label ?: key
        val valAStr = itemA?.let { "${it.formattedValue}${it.unit?.let { unit -> " $unit" }.orEmpty()}" } ?: "—"
        val valBStr = itemB?.let { "${it.formattedValue}${it.unit?.let { unit -> " $unit" }.orEmpty()}" } ?: "—"
        val isModified = valAStr != valBStr

        val numA = itemA?.value
        val numB = itemB?.value

        var deltaText: String? = null
        var deltaPercentText: String? = null
        var deltaType = DeltaType.NEUTRAL

        val expr = itemB?.expression ?: itemA?.expression
        val unit = itemB?.unit ?: itemA?.unit

        if (numA != null && numB != null) {
            val diff = numB - numA
            if (diff != 0.0) {
                deltaText = formatNumberDelta(diff) + (!unit.isNullOrBlank()).let { if (it) " $unit" else "" }
                deltaPercentText = if (numA != 0.0) formatPercentDelta((diff / kotlin.math.abs(numA)) * 100.0) else null
                deltaType = if (diff > 0) DeltaType.POSITIVE else DeltaType.NEGATIVE
            }
        }

        FieldDiffItem(
            label = label,
            key = key,
            valueBase = valAStr,
            valueTarget = valBStr,
            deltaText = deltaText,
            deltaPercentText = deltaPercentText,
            isModified = isModified,
            deltaType = deltaType,
            subText = expr?.takeIf { it.isNotBlank() }?.let { "公式: $it" },
        )
    }
}

@Composable
private fun RevisionDiffInspector(
    baseRevision: CalculationRevision,
    targetRevision: CalculationRevision,
    showOnlyDiff: Boolean,
    onToggleShowOnlyDiff: () -> Unit,
    onSwap: () -> Unit,
    onClose: () -> Unit,
) {
    val inputsDiff = remember(baseRevision, targetRevision) {
        buildInputsDiff(baseRevision.inputs, targetRevision.inputs)
    }
    val outputsDiff = remember(baseRevision, targetRevision) {
        buildOutputsDiff(baseRevision.outputs, targetRevision.outputs)
    }
    val totalModifiedCount = inputsDiff.count { it.isModified } + outputsDiff.count { it.isModified }
    val displayedInputs = if (showOnlyDiff) inputsDiff.filter { it.isModified } else inputsDiff
    val displayedOutputs = if (showOnlyDiff) outputsDiff.filter { it.isModified } else outputsDiff

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
    ) {
        Column(Modifier.padding(10.dp)) {
            // Inspector Header
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(26.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(Res.string.history_version_comparison),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSwap,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Default.SwapVert,
                            contentDescription = stringResource(Res.string.history_swap_comparison),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(Res.string.history_exit_comparison),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Version Flow Badges
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Base Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    ) {
                        Text(
                            stringResource(Res.string.history_compare_base),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(Res.string.history_revision_number, baseRevision.revisionNo),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    )
                }

                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )

                // Target Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            stringResource(Res.string.history_compare_target),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(Res.string.history_revision_number, targetRevision.revisionNo),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Filter Chip for showing only changes vs all
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = showOnlyDiff,
                    onClick = onToggleShowOnlyDiff,
                    label = {
                        Text(
                            if (showOnlyDiff) {
                                stringResource(Res.string.history_filter_only_diff)
                            } else {
                                stringResource(Res.string.history_filter_all_fields)
                            } + " ($totalModifiedCount)",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    modifier = Modifier.height(26.dp),
                )
            }

            Spacer(Modifier.height(6.dp))

            if (totalModifiedCount == 0 && showOnlyDiff) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f)
                    ),
                ) {
                    Row(
                        Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(Res.string.history_no_diff_found),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                if (displayedInputs.isNotEmpty()) {
                    Text(
                        stringResource(Res.string.history_comparison_inputs),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                    displayedInputs.forEach { item ->
                        DiffFieldItem(item)
                    }
                }

                if (displayedOutputs.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(Res.string.history_comparison_outputs),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                    displayedOutputs.forEach { item ->
                        DiffFieldItem(item)
                    }
                }
            }
        }
    }
}

private fun CalculationHistoryInput.displayComparisonValue(): String {
    val value = rawValue ?: numericValue?.toString() ?: return "—"
    return "$value${unit?.let { " $it" }.orEmpty()}"
}

@Composable
private fun DiffFieldItem(item: FieldDiffItem) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isModified) {
                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.25f)
            }
        ),
        border = BorderStroke(
            width = if (item.isModified) 1.dp else 0.5.dp,
            color = if (item.isModified) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            }
        ),
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                if (item.isModified) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.history_tag_modified),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp),
                        )
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.history_tag_unchanged),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(3.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Text(
                        text = item.valueBase,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 4.dp).size(12.dp),
                    )
                    Text(
                        text = item.valueTarget,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (item.isModified) FontWeight.Bold else FontWeight.Normal,
                            color = if (item.isModified) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (item.deltaText != null) {
                    Spacer(Modifier.width(6.dp))
                    val (badgeBg, badgeText) = when (item.deltaType) {
                        DeltaType.POSITIVE -> Color(0xFF10B981).copy(alpha = 0.15f) to Color(0xFF059669)
                        DeltaType.NEGATIVE -> Color(0xFFEF4444).copy(alpha = 0.15f) to Color(0xFFDC2626)
                        DeltaType.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = if (item.deltaPercentText != null) "${item.deltaText} (${item.deltaPercentText})" else item.deltaText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = badgeText,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
            }

            if (!item.subText.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.subText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RevisionTimelineSection(
    revisions: List<CalculationRevision>,
    baseRevisionId: String?,
    targetRevisionId: String?,
    onSelectRevisionForCompare: (String) -> Unit,
    onQuickComparePrev: () -> Unit,
    onQuickCompareFirst: () -> Unit,
    onApplyRevision: ((CalculationRevision) -> Unit)?,
) {
    val maxRevNo = revisions.maxOfOrNull { it.revisionNo } ?: 0
    val minRevNo = revisions.minOfOrNull { it.revisionNo } ?: 0

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(Res.string.history_calculation_process),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(Res.string.history_revision_count, revisions.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (revisions.size >= 2) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onQuickComparePrev,
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                ) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(Res.string.history_compare_with_prev),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (revisions.size >= 3) {
                    OutlinedButton(
                        onClick = onQuickCompareFirst,
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(Res.string.history_compare_with_first),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        revisions.forEachIndexed { index, revision ->
            val isFirstItem = index == 0
            val isLastItem = index == revisions.lastIndex
            val isLatest = revision.revisionNo == maxRevNo
            val isInitial = revision.revisionNo == minRevNo
            val isBase = revision.id == baseRevisionId
            val isTarget = revision.id == targetRevisionId
            val isSelected = isBase || isTarget

            val inputPreview = revision.inputs.sortedBy { it.sortOrder }
                .joinToString(" · ") { "${it.label} ${it.rawValue ?: it.numericValue ?: "—"}${it.unit.orEmpty()}" }
            val outputPreview = revision.outputs.sortedBy { it.sortOrder }
                .joinToString(" · ") { "${it.label} ${it.formattedValue}${it.unit.orEmpty()}" }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier.width(24.dp).heightIn(min = 72.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerX = size.width / 2
                        val nodeY = 14.dp.toPx()
                        val lineColor = Color.Gray.copy(alpha = 0.25f)
                        val strokeW = 1.5.dp.toPx()

                        if (!isFirstItem) {
                            drawLine(
                                color = lineColor,
                                start = androidx.compose.ui.geometry.Offset(centerX, 0f),
                                end = androidx.compose.ui.geometry.Offset(centerX, nodeY),
                                strokeWidth = strokeW,
                            )
                        }
                        if (!isLastItem) {
                            drawLine(
                                color = lineColor,
                                start = androidx.compose.ui.geometry.Offset(centerX, nodeY),
                                end = androidx.compose.ui.geometry.Offset(centerX, size.height),
                                strokeWidth = strokeW,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(12.dp)
                            .background(
                                color = when {
                                    isTarget -> MaterialTheme.colorScheme.primary
                                    isBase -> MaterialTheme.colorScheme.surface
                                    isLatest -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = CircleShape,
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected || isLatest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape,
                            )
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        }
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 1.dp else 0.5.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        }
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(Res.string.history_revision_number, revision.revisionNo),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            )
                            Spacer(Modifier.width(6.dp))

                            if (isLatest) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp),
                                ) {
                                    Text(
                                        stringResource(Res.string.history_tag_latest),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp),
                                    )
                                }
                            } else if (isInitial) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp),
                                ) {
                                    Text(
                                        stringResource(Res.string.history_tag_initial),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp),
                                    )
                                }
                            }

                            if (isBase) {
                                Spacer(Modifier.width(4.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(4.dp),
                                ) {
                                    Text(
                                        stringResource(Res.string.history_compare_base),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp),
                                    )
                                }
                            }
                            if (isTarget) {
                                Spacer(Modifier.width(4.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp),
                                ) {
                                    Text(
                                        stringResource(Res.string.history_compare_target),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp),
                                    )
                                }
                            }

                            Spacer(Modifier.weight(1f))
                            Text(
                                formatEpochMillis(revision.updatedAt),
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
                                    "Δ ",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    revision.changedKeys.joinToString(", "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        Spacer(Modifier.height(2.dp))
                        Text(
                            inputPreview,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        if (outputPreview.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                outputPreview,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedButton(
                                onClick = { onSelectRevisionForCompare(revision.id) },
                                modifier = Modifier.height(26.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = if (isSelected) {
                                    androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    )
                                } else androidx.compose.material3.ButtonDefaults.outlinedButtonColors(),
                                border = BorderStroke(
                                    0.5.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                ),
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.Check else Icons.AutoMirrored.Filled.CompareArrows,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = when {
                                        isBase -> stringResource(Res.string.history_compare_base)
                                        isTarget -> stringResource(Res.string.history_compare_target)
                                        else -> stringResource(Res.string.history_compare_action)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            if (onApplyRevision != null) {
                                FilledTonalButton(
                                    onClick = { onApplyRevision(revision) },
                                    modifier = Modifier.height(26.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(6.dp),
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        stringResource(Res.string.history_apply_short),
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

@Composable
private fun StatusBadge(status: CalculationStatus) {
    val success = status == CalculationStatus.SUCCESS
    val color = if (success) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp), border = BorderStroke(0.5.dp, color.copy(alpha = 0.4f))) {
        Text(if (success) stringResource(Res.string.history_status_success) else stringResource(Res.string.history_status_failed), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = color, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
    }
}

@Composable
private fun NoteSection(note: String) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)), shape = MaterialTheme.shapes.small) {
        Row(Modifier.padding(6.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Edit, stringResource(Res.string.history_note_label), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Column {
                Text(stringResource(Res.string.history_note_label), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                Text(note, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun InputsSection(inputs: List<CalculationHistoryInput>) {
    SnapshotTable(title = stringResource(Res.string.history_inputs_count, inputs.size)) {
        inputs.sortedBy { it.sortOrder }.forEachIndexed { index, input ->
            SnapshotRow(label = input.label + if (input.required) " *" else "", value = "${input.rawValue ?: input.numericValue?.toString() ?: "—"}${input.unit?.let { " $it" } ?: ""}")
            if (index < inputs.lastIndex) ThinDivider()
        }
    }
}

@Composable
private fun OutputsSection(outputs: List<CalculationHistoryOutput>) {
    SnapshotTable(title = stringResource(Res.string.history_outputs_count, outputs.size)) {
        outputs.sortedBy { it.sortOrder }.forEachIndexed { index, output ->
            Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                SnapshotRow(label = output.label, value = "${output.formattedValue}${output.unit?.let { " $it" } ?: ""}", primary = true)
                if (output.expression.isNotBlank()) Text(stringResource(Res.string.history_formula_expression, output.expression), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (index < outputs.lastIndex) ThinDivider()
        }
    }
}

@Composable
private fun SnapshotTable(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp))
        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small, border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) { content() }
        }
    }
}

@Composable
private fun SnapshotRow(label: String, value: String, primary: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.4f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = if (primary) FontWeight.Bold else FontWeight.SemiBold, color = if (primary) MaterialTheme.colorScheme.primary else Color.Unspecified), modifier = Modifier.weight(0.6f), textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ThinDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)
}

@Composable
private fun ErrorSection(errorMessage: String, errorFieldKey: String?) {
    Column(Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.history_error_log), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp))
        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)), border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))) {
            Column(Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(Res.string.history_calculation_exception), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.error)
                }
                Text(errorMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                if (!errorFieldKey.isNullOrBlank()) Text(stringResource(Res.string.history_error_field, errorFieldKey), style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun MicroFilterChip(
    label: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        modifier = modifier.height(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.action_remove_filter),
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(10.dp)
                    .clickable(onClick = onDismiss)
            )
        }
    }
}

private fun formatEpochMillis(epochMillis: Long): String {
    val dateTime = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dateTime.year}-${dateTime.month.number.pad()}-${dateTime.day.pad()} " +
        "${dateTime.hour.pad()}:${dateTime.minute.pad()}"
}

private fun Int.pad(): String = toString().padStart(2, '0')
