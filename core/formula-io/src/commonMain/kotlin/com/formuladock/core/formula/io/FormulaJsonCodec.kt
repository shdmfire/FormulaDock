package com.formuladock.core.formula.io

import com.formuladock.core.model.formula.model.FormulaDefinition
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object FormulaJsonCodec {
    const val SCHEMA_VERSION = 1
    const val APP_NAME = "FormulaDock"

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(
        formulas: List<FormulaDefinition>,
        now: Long,
        includeBuiltins: Boolean = false,
    ): String {
        val exported = formulas
            .filter { includeBuiltins || !it.isBuiltin }
            .map { it.toDto() }
        return json.encodeToString(
            FormulaExportFileDto(
                exportedAt = now,
                formulas = exported,
            ),
        )
    }

    fun decode(
        content: String,
        options: FormulaImportOptions,
    ): FormulaImportResult {
        FormulaImportValidator.validateContent(content).takeIf { it.isNotEmpty() }?.let {
            return FormulaImportResult.Failure(it)
        }

        val file = try {
            json.decodeFromString<FormulaExportFileDto>(content)
        } catch (_: SerializationException) {
            return FormulaImportResult.Failure(listOf("JSON 格式错误"))
        } catch (_: IllegalArgumentException) {
            return FormulaImportResult.Failure(listOf("JSON 格式错误"))
        }

        FormulaImportValidator.validateFile(file).takeIf { it.isNotEmpty() }?.let {
            return FormulaImportResult.Failure(it)
        }

        val formulas = file.formulas
            .filter { options.includeBuiltinsFromFile || !it.isBuiltin }
            .map { dto -> dto.toModel(options) }

        if (formulas.isEmpty()) return FormulaImportResult.Failure(listOf("公式列表为空"))

        return FormulaImportResult.Success(formulas)
    }
}
