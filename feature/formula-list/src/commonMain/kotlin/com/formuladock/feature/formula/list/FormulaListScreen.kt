@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.formuladock.feature.formula.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.formuladock.core.designsystem.component.FdSearchBar
import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.domain.formula.DeleteFormulaUseCase
import com.formuladock.core.domain.formula.GetFormulaListUseCase
import com.formuladock.core.model.formula.model.FormulaDefinition
import com.formuladock.core.preferences.FormulaDockPreferences
import com.formuladock.feature.formula.io.SingleFormulaExportSheet
import org.jetbrains.compose.resources.stringResource
import formuladock.feature.formula_list.generated.resources.*
import kotlinx.coroutines.launch
import kotlin.time.Clock

enum class FormulaListItemAction(
    val isTextButton: Boolean = false,
    val isDestructive: Boolean = false,
) {
    Run,
    Edit,
    Duplicate,
    Export,
    Delete(isTextButton = true, isDestructive = true)
}

val FormulaListItemAction.label: String
    @Composable
    get() = when (this) {
        FormulaListItemAction.Run -> stringResource(Res.string.action_run)
        FormulaListItemAction.Edit -> stringResource(Res.string.action_edit)
        FormulaListItemAction.Duplicate -> stringResource(Res.string.action_duplicate)
        FormulaListItemAction.Export -> stringResource(Res.string.action_export)
        FormulaListItemAction.Delete -> stringResource(Res.string.action_delete)
    }

data class FormulaListUiState(
    val query: String = "",
    val items: List<FormulaListItemUiModel> = emptyList(),
    val error: String? = null,
    val formulasEmpty: Boolean = false,
    val fabMenuExpanded: Boolean = false,
)

data class FormulaListItemUiModel(
    val id: String,
    val title: String,
    val description: String?,
    val isDefault: Boolean,
    val isBuiltin: Boolean,
    val allowedActions: List<FormulaListItemAction>,
)

fun FormulaDefinition.toUiModel(isDefault: Boolean): FormulaListItemUiModel {
    val actions = mutableListOf<FormulaListItemAction>()
    actions.add(FormulaListItemAction.Run)
    if (!isBuiltin) {
        actions.add(FormulaListItemAction.Edit)
    }
    actions.add(FormulaListItemAction.Duplicate)
    actions.add(FormulaListItemAction.Export)
    if (!isBuiltin) {
        actions.add(FormulaListItemAction.Delete)
    }
    return FormulaListItemUiModel(
        id = id,
        title = title,
        description = description,
        isDefault = isDefault,
        isBuiltin = isBuiltin,
        allowedActions = actions,
    )
}

