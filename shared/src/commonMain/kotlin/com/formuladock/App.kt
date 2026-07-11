package com.formuladock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.data.history.CalculationHistoryRepository
import com.formuladock.formula.InMemoryFormulaRepository
import com.formuladock.formula.InMemoryCalculationHistoryRepository
import com.formuladock.feature.formula.io.FormulaShareService
import com.formuladock.feature.formula.io.NoOpFormulaShareService
import com.formuladock.navigation.FormulaDockApp

@Composable
fun App() {
    val repository = remember { InMemoryFormulaRepository() }
    val historyRepository = remember { InMemoryCalculationHistoryRepository() }
    App(repository = repository, historyRepository = historyRepository)
}

@Composable
fun App(
    repository: FormulaRepository,
    historyRepository: CalculationHistoryRepository,
    shareService: FormulaShareService = NoOpFormulaShareService,
) {
    FormulaDockApp(
        repository = repository,
        historyRepository = historyRepository,
        shareService = shareService,
    )
}

@Composable
@Preview
fun AppPreview() {
    App()
}

