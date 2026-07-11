package com.formuladock.feature.formula.list

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.navigation.AppRoute
import kotlinx.serialization.Serializable

@Serializable
data object FormulaListRoute : AppRoute

fun EntryProviderScope<NavKey>.formulaListEntry(
    repository: FormulaRepository,
    onRunFormula: (String) -> Unit,
    onCreateFormula: () -> Unit,
    onEditFormula: (String) -> Unit,
    onDuplicateFormula: (String) -> Unit,
    onHistoryClick: () -> Unit = {},
    onFormulaIoClick: () -> Unit = {},
    onPreferencesClick: () -> Unit = {},
) {
    entry<FormulaListRoute> {
        FormulaListContainer(
            repository = repository,
            onRunFormula = { onRunFormula(it.id) },
            onCreateFormula = onCreateFormula,
            onEditFormula = onEditFormula,
            onDuplicateFormula = onDuplicateFormula,
            onHistoryClick = onHistoryClick,
            onPreferencesClick = onPreferencesClick,
            onImportExportClick = onFormulaIoClick,
        )
    }
}
