package com.formuladock.core.database

import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): FormulaDockDatabase {
    return FormulaDockDatabase(driverFactory.createDriver())
}
