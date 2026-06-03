package com.nuevoso.launcher.ai

data class Msg(
    val role: String,                                 // "user" | "model" | "tool"
    val text: String = "",
    val toolCalls: List<ToolCall> = emptyList(),      // turnos "model"
    val toolResults: List<ToolResult> = emptyList(),  // turnos "tool"
)

data class ToolSpec(
    val name: String,
    val description: String,
    val parameters: Map<String, ParamSpec>,
    val required: List<String> = emptyList(),
)

data class ParamSpec(val type: String, val description: String, val enum: List<String>? = null)

data class ToolCall(val id: String, val name: String, val args: Map<String, String>)

data class ToolResult(val id: String, val result: String)

data class AiTurn(
    val text: String,
    val toolCalls: List<ToolCall> = emptyList(),
    val stopReason: String = "end_turn",
)
