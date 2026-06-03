package com.nuevoso.launcher.ai.gemini

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.nuevoso.launcher.ai.AiProvider
import com.nuevoso.launcher.ai.AiTurn
import com.nuevoso.launcher.ai.Msg
import com.nuevoso.launcher.ai.ParamSpec
import com.nuevoso.launcher.ai.ToolCall
import com.nuevoso.launcher.ai.ToolSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.HttpException
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
        onTextDelta: (String) -> Unit,
    ): AiTurn {
        // Retry con backoff exponencial en caso de rate limit (429)
        var lastError: Exception? = null
        for (attempt in 0..3) {
            try {
                return chatInternal(system, history, tools, onTextDelta)
            } catch (e: HttpException) {
                if (e.code() == 429) {
                    lastError = e
                    val waitMs = (1L shl attempt) * 5_000L  // 5s, 10s, 20s, 40s
                    delay(waitMs)
                } else throw e
            }
        }
        throw lastError!!
    }

    private suspend fun chatInternal(
        system: String,
        history: List<Msg>,
        tools: List<ToolSpec>,
        onTextDelta: (String) -> Unit,
    ): AiTurn {
        val contents = mutableListOf<GeminiContent>()

        // El transcript llega como una lista ordenada de Msg: texto de usuario/modelo,
        // los functionCall que emitió el modelo y los functionResponse de las herramientas.
        for (msg in history) {
            when (msg.role) {
                "user" -> contents.add(
                    GeminiContent(role = "user", parts = listOf(GeminiPart(text = msg.text)))
                )
                "tool" -> {
                    val parts = msg.toolResults.map { tr ->
                        GeminiPart(
                            functionResponse = GeminiFunctionResponse(
                                name = tr.id,
                                response = mapOf("result" to JsonPrimitive(tr.result)),
                            )
                        )
                    }
                    if (parts.isNotEmpty()) contents.add(GeminiContent(role = "function", parts = parts))
                }
                else -> { // "model"
                    val parts = mutableListOf<GeminiPart>()
                    if (msg.text.isNotBlank()) parts.add(GeminiPart(text = msg.text))
                    for (tc in msg.toolCalls) {
                        parts.add(
                            GeminiPart(
                                functionCall = GeminiFunctionCall(
                                    name = tc.name,
                                    args = tc.args.mapValues { (_, v) -> JsonPrimitive(v) },
                                )
                            )
                        )
                    }
                    if (parts.isNotEmpty()) contents.add(GeminiContent(role = "model", parts = parts))
                }
            }
        }

        val geminiTools = if (tools.isNotEmpty()) listOf(
            GeminiTool(tools.map { spec ->
                GeminiFunctionDeclaration(
                    name = spec.name,
                    description = spec.description,
                    parameters = if (spec.parameters.isNotEmpty()) GeminiSchema(
                        type = "OBJECT",
                        properties = spec.parameters.mapValues { (_, p) ->
                            GeminiProperty(
                                type = p.type.uppercase(),
                                description = p.description,
                                enum = p.enum,
                            )
                        },
                        required = spec.required,
                    ) else null,
                )
            })
        ) else null

        val request = GeminiRequest(
            systemInstruction = GeminiSystemInstruction(listOf(GeminiPart(text = system))),
            contents = contents,
            tools = geminiTools,
            generationConfig = GeminiGenerationConfig(),
        )

        return withContext(Dispatchers.IO) {
            val body = api.streamGenerateContent(model = modelId, apiKey = apiKey, request = request)
            val acc = StringBuilder()
            val toolCalls = mutableListOf<ToolCall>()

            body.charStream().buffered().useLines { lines ->
                for (line in lines) {
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("data:")) continue
                    val payload = trimmed.removePrefix("data:").trim()
                    if (payload.isEmpty() || payload == "[DONE]") continue

                    val chunk = try {
                        json.decodeFromString<GeminiResponse>(payload)
                    } catch (e: Exception) {
                        continue // chunk incompleto/no-JSON: ignorar
                    }

                    val parts = chunk.candidates.firstOrNull()?.content?.parts ?: emptyList()
                    for (part in parts) {
                        part.text?.let { t ->
                            if (t.isNotEmpty()) {
                                acc.append(t)
                                onTextDelta(acc.toString())
                            }
                        }
                        part.functionCall?.let { fc ->
                            toolCalls.add(
                                ToolCall(
                                    id = fc.name,
                                    name = fc.name,
                                    args = fc.args.mapValues { (_, v) ->
                                        if (v is JsonPrimitive) v.content else v.toString().removeSurrounding("\"")
                                    },
                                )
                            )
                        }
                    }
                }
            }

            val stopReason = if (toolCalls.isNotEmpty()) "tool_use" else "end_turn"
            AiTurn(text = acc.toString(), toolCalls = toolCalls, stopReason = stopReason)
        }
    }
}
