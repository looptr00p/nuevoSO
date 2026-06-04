package com.nuevoso.launcher.data.memory

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryMigrationsTest {
    @Test
    fun migrationOneToTwoDoesNotIntentionallyDeleteMemoryData() {
        val sql = MIGRATION_1_2_CREATE_ACTION_AUDIT_SQL.lowercase()

        assertTrue(sql.contains("create table"))
        assertTrue(sql.contains("action_audit_events"))
        assertFalse(sql.contains("drop table"))
        assertFalse(sql.contains("delete from"))
        assertFalse(sql.contains("user_facts"))
        assertFalse(sql.contains("chat_messages"))
    }

    @Test
    fun migrationTwoToThreeCreatesAppendOnlyAuditTable() {
        val sql = CREATE_ACTION_AUDIT_EVENTS_V3_SQL.lowercase()
        val indexSql = CREATE_ACTION_AUDIT_EVENTS_ACTION_ID_INDEX_SQL.lowercase()

        assertTrue(sql.contains("create table"))
        assertTrue(sql.contains("action_audit_events"))
        assertTrue(sql.contains("primary key(`eventid`)"))
        assertTrue(sql.contains("`actionid` text not null"))
        assertTrue(sql.contains("`lifecyclestage` text not null"))
        assertTrue(sql.contains("`safefailurecode` text not null"))
        assertTrue(indexSql.contains("index_action_audit_events_actionid"))
        assertFalse(sql.contains("drop table"))
        assertFalse(sql.contains("delete from"))
    }
}
