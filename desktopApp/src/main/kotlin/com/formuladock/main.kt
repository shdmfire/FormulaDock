package com.formuladock

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.formuladock.core.data.formula.SqlDelightFormulaRepository
import com.formuladock.core.data.history.SqlDelightCalculationHistoryRepository
import com.formuladock.core.database.DriverFactory
import com.formuladock.core.database.createDatabase
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import formuladock.desktopapp.generated.resources.*
import androidx.compose.ui.unit.dp
import com.kdroid.composetray.tray.api.Tray
import com.formuladock.feature.formula.panel.FormulaCalculatorPanel

fun main() = application {
    var isMainWindowVisible by remember { mutableStateOf(false) }
    var isCalculatorPanelVisible by remember { mutableStateOf(false) }
    val appIcon = painterResource(Res.drawable.icon)
    val db = remember { createDatabase(DriverFactory()) }
    val repository = remember { SqlDelightFormulaRepository(db) }
    val historyRepository = remember { SqlDelightCalculationHistoryRepository(db) }

    val hotkeyManager = remember {
        DesktopHotkeyManager(
            onToggle = { isCalculatorPanelVisible = !isCalculatorPanelVisible }
        )
    }

    val calculatorWindowState = rememberWindowState(
        size = DpSize(
            width = 360.dp,
            height = 260.dp
        ),
        position = WindowPosition.Aligned(Alignment.BottomEnd)
    )

    LaunchedEffect(isCalculatorPanelVisible) {
        if (isCalculatorPanelVisible) {
            calculatorWindowState.size =
                DpSize(360.dp, Dp.Unspecified)
        }
    }

    fun exitFormulaDock() {
        hotkeyManager.unregister()
        exitApplication()
    }

    DisposableEffect(hotkeyManager) {
        hotkeyManager.register()
        onDispose { hotkeyManager.unregister() }
    }

    val trayOpenMain = stringResource(Res.string.tray_open_main)
    val trayOpenQuickCalc = stringResource(Res.string.tray_open_quick_calc)
    val trayExit = stringResource(Res.string.tray_exit)

    Tray(
        icon = Res.drawable.icon,
        tooltip = stringResource(Res.string.app_name),
        primaryAction = { isMainWindowVisible = true }
    ) {
        Item(label = trayOpenMain) { isMainWindowVisible = true }
        Item(label = trayOpenQuickCalc) { isCalculatorPanelVisible = true }
        Divider()
        Item(label = trayExit) { exitFormulaDock() }
    }

    Window(
        visible = isMainWindowVisible,
        onCloseRequest = { isMainWindowVisible = false },
        title = stringResource(Res.string.app_name),
        icon = appIcon,
        state = rememberWindowState(
            width = 520.dp,
            height = 720.dp,
            position = WindowPosition.Aligned(Alignment.BottomEnd)
        ),
        alwaysOnTop = true,
        resizable = true
    ) {
        App(repository = repository, historyRepository = historyRepository)
    }

    Window(
        visible = isCalculatorPanelVisible,
        onCloseRequest = { isCalculatorPanelVisible = false },
        title = stringResource(Res.string.window_title_quick_calc),
        icon = appIcon,
        state = calculatorWindowState,
        alwaysOnTop = true,
        resizable = false
    ) {
        FormulaCalculatorPanel(
            repository = repository,
            historyRepository = historyRepository,
            onClose = {
                isCalculatorPanelVisible = false
            },
            onContentReady = {
                calculatorWindowState.size = DpSize(
                    width = 360.dp,
                    height = Dp.Unspecified
                )
            }
        )
    }
}
