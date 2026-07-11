package com.formuladock

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.formuladock.core.data.formula.SqlDelightFormulaRepository
import com.formuladock.core.data.history.SqlDelightCalculationHistoryRepository
import com.formuladock.core.database.DriverFactory
import com.formuladock.core.database.createDatabase
import com.formuladock.feature.formula.io.AndroidFormulaShareService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.formuladock.core.i18n.AppLanguage
import com.formuladock.core.preferences.FormulaDockPreferences

class MainActivity : ComponentActivity() {
    private var isNotificationEnabledBySettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val db = createDatabase(DriverFactory(this))
        val repository = SqlDelightFormulaRepository(db)
        val historyRepository = SqlDelightCalculationHistoryRepository(db)

        setContent {
            App(
                repository = repository,
                historyRepository = historyRepository,
                shareService = AndroidFormulaShareService(this),
            )
        }

        val preferences = FormulaDockPreferences()
        lifecycleScope.launch {
            combine(
                preferences.quickCalculatorEnabled,
                preferences.quickCalculatorNotificationEnabled,
                preferences.language
            ) { enabled, notificationEnabled, lang ->
                Triple(enabled && notificationEnabled, lang, notificationEnabled)
            }.collect { (showNotification, lang, isEnabled) ->
                isNotificationEnabledBySettings = isEnabled
                if (showNotification) {
                    installQuickCalcNotification(lang)
                } else {
                    QuickCalcNotification.cancel(this@MainActivity)
                }
            }
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)} passing\n      in a {@link RequestMultiplePermissions} object for the {@link ActivityResultContract} and\n      handling the result in the {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NotificationPermissionRequestCode &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            if (isNotificationEnabledBySettings) {
                lifecycleScope.launch {
                    val lang = FormulaDockPreferences().language.first()
                    QuickCalcNotification.show(this@MainActivity, lang)
                }
            }
        }
    }

    private fun installQuickCalcNotification(language: AppLanguage) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            QuickCalcNotification.show(this, language)
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NotificationPermissionRequestCode
            )
        }
    }

    private companion object {
        const val NotificationPermissionRequestCode = 10
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
