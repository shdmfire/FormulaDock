package com.formuladock.feature.formula.io

interface FormulaIdGenerator {
    fun newFormulaId(): String
    fun newInputId(): String
    fun newConstantId(): String
    fun newOutputId(): String
}

class SimpleFormulaIdGenerator(
    private val prefix: String = "imported",
    private val nowProvider: () -> Long,
) : FormulaIdGenerator {
    private var next = 0L

    override fun newFormulaId(): String = nextId("formula")
    override fun newInputId(): String = nextId("input")
    override fun newConstantId(): String = nextId("constant")
    override fun newOutputId(): String = nextId("output")

    private fun nextId(kind: String): String = "${prefix}_${kind}_${nowProvider()}_${next++}"
}
