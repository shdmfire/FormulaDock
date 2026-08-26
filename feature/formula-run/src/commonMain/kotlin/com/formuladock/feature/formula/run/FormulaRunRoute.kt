package com.formuladock.feature.formula.run

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.data.history.CalculationHistoryRepository
import com.formuladock.core.domain.formula.EvaluateFormulaUseCase
import com.formuladock.core.formula.engine.DefaultFormulaEngine
import com.formuladock.core.model.formula.model.FormulaDefinition
import com.formuladock.core.navigation.AppRoute
import kotlinx.serialization.Serializable

@Serializable
data class FormulaRunRoute(
    val formulaId: String,
    val initialInputs: Map<String, String>? = null,
) : AppRoute

fun EntryProviderScope<NavKey>.formulaRunEntry(
    repository: FormulaRepository,
    historyRepository: CalculationHistoryRepository,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onDeleted: () -> Unit,
) {
    entry<FormulaRunRoute> { route ->
        FormulaRunRoute(
            repository = repository,
            historyRepository = historyRepository,
            formulaId = route.formulaId,
            initialInputs = route.initialInputs,
            onBack = onBack,
            onEdit = { onEdit(route.formulaId) },
            onDuplicate = { onDuplicate(route.formulaId) },
            onDeleted = onDeleted,
        )
    }
}

@Composable
fun FormulaRunRoute(
    repository: FormulaRepository,
    historyRepository: CalculationHistoryRepository,
    formulaId: String,
    modifier: Modifier = Modifier,
    initialInputs: Map<String, String>? = null,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDeleted: () -> Unit,
) {
    var formula by remember(formulaId) { mutableStateOf<FormulaDefinition?>(null) }
    var isLoading by remember(formulaId) { mutableStateOf(true) }
    val evaluateFormula = remember { EvaluateFormulaUseCase(DefaultFormulaEngine()) }

    LaunchedEffect(repository, formulaId) {
        isLoading = true
        formula = repository.getFormula(formulaId)
        isLoading = false
    }

    when (val current = formula) {
        null -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(if (isLoading) "Loading formula..." else "Formula not found")
        }
        else -> FormulaRunScreen(
            formula = current,
            evaluateFormula = evaluateFormula,
            historyRepository = historyRepository,
            modifier = modifier.fillMaxSize(),
            initialInputs = initialInputs,
            onBack = onBack,
            onEdit = onEdit,
            onDuplicate = onDuplicate,
            onDelete = {
                onDeleted()
            },
        )
    }
}
