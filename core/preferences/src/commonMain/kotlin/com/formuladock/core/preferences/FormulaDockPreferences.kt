package com.formuladock.core.preferences

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.coroutines.getStringFlow
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import com.formuladock.core.i18n.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Application-level key/value preferences backed by Multiplatform Settings.
 *
 * Uses the no-arg Settings factory, which maps to SharedPreferences on Android,
 * NSUserDefaults on Apple platforms, and java.util.prefs.Preferences on JVM.
 */
@OptIn(ExperimentalSettingsApi::class)
class FormulaDockPreferences(
    private val settings: ObservableSettings = Settings() as ObservableSettings,
) {
    val themeMode: Flow<ThemeMode> = settings
        .getStringFlow(KEY_THEME_MODE, ThemeMode.System.storageValue)
        .map { ThemeMode.fromStorageValue(it) }

    val language: Flow<AppLanguage> = settings
        .getStringOrNullFlow(KEY_LANGUAGE)
        .map { AppLanguage.fromStorageValue(it) }

    val dynamicColorEnabled: Flow<Boolean> = settings.getBooleanFlow(
        key = KEY_DYNAMIC_COLOR_ENABLED,
        defaultValue = true,
    )

    val historyEnabled: Flow<Boolean> = settings.getBooleanFlow(
        key = KEY_HISTORY_ENABLED,
        defaultValue = true,
    )

    val quickCalculatorEnabled: Flow<Boolean> = settings.getBooleanFlow(
        key = KEY_QUICK_CALCULATOR_ENABLED,
        defaultValue = true,
    )

    val quickCalculatorNotificationEnabled: Flow<Boolean> = settings.getBooleanFlow(
        key = KEY_QUICK_CALCULATOR_NOTIFICATION_ENABLED,
        defaultValue = true,
    )

    val lastFormulaId: Flow<String?> = settings.getStringOrNullFlow(KEY_LAST_FORMULA_ID)

    val defaultFormulaId: Flow<String?> = settings.getStringOrNullFlow(KEY_DEFAULT_FORMULA_ID)

    val formulaSearchQuery: Flow<String> = settings.getStringFlow(
        key = KEY_FORMULA_SEARCH_QUERY,
        defaultValue = "",
    )

    val desktopHotkey: Flow<DesktopHotkeySetting> = settings
        .getStringFlow(KEY_DESKTOP_HOTKEY, DesktopHotkeySetting.Default.storageValue)
        .map(DesktopHotkeySetting::fromStorageValue)

    suspend fun setThemeMode(value: ThemeMode) {
        settings.putString(KEY_THEME_MODE, value.storageValue)
    }

    suspend fun setLanguage(value: AppLanguage) {
        putNullableString(KEY_LANGUAGE, value.languageTag)
    }

    suspend fun setDynamicColorEnabled(value: Boolean) {
        settings.putBoolean(KEY_DYNAMIC_COLOR_ENABLED, value)
    }

    suspend fun setHistoryEnabled(value: Boolean) {
        settings.putBoolean(KEY_HISTORY_ENABLED, value)
    }

    suspend fun setQuickCalculatorEnabled(value: Boolean) {
        settings.putBoolean(KEY_QUICK_CALCULATOR_ENABLED, value)
    }

    suspend fun setQuickCalculatorNotificationEnabled(value: Boolean) {
        settings.putBoolean(KEY_QUICK_CALCULATOR_NOTIFICATION_ENABLED, value)
    }

    suspend fun setLastFormulaId(value: String?) {
        putNullableString(KEY_LAST_FORMULA_ID, value)
    }

    suspend fun setDefaultFormulaId(value: String?) {
        putNullableString(KEY_DEFAULT_FORMULA_ID, value)
    }

    suspend fun setFormulaSearchQuery(value: String) {
        settings.putString(KEY_FORMULA_SEARCH_QUERY, value)
    }

    suspend fun setDesktopHotkey(value: DesktopHotkeySetting) {
        settings.putString(KEY_DESKTOP_HOTKEY, value.storageValue)
    }

    suspend fun clearLastFormula() {
        settings.remove(KEY_LAST_FORMULA_ID)
    }

    suspend fun reset() {
        settings.remove(KEY_THEME_MODE)
        settings.remove(KEY_LANGUAGE)
        settings.remove(KEY_DYNAMIC_COLOR_ENABLED)
        settings.remove(KEY_HISTORY_ENABLED)
        settings.remove(KEY_QUICK_CALCULATOR_ENABLED)
        settings.remove(KEY_QUICK_CALCULATOR_NOTIFICATION_ENABLED)
        settings.remove(KEY_LAST_FORMULA_ID)
        settings.remove(KEY_DEFAULT_FORMULA_ID)
        settings.remove(KEY_FORMULA_SEARCH_QUERY)
        settings.remove(KEY_DESKTOP_HOTKEY)
    }

    private fun putNullableString(key: String, value: String?) {
        if (value == null) {
            settings.remove(key)
        } else {
            settings.putString(key, value)
        }
    }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_LANGUAGE = "language"
        const val KEY_DYNAMIC_COLOR_ENABLED = "dynamic_color_enabled"
        const val KEY_HISTORY_ENABLED = "history_enabled"
        const val KEY_QUICK_CALCULATOR_ENABLED = "quick_calculator_enabled"
        const val KEY_QUICK_CALCULATOR_NOTIFICATION_ENABLED = "quick_calculator_notification_enabled"
        const val KEY_LAST_FORMULA_ID = "last_formula_id"
        const val KEY_DEFAULT_FORMULA_ID = "default_formula_id"
        const val KEY_FORMULA_SEARCH_QUERY = "formula_search_query"
        const val KEY_DESKTOP_HOTKEY = "desktop_hotkey"
    }
}

data class DesktopHotkeySetting(
    val keyCode: Int,
    val ctrl: Boolean,
    val alt: Boolean,
    val shift: Boolean,
    val meta: Boolean,
) {
    val hasModifier: Boolean get() = ctrl || alt || shift || meta

    val storageValue: String
        get() = listOf(keyCode, ctrl, alt, shift, meta).joinToString(",")

    companion object {
        // java.awt.event.KeyEvent.VK_F; kept platform-neutral for commonMain.
        const val DEFAULT_KEY_CODE = 70
        val Default = DesktopHotkeySetting(DEFAULT_KEY_CODE, ctrl = true, alt = true, shift = false, meta = false)

        fun fromStorageValue(value: String): DesktopHotkeySetting {
            val parts = value.split(',')
            if (parts.size != 5) return Default
            return DesktopHotkeySetting(
                keyCode = parts[0].toIntOrNull() ?: return Default,
                ctrl = parts[1].toBooleanStrictOrNull() ?: return Default,
                alt = parts[2].toBooleanStrictOrNull() ?: return Default,
                shift = parts[3].toBooleanStrictOrNull() ?: return Default,
                meta = parts[4].toBooleanStrictOrNull() ?: return Default,
            )
        }
    }
}

enum class ThemeMode(val storageValue: String) {
    System("system"),
    Light("light"),
    Dark("dark"),
    ;

    companion object {
        fun fromStorageValue(value: String): ThemeMode = entries.firstOrNull { it.storageValue == value } ?: System
    }
}
