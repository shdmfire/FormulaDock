package com.formuladock.feature.formula.io

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

class AndroidFormulaShareService(
    private val context: Context,
) : FormulaShareService {
    override suspend fun shareJson(fileName: String, content: String): Boolean {
        val safeFileName = fileName.ensureJsonFileName()
        val exportDir = File(context.cacheDir, "formula_exports").apply {
            mkdirs()
        }
        val file = File(exportDir, safeFileName)
        file.writeText(content)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "分享公式").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        return true
    }
}

private fun String.ensureJsonFileName(): String =
    if (endsWith(".json", ignoreCase = true)) this else "$this.json"
