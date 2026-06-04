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

const val CREATE_ACTION_AUDIT_EVENTS_V3_SQL =
    "CREATE TABLE IF NOT EXISTS `action_audit_events` (" +
        "`eventId` TEXT NOT NULL, " +
        "`actionId` TEXT NOT NULL, " +
        "`timestampMillis` INTEGER NOT NULL, " +
        "`toolName` TEXT NOT NULL, " +
        "`riskLevel` TEXT NOT NULL, " +
        "`policyDecision` TEXT NOT NULL, " +
        "`lifecycleStage` TEXT NOT NULL, " +
        "`sanitizedSummary` TEXT NOT NULL, " +
        "`executionResultCategory` TEXT NOT NULL, " +
        "`safeFailureCode` TEXT NOT NULL, " +
        "PRIMARY KEY(`eventId`)" +
        ")"

const val CREATE_ACTION_AUDIT_EVENTS_ACTION_ID_INDEX_SQL =
    "CREATE INDEX IF NOT EXISTS `index_action_audit_events_actionId` " +
        "ON `action_audit_events` (`actionId`)"

object MemoryMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(MIGRATION_1_2_CREATE_ACTION_AUDIT_SQL)
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `action_audit_events` RENAME TO `action_audit_events_v2`")
            db.execSQL(CREATE_ACTION_AUDIT_EVENTS_V3_SQL)
            db.execSQL(CREATE_ACTION_AUDIT_EVENTS_ACTION_ID_INDEX_SQL)
            db.execSQL(
                "INSERT INTO `action_audit_events` (" +
                    "`eventId`, `actionId`, `timestampMillis`, `toolName`, `riskLevel`, " +
                    "`policyDecision`, `lifecycleStage`, `sanitizedSummary`, " +
                    "`executionResultCategory`, `safeFailureCode`" +
                    ") SELECT " +
                    "`actionId` || ':legacy', " +
                    "`actionId`, " +
                    "`timestampMillis`, " +
                    "`toolName`, " +
                    "`riskLevel`, " +
                    "`policyDecision`, " +
                    "'LEGACY_RECORDED', " +
                    "'[LEGACY_SUMMARY_REDACTED length=' || length(`sanitizedSummary`) || ']', " +
                    "`executionResultCategory`, " +
                    "CASE WHEN `failureReason` IS NULL OR `failureReason` = '' " +
                    "THEN 'NONE' ELSE 'UNKNOWN_FAILURE' END " +
                    "FROM `action_audit_events_v2`"
            )
            db.execSQL("DROP TABLE `action_audit_events_v2`")
        }
    }
}
