package com.nuevoso.launcher.ai.gemini

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.nuevoso.launcher.ai.AiProvider
import com.nuevoso.launcher.ai.AiTurn
import com.nuevoso.launcher.ai.Msg
import com.nuevoso.launcher.ai.ParamSpec
import com.nuevoso.launcher.ai.ToolCall
import com.nuevoso.launcher.ai.ToolResult
import com.nuevoso.launcher.ai.ToolSpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

class GeminiProvider(
    private val apiKey: String,
    private val modelId: String = "gemini-2.5-flash",
) : AiProvider {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val api: GeminiApi by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApi::class.java)
    }

    override suspend fun chat(
        system: String,
        history: List<Msg>,
        tools: List<ToolSpec>,
        toolResults: List<ToolResult>,
    ): AiTurn {
        val contents = mutableListOf<GeminiContent>()

        for (msg in history) {
            val geminiRole = if (msg.role == "user") "user" else "model"
            contents.add(GeminiContent(role = geminiRole, parts = listOf(GeminiPart(text = msg.text))))
        }

        for (tr in toolResults) {
            contents.add(
                GeminiContent(
                    role = "function",
                    parts = listOf(
                        GeminiPart(
                            functionResponse = GeminiFunctionResponse(
                                name = tr.id,
                                response = mapOf("result" to JsonPrimitive(tr.result)),
                            )
                        )
                    )
                )
            )
        }

        val geminiTools = if (tools.isNotEmpty()) listOf(
            GeminiTool(tools.map { spec ->
                GeminiFunctionDeclaration(
                    name = spec.name,
                    description = spec.description,
                    parameters = GeminiSchema(
                        type = "OBJECT",
                        properties = spec.parameters.mapValues { (_, p) ->
                            GeminiProperty(
                                type = p.type.uppercase(),
                                description = p.description,
                                enum = p.enum,
                            )
                        },
                        required = spec.required,
                    )
                )
            })
        ) else null

        val request = GeminiRequest(
            systemInstruction = GeminiSystemInstruction(listOf(GeminiPart(text = system))),
            contents = contents,
            tools = geminiTools,
            generationConfig = GeminiGenerationConfig(),
        )

        val response = api.generateContent(modelId, apiKey, request)
        val candidate = response.candidates.firstOrNull()
        val parts = candidate?.content?.parts ?: emptyList()

        val textParts = parts.mapNotNull { it.text }.joinToString("")
        val toolCalls = parts.mapNotNull { part ->
            part.functionCall?.let { fc ->
                ToolCall(
                    id = fc.name,
                    name = fc.name,
                    args = fc.args.mapValues { (_, v) ->
                        if (v is JsonPrimitive) v.content else v.toString().removeSurrounding("\"")
                    },
                )
            }
        }

        val stopReason = when {
            toolCalls.isNotEmpty() -> "tool_use"
            else -> "end_turn"
        }

        return AiTurn(text = textParts, toolCalls = toolCalls, stopReason = stopReason)
    }
}
