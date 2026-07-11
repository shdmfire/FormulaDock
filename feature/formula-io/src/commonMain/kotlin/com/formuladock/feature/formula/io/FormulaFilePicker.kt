package com.formuladock.feature.formula.io

data class FormulaPickedFile(
    val fileName: String,
    val content: String,
)

fun interface FormulaFilePicker {
    suspend fun pickJsonFile(): FormulaPickedFile?
}

object NoOpFormulaFilePicker : FormulaFilePicker {
    override suspend fun pickJsonFile(): FormulaPickedFile? = null
}
