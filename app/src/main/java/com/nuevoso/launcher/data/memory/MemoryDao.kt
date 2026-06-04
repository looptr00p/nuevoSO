package com.nuevoso.launcher.data.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    @Insert
    suspend fun insertFact(fact: UserFact)

    @Query("SELECT * FROM user_facts ORDER BY createdAt ASC")
    fun getAllFacts(): Flow<List<UserFact>>

    @Query("SELECT * FROM user_facts ORDER BY createdAt ASC")
    suspend fun getAllFactsList(): List<UserFact>

    @Query("DELETE FROM user_facts")
    suspend fun clearFacts()

    @Insert
    suspend fun insertMessage(msg: ChatMessageEntity)

    @Query("SELECT * FROM chat_messages ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int): List<ChatMessageEntity>

    @Query("DELETE FROM chat_messages")
    suspend fun clearMessages()

    @Insert
    suspend fun insertActionAuditEvent(event: ActionAuditEntity)
}
