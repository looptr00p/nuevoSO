package com.nuevoso.launcher.data.memory

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MemoryMigrationInstrumentedTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MemoryDb::class.java,
    )

    @Test
    fun migratesVersionOneToThreeAndPreservesMemoryRows() {
        createLegacyDatabase(version = 1) { db ->
            createVersionOneTables(db)
            insertFact(db)
            insertMessage(db)
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            MemoryMigrations.MIGRATION_1_2,
            MemoryMigrations.MIGRATION_2_3,
        )
        helper.closeWhenFinished(migrated)

        assertSingleString(migrated, "SELECT fact FROM user_facts WHERE id = 1", "Synthetic local fact")
        assertSingleString(migrated, "SELECT text FROM chat_messages WHERE id = 1", "Synthetic chat message")
    }

    @Test
    fun migratesVersionTwoAuditRowsAndAllowsAppendOnlyLifecycleEvents() {
        createLegacyDatabase(version = 2) { db ->
            createVersionOneTables(db)
            createVersionTwoAuditTable(db)
            insertFact(db)
            insertMessage(db)
            db.execSQL(
                "INSERT INTO action_audit_events " +
                    "(actionId, timestampMillis, toolName, riskLevel, policyDecision, sanitizedSummary, executionResultCategory, failureReason) " +
                    "VALUES ('action-1', 3000, 'open_app', 'R1_REVERSIBLE', 'ALLOW', 'legacy raw private payload', 'EXECUTED', 'raw failure')"
            )
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            MemoryMigrations.MIGRATION_2_3,
        )
        helper.closeWhenFinished(migrated)

        assertSingleString(migrated, "SELECT fact FROM user_facts WHERE id = 1", "Synthetic local fact")
        assertSingleString(migrated, "SELECT text FROM chat_messages WHERE id = 1", "Synthetic chat message")
        assertSingleString(
            migrated,
            "SELECT lifecycleStage FROM action_audit_events WHERE actionId = 'action-1'",
            "LEGACY_RECORDED",
        )
        val legacySummary = singleString(
            migrated,
            "SELECT sanitizedSummary FROM action_audit_events WHERE actionId = 'action-1'",
        )
        assertTrue(legacySummary.startsWith("[LEGACY_SUMMARY_REDACTED"))
        assertFalse(legacySummary.contains("legacy raw private payload"))
        assertSingleString(
            migrated,
            "SELECT safeFailureCode FROM action_audit_events WHERE actionId = 'action-1'",
            "UNKNOWN_FAILURE",
        )

        migrated.execSQL(
            "INSERT INTO action_audit_events " +
                "(eventId, actionId, timestampMillis, toolName, riskLevel, policyDecision, lifecycleStage, sanitizedSummary, executionResultCategory, safeFailureCode) " +
                "VALUES ('event-2', 'action-1', 4000, 'open_app', 'R1_REVERSIBLE', 'ALLOW', 'EXECUTION_STARTED', 'app_name=Clock', 'NOT_EXECUTED', 'NONE')"
        )
        migrated.execSQL(
            "INSERT INTO action_audit_events " +
                "(eventId, actionId, timestampMillis, toolName, riskLevel, policyDecision, lifecycleStage, sanitizedSummary, executionResultCategory, safeFailureCode) " +
                "VALUES ('event-3', 'action-1', 5000, 'open_app', 'R1_REVERSIBLE', 'ALLOW', 'EXECUTION_SUCCEEDED', 'app_name=Clock', 'EXECUTED', 'NONE')"
        )

        assertSingleLong(
            migrated,
            "SELECT COUNT(*) FROM action_audit_events WHERE actionId = 'action-1'",
            3L,
        )
    }

    private fun createLegacyDatabase(version: Int, block: (SQLiteDatabase) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DB)
        val db = context.openOrCreateDatabase(TEST_DB, Context.MODE_PRIVATE, null)
        db.use {
            block(it)
            it.version = version
        }
    }

    private fun createVersionOneTables(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS user_facts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "fact TEXT NOT NULL, " +
                "createdAt INTEGER NOT NULL" +
                ")"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS chat_messages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "role TEXT NOT NULL, " +
                "text TEXT NOT NULL, " +
                "createdAt INTEGER NOT NULL" +
                ")"
        )
    }

    private fun createVersionTwoAuditTable(db: SQLiteDatabase) {
        db.execSQL(MIGRATION_1_2_CREATE_ACTION_AUDIT_SQL)
    }

    private fun insertFact(db: SQLiteDatabase) {
        db.execSQL(
            "INSERT INTO user_facts (id, fact, createdAt) VALUES (1, 'Synthetic local fact', 1000)"
        )
    }

    private fun insertMessage(db: SQLiteDatabase) {
        db.execSQL(
            "INSERT INTO chat_messages (id, role, text, createdAt) VALUES (1, 'user', 'Synthetic chat message', 2000)"
        )
    }

    private fun assertSingleString(db: SupportSQLiteDatabase, query: String, expected: String) {
        assertEquals(expected, singleString(db, query))
    }

    private fun singleString(db: SupportSQLiteDatabase, query: String): String {
        db.query(query).use { cursor ->
            assertTrue(cursor.moveToFirst())
            return cursor.getString(0)
        }
    }

    private fun assertSingleLong(db: SupportSQLiteDatabase, query: String, expected: Long) {
        db.query(query).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(expected, cursor.getLong(0))
        }
    }

    companion object {
        private const val TEST_DB = "memory-migration-test.db"
    }
}
