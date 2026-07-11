package com.formuladock.feature.preferences

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.formuladock.core.navigation.AppRoute
import kotlinx.serialization.Serializable

@Serializable
data object PreferencesRoute : AppRoute

fun EntryProviderScope<NavKey>.preferencesEntry(
    onBack: () -> Unit,
) {
    entry<PreferencesRoute> {
        PreferencesContainer(onBack = onBack)
    }
}
