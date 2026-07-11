@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.formuladock.feature.formula.io

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.formuladock.core.designsystem.component.FdListItemCard
import com.formuladock.core.designsystem.component.FdSearchBar
import com.formuladock.core.designsystem.component.FdSectionCard
import com.formuladock.core.designsystem.theme.FdAlphas
import com.formuladock.core.designsystem.theme.FdDimensions
import org.jetbrains.compose.resources.stringResource
import formuladock.feature.formula_io.generated.resources.*

@Composable
fun FormulaIoScreen(
    state: FormulaIoUiState,
    onTabClick: (FormulaIoTab) -> Unit,
    onImportFileClick: () -> Unit,
    onImportClipboardClick: () -> Unit,
    onToggleImportItem: (Int) -> Unit,
    onConfirmImportClick: () -> Unit,
    onResetImportClick: () -> Unit,
    onExportQueryChange: (String) -> Unit,
    onToggleExportItem: (String) -> Unit,
    onSelectAllExport: (Boolean) -> Unit,
    onSaveSelectedClick: () -> Unit,
    onCopySelectedClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(Res.string.io_title), 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = state.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[state.selectedTab.ordinal]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = state.selectedTab == FormulaIoTab.Import,
                    onClick = { onTabClick(FormulaIoTab.Import) },
                    text = { Text(stringResource(Res.string.io_tab_import), fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = state.selectedTab == FormulaIoTab.Export,
                    onClick = { onTabClick(FormulaIoTab.Export) },
                    text = { Text(stringResource(Res.string.io_tab_export), fontWeight = FontWeight.SemiBold) }
                )
            }

            state.message?.let {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = FdDimensions.SpaceL, vertical = FdDimensions.SpaceS),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(FdDimensions.IconS)
                        )
                        Spacer(modifier = Modifier.width(FdDimensions.SpaceS))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            when (state.selectedTab) {
                FormulaIoTab.Import -> FormulaImportTab(
                    state = state.importState,
                    onImportFileClick = onImportFileClick,
                    onImportClipboardClick = onImportClipboardClick,
                    onToggleItem = onToggleImportItem,
                    onConfirmClick = onConfirmImportClick,
                    onResetClick = onResetImportClick
                )
                FormulaIoTab.Export -> FormulaExportTab(
                    state = state.exportState,
                    onQueryChange = onExportQueryChange,
                    onToggleItem = onToggleExportItem,
                    onSelectAll = onSelectAllExport,
                    onSaveClick = onSaveSelectedClick,
                    onCopyClick = onCopySelectedClick
                )
            }
        }
    }
}

