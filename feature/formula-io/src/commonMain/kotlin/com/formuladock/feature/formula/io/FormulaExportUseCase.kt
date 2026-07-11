package com.formuladock.feature.formula.io

import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.formula.io.FormulaJsonCodec

data class FormulaExportPayload(
    val fileName: String,
    val content: String,
    val count: Int,
)

class FormulaExportUseCase(
    private val repository: FormulaRepository,
) {
    suspend fun loadUserFormulas() = repository.getAllFormulas().filter { !it.isBuiltin }

    suspend operator fun invoke(
        now: Long,
        includeBuiltins: Boolean = false,
    ): FormulaExportPayload? {
        val formulas = repository.getAllFormulas()
            .filter { includeBuiltins || !it.isBuiltin }
        if (formulas.isEmpty()) return null

        return FormulaExportPayload(
            fileName = DEFAULT_FILE_NAME,
            content = FormulaJsonCodec.encode(formulas, now, includeBuiltins = includeBuiltins),
            count = formulas.size,
        )
    }

    private companion object {
        const val DEFAULT_FILE_NAME = "formuladock-formulas.json"
    }
}
