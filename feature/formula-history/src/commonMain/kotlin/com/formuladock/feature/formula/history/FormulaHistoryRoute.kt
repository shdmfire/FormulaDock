package com.formuladock.feature.formula.history

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.formuladock.core.data.history.CalculationHistoryRepository
import com.formuladock.core.navigation.AppRoute
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import formuladock.feature.formula_history.generated.resources.*

@Serializable
data class FormulaHistoryRoute(
    val formulaId: String? = null,
    val formulaTitle: String? = null,
) : AppRoute

fun EntryProviderScope<NavKey>.formulaHistoryEntry(
    repository: CalculationHistoryRepository,
    onBack: () -> Unit,
) {
    entry<FormulaHistoryRoute> { route ->
        FormulaHistoryScreen(
            repository = repository,
            formulaId = route.formulaId,
            formulaTitle = route.formulaTitle ?: stringResource(Res.string.history_all_formulas),
            onBackClick = onBack,
        )
    }
}
