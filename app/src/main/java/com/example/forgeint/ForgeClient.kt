package com.example.forgeint

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ForgeClient(
    private val host: String = DEFAULT_HOST,
    private val port: Int = DEFAULT_PORT,
    private val token: String = DEFAULT_TOKEN
) {
    companion object {
        const val DEFAULT_HOST = "100.96.101.62"
        const val DEFAULT_PORT = 1235
        const val DEFAULT_TOKEN = "forge-secret-token"

        private val COMMAND_ALIASES = mapOf(
            "sleep_pc" to "sleep",
            "shutdown_pc" to "shutdown",
            "tts" to "text_to_speech",
            "notify" to "show_notification",
            "cmd" to "run_cmd"
        )

        fun normalizeCommand(command: String): String {
            val trimmed = command.trim()
            if (trimmed.isEmpty()) return trimmed
            return COMMAND_ALIASES[trimmed.lowercase()] ?: trimmed
        }

        fun commandHint(command: String): String? {
            val trimmed = command.trim()
            if (trimmed.isEmpty()) return null

            val normalized = normalizeCommand(trimmed)
            if (!trimmed.equals(normalized, ignoreCase = true)) {
                return "\"$trimmed\" will be sent as \"$normalized\"."
            }

            return when (trimmed.lowercase()) {
                "system_info" ->
                    "\"system_info\" is not supported by the current Forge endpoint. Try \"screenshot\", \"text_to_speech\", \"show_notification\", \"run_cmd\", \"sleep\", or \"shutdown\"."
                else -> null
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)  // some commands take time (sysinfo, cmd)
        .build()

    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val baseUrl get() = if (host.startsWith("http")) host else "http://$host:$port"

    /**
     * Send a command to the Forge server.
     * Must be called from a coroutine (suspend function).
     *
     * @param command  The command name (e.g. "sleep", "tts", "screenshot")
     * @param payload  Extra data as a Map (e.g. mapOf("text" to "hello"))
     * @return         The raw response body as a String, or the PNG bytes for screenshot
     */
    suspend fun execute(
        command: String,
        payload: Map<String, Any> = emptyMap()
    ): ForgeResponse = withContext(Dispatchers.IO) {
        val normalizedCommand = normalizeCommand(command)
        val body = mutableMapOf<String, Any>("command" to normalizedCommand)
        body.putAll(payload)

        val json = gson.toJson(body)
        val requestBody = json.toRequestBody(JSON)

        val request = Request.Builder()
            .url("$baseUrl/action")
            .header("X-Token", token)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val rawBytes = response.body?.bytes()
            val contentType = response.header("Content-Type") ?: ""
            val charset = response.body?.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
            val isImageResponse = contentType.contains("image", ignoreCase = true)

            ForgeResponse(
                success = response.isSuccessful,
                code = response.code,
                contentType = contentType,
                body = if (isImageResponse || rawBytes == null) {
                    ""
                } else {
                    rawBytes.toString(charset)
                },
                bytes = if (isImageResponse) rawBytes else null
            )
        }
    }

    suspend fun executeJson(
        command: String,
        payloadJson: String
    ): ForgeResponse {
        val parsedPayload = parsePayloadJson(payloadJson)
        return execute(command = command, payload = parsedPayload)
    }

    private fun parsePayloadJson(payloadJson: String): Map<String, Any> {
        val trimmedPayload = payloadJson.trim()
        if (trimmedPayload.isEmpty()) return emptyMap()

        return gson.fromJson<Map<String, Any>>(
            trimmedPayload,
            object : TypeToken<Map<String, Any>>() {}.type
        ) ?: emptyMap()
    }
}

data class ForgeResponse(
    val success: Boolean,
    val code: Int,
    val contentType: String,
    val body: String,
    val bytes: ByteArray? = null   // for screenshot PNG
)
