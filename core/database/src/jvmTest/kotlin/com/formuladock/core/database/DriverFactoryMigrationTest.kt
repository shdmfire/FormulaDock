package com.formuladock.core.database

import java.nio.file.Files
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals

class DriverFactoryMigrationTest {
    @Test
    fun migratesExistingDesktopHistoryIntoClosedSession() {
        val path = Files.createTempFile("formuladock-v1", ".db")
        val url = "jdbc:sqlite:${path.toAbsolutePath()}"
        try {
            DriverManager.getConnection(url).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        """
                        CREATE TABLE calculation_history (
                            id TEXT NOT NULL PRIMARY KEY,
                            formula_id TEXT NOT NULL,
                            formula_title TEXT NOT NULL,
                            formula_description TEXT,
                            formula_is_builtin INTEGER NOT NULL DEFAULT 0,
                            status TEXT NOT NULL,
                            error_message TEXT,
                            error_field_key TEXT,
                            note TEXT,
                            created_at INTEGER NOT NULL,
                            updated_at INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    statement.execute(
                        """
                        INSERT INTO calculation_history VALUES (
                            'old_1', 'loan', 'Loan', NULL, 1, 'SUCCESS',
                            NULL, NULL, NULL, 1000, 1200
                        )
                        """.trimIndent()
                    )
                }
            }

            DriverFactory(path.toString()).createDriver().close()

            DriverManager.getConnection(url).use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery(
                        "SELECT status, revision_count, final_revision_id FROM calculation_session"
                    ).use { result ->
                        result.next()
                        assertEquals("CLOSED", result.getString(1))
                        assertEquals(1, result.getInt(2))
                        assertEquals("old_1", result.getString(3))
                    }
                    statement.executeQuery(
                        "SELECT session_id, revision_no FROM calculation_history WHERE id = 'old_1'"
                    ).use { result ->
                        result.next()
                        assertEquals("session_old_1", result.getString(1))
                        assertEquals(1, result.getInt(2))
                    }
                }
            }
        } finally {
            Files.deleteIfExists(path)
        }
    }
}