@Composable
private fun FormulaImportTab(
    state: ImportPanelState,
    onImportFileClick: () -> Unit,
    onImportClipboardClick: () -> Unit,
    onToggleItem: (Int) -> Unit,
    onConfirmClick: () -> Unit,
    onResetClick: () -> Unit,
) {
    when (state) {
        ImportPanelState.Idle -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(FdDimensions.SpaceL),
                verticalArrangement = Arrangement.spacedBy(FdDimensions.SpaceM)
            ) {
                FdSectionCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = FdDimensions.SpaceXs)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(FdDimensions.IconM)
                        )
                        Spacer(modifier = Modifier.width(FdDimensions.SpaceS))
                        Text(
                            text = stringResource(Res.string.import_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(FdDimensions.SpaceXs))

                FdListItemCard(onClick = onImportFileClick) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(FdDimensions.SpaceL))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(Res.string.import_file_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(stringResource(Res.string.import_file_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = FdAlphas.SecondaryContent))
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = FdAlphas.SecondaryContent)
                    )
                }

                FdListItemCard(onClick = onImportClipboardClick) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(FdDimensions.SpaceL))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(Res.string.import_clipboard_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(stringResource(Res.string.import_clipboard_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = FdAlphas.SecondaryContent))
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = FdAlphas.SecondaryContent)
                    )
                }
            }
        }

        ImportPanelState.Parsing -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(FdDimensions.SpaceM))
                    Text(stringResource(Res.string.import_parsing), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        is ImportPanelState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(FdDimensions.SpaceL),
                verticalArrangement = Arrangement.spacedBy(FdDimensions.SpaceM)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = stringResource(Res.string.import_failed_title),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(FdDimensions.SpaceS))
                    Text(stringResource(Res.string.import_failed_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }

                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(FdDimensions.CompactCorner),
                    border = androidx.compose.foundation.BorderStroke(FdDimensions.Hairline, MaterialTheme.colorScheme.error.copy(alpha = FdAlphas.Border)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(FdDimensions.SpaceM)) {
                        state.errors.forEach { errorMsg ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text("• ", color = MaterialTheme.colorScheme.error)
                                Text(errorMsg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onResetClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(Res.string.action_retry_import))
                }
            }
        }

        is ImportPanelState.Success -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(FdDimensions.SpaceL)
                ) {
                    Surface(
                        color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.size(64.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2E7D32).copy(alpha = 0.4f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(FdDimensions.SpaceL))
                    Text(stringResource(Res.string.import_success_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(FdDimensions.SpaceS))
                    Text(stringResource(Res.string.import_success_message, state.importedCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onResetClick) {
                        Text(stringResource(Res.string.action_continue_import))
                    }
                }
            }
        }
        is ImportPanelState.Preview -> {
            val selectedCount = state.items.count { it.isSelected }
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Summary Card
                FdSectionCard(modifier = Modifier.padding(FdDimensions.SpaceL)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(Res.string.preview_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = stringResource(Res.string.preview_summary, state.items.size, selectedCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selectedCount > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(FdDimensions.CompactCorner)
                            ) {
                                Text(
                                    text = "Ready",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Lazy List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = FdDimensions.SpaceL, vertical = FdDimensions.SpaceXs),
                    verticalArrangement = Arrangement.spacedBy(FdDimensions.SpaceS)
                ) {
                    items(state.items, key = { it.index }) { item ->
                        val borderAlpha = if (item.isSelected) 0.4f else FdAlphas.Border
                        val borderStroke = androidx.compose.foundation.BorderStroke(
                            FdDimensions.Hairline,
                            if (item.isSelected) MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha)
                            else MaterialTheme.colorScheme.outline.copy(alpha = borderAlpha)
                        )
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleItem(item.index) },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(FdDimensions.CardCorner),
                            border = borderStroke,
                            color = if (item.isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(FdDimensions.SpaceM),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isSelected,
                                    onCheckedChange = { onToggleItem(item.index) }
                                )
                                Spacer(modifier = Modifier.width(FdDimensions.SpaceS))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.description ?: stringResource(Res.string.formula_no_description),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = FdAlphas.SecondaryContent),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(FdDimensions.SpaceXs))
                                    Text(
                                        text = stringResource(Res.string.formula_io_counts, item.inputCount, item.outputCount),
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = FdAlphas.SecondaryContent)
                                    )
                                }
                            }
                        }
                    }
                }

                // Footer actions
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(FdDimensions.SpaceL)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FdDimensions.SpaceM),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onResetClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(Res.string.action_cancel))
                        }
                        Button(
                            onClick = onConfirmClick,
                            enabled = selectedCount > 0,
                            modifier = Modifier.weight(2f)
                        ) {
                            Text(stringResource(Res.string.action_confirm_import_selected, selectedCount))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormulaExportTab(
    state: ExportPanelState,
    onQueryChange: (String) -> Unit,
    onToggleItem: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
    onCopyClick: () -> Unit,
) {
    when (state) {
        ExportPanelState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        ExportPanelState.Empty -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(FdDimensions.SpaceL)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = FdAlphas.SecondaryContent),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(FdDimensions.SpaceM))
                    Text(
                        text = stringResource(Res.string.export_empty_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(Res.string.export_empty_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = FdAlphas.SecondaryContent)
                    )
                }
            }
        }

        is ExportPanelState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }

        is ExportPanelState.Ready -> {
            val visible = state.items.filter {
                it.title.contains(state.query, ignoreCase = true) ||
                        (it.description?.contains(state.query, ignoreCase = true) == true)
            }
            val selectedCount = state.items.count { it.isSelected }

            Column(modifier = Modifier.fillMaxSize()) {
                // Search Bar Section
                Column(modifier = Modifier.padding(horizontal = FdDimensions.SpaceL, vertical = FdDimensions.SpaceS)) {
                    FdSearchBar(
                        query = state.query,
                        onQueryChange = onQueryChange,
                        placeholder = stringResource(Res.string.export_search_placeholder),
                        clearContentDescription = stringResource(Res.string.action_clear)
                    )

                    Spacer(modifier = Modifier.height(FdDimensions.SpaceS))

                    // Select Actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(FdDimensions.SpaceS),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(FdDimensions.CompactCorner),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            modifier = Modifier
                                .clickable { onSelectAll(true) }
                        ) {
                            Text(
                                text = stringResource(Res.string.action_select_all),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = FdDimensions.SpaceM, vertical = FdDimensions.SpaceXs),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(FdDimensions.CompactCorner),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier
                                .clickable { onSelectAll(false) }
                        ) {
                            Text(
                                text = stringResource(Res.string.action_deselect_all),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = FdDimensions.SpaceM, vertical = FdDimensions.SpaceXs)
                            )
                        }
                    }
                }

                // Lazy Column List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = FdDimensions.SpaceL, vertical = FdDimensions.SpaceXs),
                    verticalArrangement = Arrangement.spacedBy(FdDimensions.SpaceS)
                ) {
                    if (visible.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(Res.string.export_no_matches),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    items(visible, key = { it.id }) { item ->
                        val borderAlpha = if (item.isSelected) 0.4f else FdAlphas.Border
                        val borderStroke = androidx.compose.foundation.BorderStroke(
                            FdDimensions.Hairline,
                            if (item.isSelected) MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha)
                            else MaterialTheme.colorScheme.outline.copy(alpha = borderAlpha)
                        )
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleItem(item.id) },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(FdDimensions.CardCorner),
                            border = borderStroke,
                            color = if (item.isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(FdDimensions.SpaceM),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isSelected,
                                    onCheckedChange = { onToggleItem(item.id) }
                                )
                                Spacer(modifier = Modifier.width(FdDimensions.SpaceS))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    item.description?.let { desc ->
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = FdAlphas.SecondaryContent),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(FdDimensions.SpaceL)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FdDimensions.SpaceM),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(Res.string.export_selected_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = stringResource(Res.string.export_selected_count, selectedCount),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = if (selectedCount > 0) MaterialTheme.colorScheme.primary else Color.Unspecified
                            )
                        }

                        OutlinedButton(
                            onClick = onCopyClick,
                            enabled = selectedCount > 0 && !state.isExporting,
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(stringResource(Res.string.action_copy_json))
                        }

                        Button(
                            onClick = onSaveClick,
                            enabled = selectedCount > 0 && !state.isExporting,
                            modifier = Modifier.weight(1.8f)
                        ) {
                            Text(stringResource(Res.string.action_export_file))
                        }
                    }
                }
            }
        }
    }
}
