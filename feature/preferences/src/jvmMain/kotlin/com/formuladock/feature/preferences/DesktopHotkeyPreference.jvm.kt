package com.formuladock.feature.preferences

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import com.formuladock.core.designsystem.component.FdSettingsGroup
import com.formuladock.core.designsystem.theme.FdDimensions
import com.formuladock.core.preferences.DesktopHotkeySetting
import com.formuladock.core.preferences.FormulaDockPreferences
import java.awt.event.KeyEvent as AwtKeyEvent
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import formuladock.feature.preferences.generated.resources.*

@Composable
internal actual fun DesktopHotkeyPreference(preferences: FormulaDockPreferences) {
    val hotkey by preferences.desktopHotkey.collectAsState(initial = DesktopHotkeySetting.Default)
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var isCapturing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var candidateHotkey by remember { mutableStateOf<DesktopHotkeySetting?>(null) }

    val errorModifierRequired = stringResource(Res.string.hotkey_error_modifier_required)

    LaunchedEffect(isCapturing) {
        if (isCapturing) focusRequester.requestFocus()
    }

    Column(verticalArrangement = Arrangement.spacedBy(FdDimensions.SpaceXs)) {
        Text(
            text = stringResource(Res.string.hotkey_group_title),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = FdDimensions.SpaceXs, vertical = FdDimensions.SpaceXs),
        )
        FdSettingsGroup {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { event ->
                        if (!isCapturing) return@onPreviewKeyEvent false
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent true

                        val keyCode = event.key.nativeKeyCode
                        if (keyCode in modifierKeyCodes) return@onPreviewKeyEvent true
                        if (keyCode == AwtKeyEvent.VK_ESCAPE) {
                            isCapturing = false
                            errorMessage = null
                            return@onPreviewKeyEvent true
                        }

                        val candidate = DesktopHotkeySetting(
                            keyCode = keyCode,
                            ctrl = event.isCtrlPressed,
                            alt = event.isAltPressed,
                            shift = event.isShiftPressed,
                            meta = event.isMetaPressed,
                        )
                        if (keyCode == AwtKeyEvent.VK_UNDEFINED || !candidate.hasModifier) {
                            errorMessage = errorModifierRequired
                            return@onPreviewKeyEvent true
                        }

                        candidateHotkey = candidate
                        isCapturing = false
                        errorMessage = null
                        true
                    }
                    .focusable()
                    .padding(FdDimensions.SpaceM),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(FdDimensions.SpaceXs),
                ) {
                    Text(stringResource(Res.string.hotkey_convenient_calc_panel), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (isCapturing) stringResource(Res.string.hotkey_capturing_hint) else hotkey.displayText(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCapturing) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    errorMessage?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                OutlinedButton(onClick = {
                    isCapturing = !isCapturing
                    errorMessage = null
                }) {
                    Text(if (isCapturing) stringResource(Res.string.action_cancel) else stringResource(Res.string.action_modify))
                }
            }
        }
    }

    candidateHotkey?.let { candidate ->
        AlertDialog(
            onDismissRequest = { candidateHotkey = null },
            title = { Text(stringResource(Res.string.hotkey_confirm_dialog_title)) },
            text = {
                Text(stringResource(Res.string.hotkey_confirm_dialog_message, candidate.displayText()))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            preferences.setDesktopHotkey(candidate)
                        }
                        candidateHotkey = null
                    }
                ) {
                    Text(stringResource(Res.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        candidateHotkey = null
                    }
                ) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        )
    }
}

private val modifierKeyCodes = setOf(
    AwtKeyEvent.VK_CONTROL,
    AwtKeyEvent.VK_ALT,
    AwtKeyEvent.VK_ALT_GRAPH,
    AwtKeyEvent.VK_SHIFT,
    AwtKeyEvent.VK_META,
    AwtKeyEvent.VK_WINDOWS,
)

private fun DesktopHotkeySetting.displayText(): String = buildList {
    if (ctrl) add("Ctrl")
    if (alt) add("Alt")
    if (shift) add("Shift")
    if (meta) add(if (System.getProperty("os.name").contains("mac", ignoreCase = true)) "Command" else "Meta")
    add(AwtKeyEvent.getKeyText(keyCode))
}.joinToString(" + ")
