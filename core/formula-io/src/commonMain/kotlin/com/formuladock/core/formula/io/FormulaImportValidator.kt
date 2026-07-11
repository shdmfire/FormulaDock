package com.formuladock.core.formula.io

internal object FormulaImportValidator {
    private val keyRegex = Regex("[A-Za-z_][A-Za-z0-9_]*")

    fun validateContent(content: String): List<String> =
        if (content.isBlank()) listOf("文件为空") else emptyList()

    fun validateFile(file: FormulaExportFileDto): List<String> = buildList {
        if (file.schemaVersion != FormulaJsonCodec.SCHEMA_VERSION) {
            add("不支持的 schemaVersion：${file.schemaVersion}")
        }
        if (file.formulas.isEmpty()) add("公式列表为空")
        file.formulas.forEachIndexed { index, formula -> validateFormula(index, formula).forEach(::add) }
    }

    private fun validateFormula(index: Int, formula: FormulaDefinitionDto): List<String> = buildList {
        val name = formula.title.ifBlank { "第 ${index + 1} 个公式" }
        if (formula.title.isBlank()) add("第 ${index + 1} 个公式标题为空")
        if (formula.outputs.isEmpty()) add("$name 的输出列表为空")

        val keys = formula.inputs.map { it.key } + formula.constants.map { it.key } + formula.outputs.map { it.key }
        keys.filter { it.isNotBlank() }.groupingBy { it }.eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { add("$name 的 key 重复：$it") }

        formula.inputs.forEach { input ->
            validateKey(name, input.key)?.let(::add)
            if (input.defaultValue?.toDoubleOrNull() == null && input.defaultValue != null) {
                add("$name 的输入 ${input.key} 默认值不是数字")
            }
        }
        formula.constants.forEach { constant ->
            validateKey(name, constant.key)?.let(::add)
            if (constant.value.toDoubleOrNull() == null) add("$name 的常量 ${constant.key} 不是数字")
        }
        formula.outputs.forEach { output ->
            validateKey(name, output.key)?.let(::add)
            if (output.expression.isBlank()) add("$name 的输出 ${output.key} 表达式为空")
            if (output.precision !in 0..10) add("$name 的输出 ${output.key} 精度超出范围")
        }
    }

    private fun validateKey(formulaName: String, key: String): String? =
        if (keyRegex.matches(key)) null else "$formulaName 的 key 不合法：$key"
}
