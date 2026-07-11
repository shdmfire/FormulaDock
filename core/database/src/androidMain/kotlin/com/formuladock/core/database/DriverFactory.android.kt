package com.formuladock.core.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DriverFactory(
    private val context: Context,
) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = FormulaDockDatabase.Schema,
            context = context,
            name = DatabaseName,
        )
    }
}

private const val DatabaseName = "formuladock.db"
