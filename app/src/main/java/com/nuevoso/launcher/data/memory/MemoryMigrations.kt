package com.nuevoso.launcher.data.memory

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

const val MIGRATION_1_2_CREATE_ACTION_AUDIT_SQL =
    "CREATE TABLE IF NOT EXISTS `action_audit_events` (" +
        "`actionId` TEXT NOT NULL, " +
        "`timestampMillis` INTEGER NOT NULL, " +
        "`toolName` TEXT NOT NULL, " +
        "`riskLevel` TEXT NOT NULL, " +
        "`policyDecision` TEXT NOT NULL, " +
        "`sanitizedSummary` TEXT NOT NULL, " +
        "`executionResultCategory` TEXT NOT NULL, " +
        "`failureReason` TEXT, " +
        "PRIMARY KEY(`actionId`)" +
        ")"

object MemoryMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(MIGRATION_1_2_CREATE_ACTION_AUDIT_SQL)
        }
    }
}
