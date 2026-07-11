package com.formuladock.feature.formula.io

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readString

fun interface FormulaClipboardService {
    suspend fun writeText(text: String): Boolean
}

fun interface FormulaClipboardReader {
    suspend fun readText(): String?
}

fun interface FormulaShareService {
    suspend fun shareJson(fileName: String, content: String): Boolean
}

object NoOpFormulaClipboardService : FormulaClipboardService {
    override suspend fun writeText(text: String): Boolean = false
}

object NoOpFormulaClipboardReader : FormulaClipboardReader {
    override suspend fun readText(): String? = null
}

object NoOpFormulaShareService : FormulaShareService {
    override suspend fun shareJson(fileName: String, content: String): Boolean = false
}

val LocalFormulaShareService = androidx.compose.runtime.staticCompositionLocalOf<FormulaShareService> { NoOpFormulaShareService }

object FileKitFormulaFilePicker : FormulaFilePicker {
    override suspend fun pickJsonFile(): FormulaPickedFile? {
        val file = FileKit.openFilePicker(
            type = FileKitType.File(listOf("json"))
        ) ?: return null
        return FormulaPickedFile(
            fileName = file.name,
            content = file.readString()
        )
    }
}

internal fun formulaJsonFileName(title: String): String {
    val safe = title.trim().ifBlank { "formula" }
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
    return "$safe.formula.json"
}
