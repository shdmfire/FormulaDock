package com.formuladock.feature.preferences

import androidx.compose.runtime.Composable
import com.formuladock.core.preferences.FormulaDockPreferences

/** JVM renders the setting; other targets intentionally render nothing. */
@Composable
internal expect fun DesktopHotkeyPreference(preferences: FormulaDockPreferences)
