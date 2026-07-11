package com.formuladock.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = FormulaDockDatabase.Schema,
            name = DatabaseName,
        )
    }
}

private const val DatabaseName = "formuladock.db"
