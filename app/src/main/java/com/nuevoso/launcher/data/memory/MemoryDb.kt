package com.nuevoso.launcher.data.memory

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [UserFact::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MemoryDb : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
}
