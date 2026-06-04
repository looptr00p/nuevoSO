package com.nuevoso.launcher.data.memory

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_facts")
data class UserFact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fact: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,   // "user" | "model"
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "action_audit_events",
    indices = [Index(value = ["actionId"])],
)
data class ActionAuditEntity(
    @PrimaryKey val eventId: String,
    val actionId: String,
    val timestampMillis: Long,
    val toolName: String,
    val riskLevel: String,
    val policyDecision: String,
    val lifecycleStage: String,
    val sanitizedSummary: String,
    val executionResultCategory: String,
    val safeFailureCode: String,
)
