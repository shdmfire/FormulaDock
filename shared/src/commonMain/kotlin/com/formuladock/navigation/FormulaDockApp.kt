package com.formuladock.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.data.history.CalculationHistoryRepository
import com.formuladock.core.designsystem.theme.FormulaDockTheme
import com.formuladock.core.preferences.FormulaDockPreferences
import com.formuladock.core.i18n.AppLanguage
import com.formuladock.core.i18n.AppLocaleProvider
import androidx.compose.runtime.collectAsState
import com.formuladock.core.domain.formula.DeleteFormulaUseCase
import com.formuladock.core.domain.formula.SeedBuiltinFormulasUseCase
import com.formuladock.feature.formula.editor.FormulaEditorNavKey
import com.formuladock.feature.formula.editor.formulaEditorEntry
import com.formuladock.feature.formula.history.FormulaHistoryRoute
import com.formuladock.feature.formula.history.formulaHistoryEntry
import com.formuladock.feature.formula.io.FormulaIoNavKey
import com.formuladock.feature.formula.io.FormulaShareService
import com.formuladock.feature.formula.io.LocalFormulaShareService
import com.formuladock.feature.formula.io.NoOpFormulaShareService
import com.formuladock.feature.formula.io.formulaIoEntry
import com.formuladock.feature.formula.list.FormulaListRoute
import com.formuladock.feature.formula.list.formulaListEntry
import com.formuladock.feature.formula.run.FormulaRunRoute
import com.formuladock.feature.formula.run.formulaRunEntry
import com.formuladock.feature.preferences.PreferencesRoute
import com.formuladock.feature.preferences.preferencesEntry
import kotlinx.coroutines.launch

@Composable
fun FormulaDockApp(
    repository: FormulaRepository,
    historyRepository: CalculationHistoryRepository,
    modifier: Modifier = Modifier,
    shareService: FormulaShareService = NoOpFormulaShareService,
) {
    val scope = rememberCoroutineScope()
    val navConfig = remember {
        androidx.savedstate.serialization.SavedStateConfiguration {
            serializersModule = kotlinx.serialization.modules.SerializersModule {
                polymorphic(androidx.navigation3.runtime.NavKey::class) {
                    subclass(FormulaListRoute.serializer())
                    subclass(FormulaRunRoute.serializer())
                    subclass(FormulaHistoryRoute.serializer())
                    subclass(FormulaIoNavKey.serializer())
                    subclass(FormulaEditorNavKey.serializer())
                    subclass(PreferencesRoute.serializer())
                }
            }
        }
    }
    val backStack = rememberNavBackStack(navConfig, FormulaListRoute)
    var refreshToken by remember { mutableStateOf(0) }
    var isSeeded by remember { mutableStateOf(false) }
    val nowProvider = remember { NowProvider() }

    val seedBuiltinFormulas = remember(repository) {
        SeedBuiltinFormulasUseCase(
            repository = repository,
            nowProvider = { nowProvider.now() },
        )
    }
    val deleteFormula = remember(repository) { DeleteFormulaUseCase(repository) }

    fun popToList() {
        while (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    LaunchedEffect(seedBuiltinFormulas) {
        seedBuiltinFormulas()
        isSeeded = true
    }

    val preferences = remember { FormulaDockPreferences() }
    val language by preferences.language.collectAsState(initial = AppLanguage.System)

    AppLocaleProvider(language) {
        FormulaDockTheme {
            CompositionLocalProvider(LocalFormulaShareService provides shareService) {
            Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                if (!isSeeded) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Loading formulas...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                NavDisplay(
                    backStack = backStack,
                    entryProvider = entryProvider {
                        formulaListEntry(
                            repository = repository,
                            onRunFormula = { formulaId -> backStack.add(FormulaRunRoute(formulaId)) },
                            onCreateFormula = { backStack.add(FormulaEditorNavKey.create()) },
                            onEditFormula = { formulaId -> backStack.add(FormulaEditorNavKey.edit(formulaId)) },
                            onDuplicateFormula = { formulaId -> backStack.add(FormulaEditorNavKey.duplicate(formulaId)) },
                            onHistoryClick = { backStack.add(FormulaHistoryRoute()) },
                            onFormulaIoClick = { backStack.add(FormulaIoNavKey) },
                            onPreferencesClick = { backStack.add(PreferencesRoute) },
                        )

                        formulaRunEntry(
                            repository = repository,
                            historyRepository = historyRepository,
                            onBack = { backStack.removeLastOrNull() },
                            onEdit = { formulaId -> backStack.add(FormulaEditorNavKey.edit(formulaId)) },
                            onDuplicate = { formulaId -> backStack.add(FormulaEditorNavKey.duplicate(formulaId)) },
                            onDeleted = {
                                val route = backStack.lastOrNull() as? FormulaRunRoute
                                scope.launch {
                                    route?.let { deleteFormula(it.formulaId) }
                                    popToList()
                                    refreshToken++
                                }
                            },
                        )

                        formulaEditorEntry(
                            repository = repository,
                            refreshKey = refreshToken,
                            nowProvider = { nowProvider.now() },
                            onBack = {
                                refreshToken++
                                popToList()
                            },
                        )

                        formulaHistoryEntry(
                            repository = historyRepository,
                            onBack = { backStack.removeLastOrNull() },
                        )

                        // TODO: LocalClipboardManager
                        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                        val clipboardService = remember(clipboardManager) {
                            com.formuladock.feature.formula.io.FormulaClipboardService { text ->
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(text))
                                true
                            }
                        }
                        val clipboardReader = remember(clipboardManager) {
                            com.formuladock.feature.formula.io.FormulaClipboardReader {
                                clipboardManager.getText()?.text
                            }
                        }

                        formulaIoEntry(
                            repository = repository,
                            filePicker = com.formuladock.feature.formula.io.FileKitFormulaFilePicker,
                            clipboard = clipboardService,
                            clipboardReader = clipboardReader,
                            nowProvider = { nowProvider.now() },
                            onBack = { backStack.removeLastOrNull() }
                        )

                        preferencesEntry(
                            onBack = { backStack.removeLastOrNull() },
                        )

                    },
                )
            }
            }
            }
        }
    }
}

private class NowProvider {
    private var value: Long = 1L
    fun now(): Long = value++
}
