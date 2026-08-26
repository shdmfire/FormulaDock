package com.formuladock.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.sql.DriverManager

actual class DriverFactory(
    private val databasePath: String = DatabaseName,
) {
    actual fun createDriver(): SqlDriver {
        val isInMemory = databasePath == InMemoryDatabasePath
        val databaseFileExists = !isInMemory && File(databasePath).exists()
        val jdbcUrl = "jdbc:sqlite:$databasePath"
        val needsV1Migration = databaseFileExists && !hasTable(jdbcUrl, "calculation_session")

        return JdbcSqliteDriver(jdbcUrl).also { driver ->
            when {
                isInMemory || !databaseFileExists -> FormulaDockDatabase.Schema.create(driver)
                needsV1Migration -> FormulaDockDatabase.Schema.migrate(
                    driver = driver,
                    oldVersion = 1,
                    newVersion = FormulaDockDatabase.Schema.version,
                )
            }
            driver.execute(
                identifier = null,
                sql = "PRAGMA user_version = ${FormulaDockDatabase.Schema.version}",
                parameters = 0,
            )
        }
    }

    private fun hasTable(jdbcUrl: String, tableName: String): Boolean {
        return DriverManager.getConnection(jdbcUrl).use { connection ->
            connection.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1"
            ).use { statement ->
                statement.setString(1, tableName)
                statement.executeQuery().use { result -> result.next() }
            }
        }
    }
}

private const val DatabaseName = "formuladock.db"
private const val InMemoryDatabasePath = ":memory:"
