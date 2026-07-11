package com.formuladock.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.key

expect object LocalAppLocale {
    @Composable
    infix fun provides(languageTag: String?): ProvidedValue<*>
}

@Composable
fun AppLocaleProvider(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppLocale provides language.languageTag,
    ) {
        // 语言变化时重新创建对应 UI 子树，确保资源重新读取。
        key(language.languageTag) {
            content()
        }
    }
}
