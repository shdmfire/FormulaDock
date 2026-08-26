package com.formuladock.core.designsystem.component

import androidx.compose.runtime.staticCompositionLocalOf

/** True while the host application/window is in the foreground and interactive. */
val LocalAppInForeground = staticCompositionLocalOf { true }
