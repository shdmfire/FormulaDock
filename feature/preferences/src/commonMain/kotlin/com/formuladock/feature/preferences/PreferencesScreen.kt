package com.formuladock.feature.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.formuladock.core.designsystem.component.FdSettingsGroup
import com.formuladock.core.designsystem.component.FdSettingsOption
import com.formuladock.core.designsystem.theme.FdAlphas
import com.formuladock.core.designsystem.theme.FdDimensions
import com.formuladock.core.preferences.FormulaDockPreferences
import com.formuladock.core.preferences.ThemeMode
import com.formuladock.core.i18n.AppLanguage
import org.jetbrains.compose.resources.stringResource
import formuladock.feature.preferences.generated.resources.*
import kotlinx.coroutines.launch

@Composable
fun PreferencesContainer(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preferences = remember { FormulaDockPreferences() }
    val themeMode by preferences.themeMode.collectAsState(initial = ThemeMode.System)
    val language by preferences.language.collectAsState(initial = AppLanguage.System)
    val quickCalculatorEnabled by preferences.quickCalculatorEnabled.collectAsState(initial = true)
    val quickCalculatorNotificationEnabled by preferences.quickCalculatorNotificationEnabled.collectAsState(initial = true)
    val scope = rememberCoroutineScope()

    PreferencesScreen(
        themeMode = themeMode,
        onThemeModeChange = { scope.launch { preferences.setThemeMode(it) } },
        language = language,
        onLanguageChange = { scope.launch { preferences.setLanguage(it) } },
        quickCalculatorEnabled = quickCalculatorEnabled,
        onQuickCalculatorEnabledChange = { scope.launch { preferences.setQuickCalculatorEnabled(it) } },
        quickCalculatorNotificationEnabled = quickCalculatorNotificationEnabled,
        onQuickCalculatorNotificationEnabledChange = { scope.launch { preferences.setQuickCalculatorNotificationEnabled(it) } },
        onBack = onBack,
        modifier = modifier,
        preferences = preferences,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    quickCalculatorEnabled: Boolean,
    onQuickCalculatorEnabledChange: (Boolean) -> Unit,
    quickCalculatorNotificationEnabled: Boolean,
    onQuickCalculatorNotificationEnabledChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    preferences: FormulaDockPreferences,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.preferences_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.preferences_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(FdDimensions.SpaceS),
            verticalArrangement = Arrangement.spacedBy(FdDimensions.SpaceS),
        ) {
            Text(
                text = stringResource(Res.string.preferences_theme_mode),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = FdDimensions.SpaceXs, vertical = FdDimensions.SpaceXs)
            )
            FdSettingsGroup {
                for ((index, mode) in ThemeMode.entries.withIndex()) {
                    FdSettingsOption(
                        title = mode.title,
                        selected = themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                    )
                    if (index != ThemeMode.entries.lastIndex) ThinDivider()
                }
            }

            Text(
                text = stringResource(Res.string.preferences_language),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = FdDimensions.SpaceXs, vertical = FdDimensions.SpaceXs)
            )
            FdSettingsGroup {
                FdSettingsOption(
                    title = stringResource(Res.string.language_system),
                    selected = language == AppLanguage.System,
                    onClick = { onLanguageChange(AppLanguage.System) },
                )
                ThinDivider()
                FdSettingsOption(
                    title = "English",
                    selected = language == AppLanguage.English,
                    onClick = { onLanguageChange(AppLanguage.English) },
                )
                ThinDivider()
                FdSettingsOption(
                    title = "中文",
                    selected = language == AppLanguage.Chinese,
                    onClick = { onLanguageChange(AppLanguage.Chinese) },
                )
            }

            if (platform() == "Android") {
                Text(
                    text = stringResource(Res.string.preferences_quick_calculator),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = FdDimensions.SpaceXs, vertical = FdDimensions.SpaceXs)
                )
                FdSettingsGroup {
                    FdSettingsSwitch(
                        title = stringResource(Res.string.preferences_quick_calculator_enable),
                        checked = quickCalculatorEnabled,
                        onCheckedChange = onQuickCalculatorEnabledChange
                    )
                    ThinDivider()
                    FdSettingsCheckbox(
                        title = stringResource(Res.string.preferences_quick_calculator_notification),
                        checked = quickCalculatorNotificationEnabled && quickCalculatorEnabled,
                        onCheckedChange = onQuickCalculatorNotificationEnabledChange,
                        enabled = quickCalculatorEnabled,
                        modifier = Modifier.padding(start = FdDimensions.SpaceM)
                    )
                }
            }

            DesktopHotkeyPreference(preferences)
        }
    }
}

private val ThemeMode.title: String
    @Composable
    get() = when (this) {
        ThemeMode.System -> stringResource(Res.string.theme_mode_system)
        ThemeMode.Light -> stringResource(Res.string.theme_mode_light)
        ThemeMode.Dark -> stringResource(Res.string.theme_mode_dark)
    }

@Composable
private fun ThinDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = FdAlphas.Container),
        thickness = FdDimensions.Hairline,
    )
}

@Composable
private fun FdSettingsSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = { onCheckedChange(!checked) })
            .padding(vertical = FdDimensions.SpaceXs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun FdSettingsCheckbox(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = { onCheckedChange(!checked) })
            .padding(vertical = FdDimensions.SpaceXs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier = Modifier.weight(1f)
        )
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
