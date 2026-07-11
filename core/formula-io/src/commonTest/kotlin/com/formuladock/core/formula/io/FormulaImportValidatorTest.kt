package com.formuladock.core.formula.io

import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FormulaImportValidatorTest {
    @Test
    fun decodeReportsValidationErrors() {
        val result = FormulaJsonCodec.decode(
            content = """
                {
                  "schemaVersion": 1,
                  "app": "FormulaDock",
                  "exportedAt": 1,
                  "formulas": [
                    {
                      "id": "f1",
                      "title": "",
                      "inputs": [{"id":"i1","key":"1x","label":"X","defaultValue":"abc"}],
                      "constants": [{"id":"c1","key":"rate","label":"Rate","value":"bad"}],
                      "outputs": [{"id":"o1","key":"rate","label":"Y","expression":"","precision":11}],
                      "createdAt": 1,
                      "updatedAt": 1
                    }
                  ]
                }
            """.trimIndent(),
            options = FormulaImportOptions(1L),
        )

        val failure = assertIs<FormulaImportResult.Failure>(result)
        assertTrue(failure.errors.any { it.contains("标题为空") })
        assertTrue(failure.errors.any { it.contains("key 不合法") })
        assertTrue(failure.errors.any { it.contains("默认值不是数字") })
        assertTrue(failure.errors.any { it.contains("不是数字") })
        assertTrue(failure.errors.any { it.contains("表达式为空") })
        assertTrue(failure.errors.any { it.contains("精度超出范围") })
    }
}
