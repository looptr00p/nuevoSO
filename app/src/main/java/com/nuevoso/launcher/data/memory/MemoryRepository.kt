package com.nuevoso.launcher.data.memory

import com.nuevoso.launcher.agent.security.ActionAuditEvent
import kotlinx.coroutines.flow.Flow

class MemoryRepository(private val dao: MemoryDao) {

    val facts: Flow<List<UserFact>> = dao.getAllFacts()

    suspend fun rememberFact(fact: String) {
        dao.insertFact(UserFact(fact = fact))
    }

    suspend fun clearFacts() {
        dao.clearFacts()
    }

    suspend fun buildMemoryContext(): String {
        val recent = dao.getRecentMessages(40).reversed()
        val facts = dao.getAllFactsList()

        val factsSection = if (facts.isNotEmpty())
            "Lo que sé sobre el usuario:\n" + facts.joinToString("\n") { "- ${it.fact}" }
        else ""

        val historySection = if (recent.isNotEmpty())
            "\nHistorial reciente:\n" + recent.joinToString("\n") { msg ->
                val prefix = if (msg.role == "user") "Usuario" else "Asistente"
                "$prefix: ${msg.text}"
            }
        else ""

        return listOf(factsSection, historySection).filter { it.isNotBlank() }.joinToString("\n\n")
    }

    suspend fun saveMessage(role: String, text: String) {
        dao.insertMessage(ChatMessageEntity(role = role, text = text))
    }

    suspend fun clearHistory() {
        dao.clearMessages()
    }

    suspend fun recordActionAudit(event: ActionAuditEvent) {
        dao.insertActionAuditEvent(
            ActionAuditEntity(
                actionId = event.actionId,
                timestampMillis = event.timestampMillis,
                toolName = event.toolName,
                riskLevel = event.riskLevel.name,
                policyDecision = event.policyDecision.name,
                sanitizedSummary = event.sanitizedSummary,
                executionResultCategory = event.executionResultCategory.name,
                failureReason = event.failureReason,
            )
        )
    }
}
