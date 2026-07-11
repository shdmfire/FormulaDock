package com.formuladock.feature.formula.editor

import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.domain.formula.editor.FormulaEditorMode
import com.formuladock.core.navigation.AppRoute
import kotlinx.serialization.Serializable

@Serializable
data class FormulaEditorNavKey(
    val mode: Mode,
    val formulaId: String? = null,
) : AppRoute {
    @Serializable
    enum class Mode { Create, Edit, Duplicate }

    fun toEditorMode(): FormulaEditorMode = when (mode) {
        Mode.Create -> FormulaEditorMode.Create
        Mode.Edit -> FormulaEditorMode.Edit(requireNotNull(formulaId))
        Mode.Duplicate -> FormulaEditorMode.Duplicate(requireNotNull(formulaId))
    }

    companion object {
        fun create() = FormulaEditorNavKey(Mode.Create)
        fun edit(formulaId: String) = FormulaEditorNavKey(Mode.Edit, formulaId)
        fun duplicate(formulaId: String) = FormulaEditorNavKey(Mode.Duplicate, formulaId)
    }
}

fun EntryProviderScope<NavKey>.formulaEditorEntry(
    repository: FormulaRepository,
    refreshKey: Any? = null,
    nowProvider: () -> Long = { 0L },
    onBack: () -> Unit,
) {
    entry<FormulaEditorNavKey> { route ->
        FormulaEditorRoute(
            repository = repository,
            mode = route.toEditorMode(),
            refreshKey = refreshKey,
            modifier = Modifier,
            nowProvider = nowProvider,
            onBackClick = onBack,
        )
    }
}
