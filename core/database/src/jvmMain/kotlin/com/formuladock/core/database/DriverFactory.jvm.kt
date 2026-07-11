package com.formuladock.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DriverFactory(
    private val databasePath: String = DatabaseName,
) {
    actual fun createDriver(): SqlDriver {
        val shouldCreateSchema = databasePath == InMemoryDatabasePath || !File(databasePath).exists()
        return JdbcSqliteDriver("jdbc:sqlite:$databasePath").also { driver ->
            if (shouldCreateSchema) {
                FormulaDockDatabase.Schema.create(driver)
            }
        }
    }
}

private const val DatabaseName = "formuladock.db"
private const val InMemoryDatabasePath = ":memory:"
