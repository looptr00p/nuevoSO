package com.nuevoso.launcher.data.memory

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [UserFact::class, ChatMessageEntity::class, ActionAuditEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class MemoryDb : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
}
