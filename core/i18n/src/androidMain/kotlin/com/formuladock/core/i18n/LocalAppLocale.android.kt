package com.formuladock.core.i18n

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

actual object LocalAppLocale {

    private var initialLocale: Locale? = null

    @Composable
    actual infix fun provides(
        languageTag: String?,
    ): ProvidedValue<*> {
        val currentConfiguration = LocalConfiguration.current
        val context = LocalContext.current

        if (initialLocale == null) {
            initialLocale =
                currentConfiguration.locales.get(0)
                    ?: Locale.getDefault()
        }

        val locale = languageTag
            ?.let(Locale::forLanguageTag)
            ?: requireNotNull(initialLocale)

        Locale.setDefault(locale)

        val newConfiguration = Configuration(currentConfiguration).apply {
            setLocale(locale)
        }

        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(
            newConfiguration,
            context.resources.displayMetrics,
        )

        return LocalConfiguration.provides(newConfiguration)
    }
}
