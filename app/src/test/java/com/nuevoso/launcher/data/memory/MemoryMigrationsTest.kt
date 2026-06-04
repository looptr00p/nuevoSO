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
}
