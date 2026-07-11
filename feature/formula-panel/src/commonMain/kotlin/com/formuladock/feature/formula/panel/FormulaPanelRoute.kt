package com.formuladock.feature.formula.panel

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.navigation.AppRoute
import kotlinx.serialization.Serializable

@Serializable
data object FormulaPanelRoute : AppRoute

fun EntryProviderScope<NavKey>.formulaPanelEntry(
    repository: FormulaRepository,
    defaultFormulaId: String? = null,
    onClose: (() -> Unit)? = null,
) {
    entry<FormulaPanelRoute> {
        FormulaCalculatorPanel(
            repository = repository,
            defaultFormulaId = defaultFormulaId,
            onClose = onClose,
        )
    }
}