@Composable
fun FormulaListContainer(
    repository: FormulaRepository,
    modifier: Modifier = Modifier,
    onRunFormula: (FormulaDefinition) -> Unit,
    onCreateFormula: () -> Unit,
    onEditFormula: (String) -> Unit,
    onDuplicateFormula: (String) -> Unit,
    onHistoryClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    onImportExportClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var formulas by remember { mutableStateOf<List<FormulaDefinition>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var fabMenuExpanded by remember { mutableStateOf(false) }
    var exportFormula by remember { mutableStateOf<FormulaDefinition?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val preferences = remember { FormulaDockPreferences() }
    val defaultFormulaId by preferences.defaultFormulaId.collectAsState(initial = null)

    fun reload() {
        scope.launch {
            runCatching {
                GetFormulaListUseCase(repository)().sortedWith(
                    compareBy<FormulaDefinition> { it.sortOrder }
                        .thenByDescending { it.updatedAt }
                )
            }.onSuccess {
                formulas = it
                error = null
            }.onFailure {
                error = it.message ?: "加载公式失败"
            }
        }
    }

    LaunchedEffect(repository) { reload() }

    LaunchedEffect(formulas, defaultFormulaId) {
        val selectedDefaultFormulaId = defaultFormulaId
        if (selectedDefaultFormulaId != null && formulas.none { it.id == selectedDefaultFormulaId }) {
            preferences.setDefaultFormulaId(null)
        }
    }

    val uiModels = remember(formulas, searchQuery, defaultFormulaId) {
        val filtered = if (searchQuery.isBlank()) {
            formulas
        } else {
            formulas.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        (it.description?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
        filtered.map { it.toUiModel(it.id == defaultFormulaId) }
    }

    val state = FormulaListUiState(
        query = searchQuery,
        items = uiModels,
        error = error,
        formulasEmpty = formulas.isEmpty(),
        fabMenuExpanded = fabMenuExpanded,
    )

    // TODO: LocalClipboardManager
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val clipboardService = remember(clipboardManager) {
        com.formuladock.feature.formula.io.FormulaClipboardService { text ->
            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(text))
            true
        }
    }

    exportFormula?.let { formula ->
        SingleFormulaExportSheet(
            formula = formula,
            clipboard = clipboardService,
            shareService = com.formuladock.feature.formula.io.LocalFormulaShareService.current,
            nowProvider = { Clock.System.now().toEpochMilliseconds() },
            onDismiss = { exportFormula = null },
            onMessage = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
        )
    }

    FormulaListScreen(
        state = state,
        onSearchQueryChange = { searchQuery = it },
        onFabMenuToggle = { fabMenuExpanded = it },
        onDefaultChange = { id, checked ->
            scope.launch {
                preferences.setDefaultFormulaId(if (checked) id else null)
            }
        },
        onItemAction = { id, action ->
            when (action) {
                FormulaListItemAction.Run -> {
                    formulas.firstOrNull { it.id == id }?.let(onRunFormula)
                }
                FormulaListItemAction.Edit -> onEditFormula(id)
                FormulaListItemAction.Duplicate -> onDuplicateFormula(id)
                FormulaListItemAction.Export -> exportFormula = formulas.firstOrNull { it.id == id }
                FormulaListItemAction.Delete -> {
                    scope.launch {
                        if (id == defaultFormulaId) {
                            preferences.setDefaultFormulaId(null)
                        }
                        DeleteFormulaUseCase(repository)(id)
                        reload()
                    }
                }
            }
        },
        onHistoryClick = onHistoryClick,
        onPreferencesClick = onPreferencesClick,
        onCreateFormula = onCreateFormula,
        onImportExportClick = onImportExportClick,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun FormulaListScreen(
    state: FormulaListUiState,
    onSearchQueryChange: (String) -> Unit,
    onFabMenuToggle: (Boolean) -> Unit,
    onDefaultChange: (String, Boolean) -> Unit,
    onItemAction: (String, FormulaListItemAction) -> Unit,
    onHistoryClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    onCreateFormula: () -> Unit,
    onImportExportClick: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var moreMenuExpanded by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("FormulaDock", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onPreferencesClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Preferences",
                        )
                    }
                    IconButton(onClick = onHistoryClick) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                        )
                    }
                    IconButton(onClick = { moreMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(Res.string.action_more),
                        )
                    }
                    DropdownMenu(
                        expanded = moreMenuExpanded,
                        onDismissRequest = { moreMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.action_batch_manage)) },
                            onClick = {
                                moreMenuExpanded = false
                                onImportExportClick()
                            }
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButtonMenu(
                expanded = state.fabMenuExpanded,
                button = {
                    ToggleFloatingActionButton(
                        checked = state.fabMenuExpanded,
                        onCheckedChange = onFabMenuToggle,
                    ) {
                        Icon(
                            imageVector = if (checkedProgress > 0.5f) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = stringResource(Res.string.action_toggle_menu),
                        )
                    }
                },
            ) {
                FloatingActionButtonMenuItem(
                    onClick = {
                        onFabMenuToggle(false)
                        onCreateFormula()
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(Res.string.action_create_formula)) },
                )
                FloatingActionButtonMenuItem(
                    onClick = {
                        onFabMenuToggle(false)
                        onImportExportClick()
                    },
                    icon = { Icon(Icons.Default.Build, contentDescription = null) },
                    text = { Text(stringResource(Res.string.action_data_management)) },
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            state.error?.let {
                item {
                    val displayError = if (it == "加载公式失败") stringResource(Res.string.formula_list_load_failed) else it
                    Text(displayError, color = MaterialTheme.colorScheme.error)
                }
            }

            item {
                FdSearchBar(
                    query = state.query,
                    onQueryChange = onSearchQueryChange,
                    placeholder = stringResource(Res.string.search_placeholder),
                    clearContentDescription = stringResource(Res.string.clear_search),
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }

            if (state.formulasEmpty) {
                item {
                    Text(stringResource(Res.string.formula_list_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            } else if (state.items.isEmpty()) {
                item {
                    Text(stringResource(Res.string.formula_list_no_matches), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(state.items, key = { it.id }) { itemUiModel ->
                    FormulaListItem(
                        item = itemUiModel,
                        onDefaultChange = { onDefaultChange(itemUiModel.id, it) },
                        onAction = { onItemAction(itemUiModel.id, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FormulaListItem(
    item: FormulaListItemUiModel,
    onDefaultChange: (Boolean) -> Unit,
    onAction: (FormulaListItemAction) -> Unit,
) {
    val highlightBorder = if (item.isDefault) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
    } else {
        BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onAction(FormulaListItemAction.Run) }),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isDefault) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            }
        ),
        shape = MaterialTheme.shapes.small,
        border = highlightBorder,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TypeBadge(isBuiltin = item.isBuiltin)

                    Row(
                        modifier = Modifier
                            .clickable { onDefaultChange(!item.isDefault) }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = item.isDefault,
                            onCheckedChange = onDefaultChange,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(Res.string.badge_default),
                            color = if (item.isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (item.isDefault) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            item.description?.let {
                if (it.isNotBlank()) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item.allowedActions.forEach { action ->
                    if (action.isTextButton) {
                        TextButton(
                            onClick = { onAction(action) },
                            modifier = Modifier.height(30.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) {
                            Text(
                                text = action.label,
                                color = if (action.isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onAction(action) },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(30.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        ) {
                            Text(action.label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeBadge(isBuiltin: Boolean) {
    val color = if (isBuiltin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.3f)),
    ) {
        val textRes = if (isBuiltin) Res.string.badge_builtin else Res.string.badge_custom
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
