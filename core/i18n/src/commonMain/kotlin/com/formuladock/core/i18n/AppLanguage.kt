package com.formuladock.core.i18n

enum class AppLanguage(
    val languageTag: String?,
) {
    System(languageTag = null),
    English(languageTag = "en"),
    Chinese(languageTag = "zh"),
    ;

    companion object {
        fun fromStorageValue(value: String?): AppLanguage =
            entries.firstOrNull { it.languageTag == value } ?: System
    }
}
