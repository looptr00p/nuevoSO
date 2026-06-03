package com.nuevoso.launcher.ai.gemini

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class GeminiRequest(
    @SerialName("system_instruction") val systemInstruction: GeminiSystemInstruction? = null,
    val contents: List<GeminiContent>,
    val tools: List<GeminiTool>? = null,
    @SerialName("generationConfig") val generationConfig: GeminiGenerationConfig? = null,
)

@Serializable
data class GeminiSystemInstruction(val parts: List<GeminiPart>)

@Serializable
data class GeminiGenerationConfig(
    val temperature: Float = 0.7f,
    @SerialName("maxOutputTokens") val maxOutputTokens: Int = 2048,
)

@Serializable
data class GeminiContent(
    val role: String,
    val parts: List<GeminiPart>,
)

@Serializable
data class GeminiPart(
    val text: String? = null,
    @SerialName("functionCall") val functionCall: GeminiFunctionCall? = null,
    @SerialName("functionResponse") val functionResponse: GeminiFunctionResponse? = null,
)

@Serializable
data class GeminiFunctionCall(
    val name: String,
    val args: Map<String, JsonElement> = emptyMap(),
)

@Serializable
data class GeminiFunctionResponse(
    val name: String,
    val response: Map<String, JsonElement>,
)

@Serializable
data class GeminiTool(
    @SerialName("functionDeclarations") val functionDeclarations: List<GeminiFunctionDeclaration>,
)

@Serializable
data class GeminiFunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: GeminiSchema,
)

@Serializable
data class GeminiSchema(
    val type: String = "OBJECT",
    val properties: Map<String, GeminiProperty> = emptyMap(),
    val required: List<String> = emptyList(),
)

@Serializable
data class GeminiProperty(
    val type: String,
    val description: String,
    val enum: List<String>? = null,
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null,
    @SerialName("finishReason") val finishReason: String? = null,
)
