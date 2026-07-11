package com.formuladock.core.domain.formula.panel

import com.formuladock.core.model.formula.model.FormulaDefinition

data class FormulaPanelDomainState(
    val formulas: List<FormulaDefinition> = emptyList(),
    val selectedFormulaId: String? = null,
    val inputValues: Map<String, String> = emptyMap(),
) {
    val selectedFormula: FormulaDefinition? get() = formulas.firstOrNull { it.id == selectedFormulaId }
}

sealed interface FormulaPanelCommand {
    data class SetFormulas(
        val formulas: List<FormulaDefinition>,
        val preferredFormulaId: String? = null,
    ) : FormulaPanelCommand

    data class SelectFormula(val formulaId: String) : FormulaPanelCommand
    data class UpdateInputValue(val key: String, val value: String) : FormulaPanelCommand
    data object ClearInputs : FormulaPanelCommand
}

object FormulaPanelReducer {
    fun reduce(
        state: FormulaPanelDomainState,
        command: FormulaPanelCommand,
    ): FormulaPanelDomainState = when (command) {
        is FormulaPanelCommand.SetFormulas -> {
            val selectedId = command.preferredFormulaId
                ?.takeIf { preferred -> command.formulas.any { it.id == preferred } }
                ?: command.formulas.firstOrNull()?.id
            state.copy(
                formulas = command.formulas,
                selectedFormulaId = selectedId,
                inputValues = inputDefaults(command.formulas.firstOrNull { it.id == selectedId }),
            )
        }
        is FormulaPanelCommand.SelectFormula -> state.copy(
            selectedFormulaId = command.formulaId,
            inputValues = inputDefaults(state.formulas.firstOrNull { it.id == command.formulaId }),
        )
        is FormulaPanelCommand.UpdateInputValue -> state.copy(
            inputValues = state.inputValues + (command.key to command.value),
        )
        FormulaPanelCommand.ClearInputs -> state.copy(inputValues = emptyMap())
    }

    private fun inputDefaults(formula: FormulaDefinition?): Map<String, String> {
        return formula?.inputs
            ?.associate { input -> input.key to input.defaultValue.orEmpty() }
            .orEmpty()
    }
}
