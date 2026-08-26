package com.formuladock

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.activity.enableEdgeToEdge
import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.data.formula.SqlDelightFormulaRepository
import com.formuladock.core.data.history.CalculationHistoryRepository
import com.formuladock.core.data.history.SqlDelightCalculationHistoryRepository
import com.formuladock.core.database.DriverFactory
import com.formuladock.core.database.createDatabase
import com.formuladock.core.designsystem.component.LocalAppInForeground
import com.formuladock.core.designsystem.theme.FormulaDockTheme
import com.formuladock.core.model.formula.model.BuiltinFormulas
import com.formuladock.feature.formula.panel.FormulaCalculatorPanel

class QuickCalcActivity : ComponentActivity() {
    private val appInForeground = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        disableOpenAnimation()
        super.onCreate(savedInstanceState)

        val db = createDatabase(DriverFactory(this))
        val repository = SqlDelightFormulaRepository(db)
        val historyRepository = SqlDelightCalculationHistoryRepository(db)

        setContent {
            CompositionLocalProvider(LocalAppInForeground provides appInForeground.value) {
                FormulaDockTheme {
                    QuickCalcScreen(
                        repository = repository,
                        historyRepository = historyRepository,
                        onDismiss = { finishWithoutAnimation() }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appInForeground.value = true
    }

    override fun onStop() {
        appInForeground.value = false
        super.onStop()
    }

    private fun finishWithoutAnimation() {
        finish()
        disableCloseAnimation()
    }
}

@Composable
private fun QuickCalcScreen(
    repository: FormulaRepository,
    historyRepository: CalculationHistoryRepository,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss
                )
        )

        QuickCalcPanel(
            repository = repository,
            historyRepository = historyRepository,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
            onClose = onDismiss
        )
    }
}

@Composable
private fun QuickCalcPanel(
    repository: FormulaRepository,
    historyRepository: CalculationHistoryRepository,
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) {
            // consume click
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        FormulaCalculatorPanel(
            repository = repository,
            historyRepository = historyRepository,
            fallbackFormula = BuiltinFormulas.roadTripCost(0),
            onClose = onClose
        )
    }
}

private fun Activity.disableOpenAnimation() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        overrideActivityTransition(
            Activity.OVERRIDE_TRANSITION_OPEN,
            0,
            0
        )
    } else {
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}

private fun Activity.disableCloseAnimation() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        overrideActivityTransition(
            Activity.OVERRIDE_TRANSITION_CLOSE,
            0,
            0
        )
    } else {
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
