package com.formuladock.feature.formula.io

import com.formuladock.core.data.formula.FormulaRepository
import com.formuladock.core.formula.io.FormulaImportOptions
import com.formuladock.core.formula.io.FormulaImportResult
import com.formuladock.core.formula.io.FormulaJsonCodec
import com.formuladock.core.model.formula.model.FormulaDefinition

class FormulaImportUseCase(
    private val repository: FormulaRepository,
    private val idGenerator: FormulaIdGenerator,
) {
    suspend fun preview(file: FormulaPickedFile, now: Long): FormulaImportPreviewResult {
        validateFile(file).takeIf { it.isNotEmpty() }?.let {
            return FormulaImportPreviewResult.Failure(it)
        }

        return when (val result = FormulaJsonCodec.decode(file.content, options(now))) {
            is FormulaImportResult.Failure -> FormulaImportPreviewResult.Failure(result.errors)
            is FormulaImportResult.Success -> {
                val existingTitles = repository.getAllFormulas().map { it.title }.toSet()
                val renamedTitles = mutableSetOf<String>()
                val itemWarnings = mutableListOf<String?>()
                val globalWarnings = result.warnings.toMutableList()
                val formulas = result.formulas.map { formula ->
                    val renamed = formula.title.uniqueTitle(existingTitles + renamedTitles)
                    val warning = if (renamed != formula.title) {
                        val msg = "公式「${formula.title}」已重命名为「$renamed」"
                        globalWarnings += msg
                        msg
                    } else null
                    itemWarnings.add(warning)
                    renamedTitles += renamed
                    formula.copy(title = renamed, isBuiltin = false).withNewIds()
                }
                FormulaImportPreviewResult.Success(
                    formulas = formulas,
                    preview = FormulaImportPreviewUiModel(
                        formulaCount = formulas.size,
                        formulaTitles = formulas.map { it.title },
                        warnings = globalWarnings,
                    ),
                    itemWarnings = itemWarnings,
                )
            }
        }
    }

    suspend fun importFormulas(formulas: List<FormulaDefinition>) {
        repository.importFormulas(formulas)
    }

    private fun validateFile(file: FormulaPickedFile) = buildList {
        if (!file.fileName.endsWith(".json", ignoreCase = true)) add("请选择 .json 文件")
        if (file.content.length > MAX_JSON_CHARS) add("文件过大，请选择较小的公式 JSON 文件")
        if (file.content.isBlank()) add("文件为空")
    }

    private fun options(now: Long) = FormulaImportOptions(
        now = now,
        includeBuiltinsFromFile = true,
    )

    private fun FormulaDefinition.withNewIds() = copy(
        id = idGenerator.newFormulaId(),
        inputs = inputs.map { it.copy(id = idGenerator.newInputId()) },
        constants = constants.map { it.copy(id = idGenerator.newConstantId()) },
        outputs = outputs.map { it.copy(id = idGenerator.newOutputId()) },
    )

    private fun String.uniqueTitle(used: Set<String>): String {
        if (this !in used) return this
        var index = 1
        while (true) {
            val candidate = if (index == 1) "$this (导入)" else "$this (导入 $index)"
            if (candidate !in used) return candidate
            index++
        }
    }

    private companion object {
        const val MAX_JSON_CHARS = 5 * 1024 * 1024
    }
}

sealed interface FormulaImportPreviewResult {
    data class Success(
        val formulas: List<FormulaDefinition>,
        val preview: FormulaImportPreviewUiModel,
        val itemWarnings: List<String?>,
    ) : FormulaImportPreviewResult

    data class Failure(
        val errors: List<String>,
    ) : FormulaImportPreviewResult
}
