package com.formuladock.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

actual object LocalAppLocale {

    private var initialLocale: Locale? = null

    private val LocalLocaleTag = staticCompositionLocalOf {
        Locale.getDefault().toLanguageTag()
    }

    @Composable
    actual infix fun provides(
        languageTag: String?,
    ): ProvidedValue<*> {
        if (initialLocale == null) {
            initialLocale = Locale.getDefault()
        }

        val locale = languageTag
            ?.let(Locale::forLanguageTag)
            ?: requireNotNull(initialLocale)

        Locale.setDefault(locale)

        return LocalLocaleTag.provides(
            locale.toLanguageTag(),
        )
    }
}
