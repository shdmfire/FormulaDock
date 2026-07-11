@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.formuladock.feature.formula.io

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.formuladock.core.designsystem.theme.FdAlphas
import com.formuladock.core.designsystem.theme.FdDimensions
import com.formuladock.core.formula.io.FormulaJsonCodec
import com.formuladock.core.model.formula.model.FormulaDefinition
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import formuladock.feature.formula_io.generated.resources.*

@Composable
fun SingleFormulaExportSheet(
    formula: FormulaDefinition,
    clipboard: FormulaClipboardService,
    shareService: FormulaShareService,
    nowProvider: () -> Long,
    onDismiss: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fileName = formulaJsonFileName(formula.title)
    val suggestedName = fileName.substringBeforeLast(".")
    val extension = fileName.substringAfterLast(".", "json")

    val msgActionFailed = stringResource(Res.string.action_failed)
    val msgSaveCancelled = stringResource(Res.string.save_cancelled)
    val msgSaveSuccess = stringResource(Res.string.save_success)
    val msgSaveFailed = stringResource(Res.string.save_failed)
    val msgCopiedJson = stringResource(Res.string.message_copied_json)
    val msgCopyFailed = stringResource(Res.string.message_copy_failed)
    val msgOpenedShare = stringResource(Res.string.message_opened_share)
    val msgShareFailed = stringResource(Res.string.message_share_failed)

    fun json() = FormulaJsonCodec.encode(listOf(formula), now = nowProvider(), includeBuiltins = true)
    
    fun runAction(action: suspend () -> String) {
        scope.launch {
            val message = runCatching { action() }.getOrElse { throwable ->
                throwable.printStackTrace()
                msgActionFailed
            }
            sheetState.hide()
            onDismiss()
            onMessage(message)
        }
    }

    val fileSaverLauncher = rememberFileSaverLauncher(
        dialogSettings = FileKitDialogSettings.createDefault(),
    ) { file ->
        if (file == null) {
            scope.launch { sheetState.hide() }
            onDismiss()
            onMessage(msgSaveCancelled)
            return@rememberFileSaverLauncher
        }

        scope.launch {
            val message = runCatching {
                file.writeString(json())
                msgSaveSuccess
            }.getOrElse { throwable ->
                throwable.printStackTrace()
                msgSaveFailed
            }
            sheetState.hide()
            onDismiss()
            onMessage(message)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FdDimensions.SpaceL, vertical = FdDimensions.SpaceS)
        ) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.sheet_title_export),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formula.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Type Badge
                Surface(
                    color = if (formula.isBuiltin) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(FdDimensions.CompactCorner)
                ) {
                    Text(
                        text = if (formula.isBuiltin) stringResource(Res.string.badge_builtin) else stringResource(Res.string.badge_custom),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (formula.isBuiltin) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(FdDimensions.SpaceM))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = FdAlphas.Divider),
                thickness = 0.5.dp
            )
            Spacer(modifier = Modifier.height(FdDimensions.SpaceM))

            // Action Items
            Column(
                verticalArrangement = Arrangement.spacedBy(FdDimensions.SpaceS),
                modifier = Modifier.padding(bottom = FdDimensions.SpaceL)
            ) {
                ActionRowItem(
                    title = stringResource(Res.string.action_copy_json_title),
                    subtitle = stringResource(Res.string.action_copy_json_desc),
                    icon = Icons.Default.ContentCopy,
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = {
                        runAction {
                            if (clipboard.writeText(json())) msgCopiedJson else msgCopyFailed
                        }
                    }
                )

                ActionRowItem(
                    title = stringResource(Res.string.action_save_json_title),
                    subtitle = stringResource(Res.string.action_save_json_desc),
                    icon = Icons.Default.Upload,
                    tint = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        fileSaverLauncher.launch(
                            suggestedName = suggestedName,
                            defaultExtension = extension,
                            allowedExtensions = setOf(extension),
                        )
                    }
                )

                ActionRowItem(
                    title = stringResource(Res.string.action_share_json_title),
                    subtitle = stringResource(Res.string.action_share_json_desc),
                    icon = Icons.Default.Share,
                    tint = MaterialTheme.colorScheme.tertiary,
                    onClick = {
                        runAction {
                            if (shareService.shareJson(fileName, json())) msgOpenedShare else msgShareFailed
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ActionRowItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(FdDimensions.CardCorner),
        border = BorderStroke(
            FdDimensions.Hairline,
            MaterialTheme.colorScheme.outline.copy(alpha = FdAlphas.Border)
        ),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = FdAlphas.Container)
    ) {
        Row(
            modifier = Modifier.padding(FdDimensions.SpaceM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = tint.copy(alpha = 0.1f),
                shape = RoundedCornerShape(FdDimensions.CompactCorner),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(FdDimensions.IconM)
                    )
                }
            }
            Spacer(modifier = Modifier.width(FdDimensions.SpaceM))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = FdAlphas.SecondaryContent)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = FdAlphas.MutedContent),
                modifier = Modifier.size(FdDimensions.IconM)
            )
        }
    }
}
