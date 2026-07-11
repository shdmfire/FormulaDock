package com.formuladock.core.formula.io

import kotlinx.serialization.Serializable

@Serializable
data class FormulaExportFileDto(
    val schemaVersion: Int = FormulaJsonCodec.SCHEMA_VERSION,
    val app: String = FormulaJsonCodec.APP_NAME,
    val exportedAt: Long,
    val formulas: List<FormulaDefinitionDto>,
)
