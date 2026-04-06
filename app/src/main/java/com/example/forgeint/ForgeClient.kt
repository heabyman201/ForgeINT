package com.example.forgeint

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

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
        payload: Map<String, Any> = emptyMap(),
        imageOptions: RemoteImageOptions? = null
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
            val processedImage = if (isImageResponse && rawBytes != null && imageOptions != null) {
                compressImageForWatch(rawBytes, imageOptions)
            } else {
                null
            }

            ForgeResponse(
                success = response.isSuccessful,
                code = response.code,
                contentType = if (processedImage?.wasCompressed == true) {
                    "image/jpeg"
                } else {
                    contentType
                },
                body = if (isImageResponse || rawBytes == null) {
                    ""
                } else {
                    rawBytes.toString(charset)
                },
                bytes = when {
                    processedImage != null -> processedImage.bytes
                    isImageResponse -> rawBytes
                    else -> null
                },
                imageWidth = processedImage?.width,
                imageHeight = processedImage?.height,
                originalByteCount = if (isImageResponse) rawBytes?.size else null,
                compressedByteCount = when {
                    processedImage != null -> processedImage.bytes.size
                    isImageResponse -> rawBytes?.size
                    else -> null
                },
                imageWasCompressed = processedImage?.wasCompressed ?: false
            )
        }
    }

    suspend fun executeJson(
        command: String,
        payloadJson: String,
        imageOptions: RemoteImageOptions? = null
    ): ForgeResponse {
        val parsedPayload = parsePayloadJson(payloadJson)
        return execute(command = command, payload = parsedPayload, imageOptions = imageOptions)
    }

    private fun parsePayloadJson(payloadJson: String): Map<String, Any> {
        val trimmedPayload = payloadJson.trim()
        if (trimmedPayload.isEmpty()) return emptyMap()

        return gson.fromJson<Map<String, Any>>(
            trimmedPayload,
            object : TypeToken<Map<String, Any>>() {}.type
        ) ?: emptyMap()
    }

    private fun compressImageForWatch(
        rawBytes: ByteArray,
        imageOptions: RemoteImageOptions
    ): ProcessedImage? {
        val normalizedOptions = imageOptions.normalized()
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOptions)

        val sourceWidth = boundsOptions.outWidth
        val sourceHeight = boundsOptions.outHeight
        if (sourceWidth <= 0 || sourceHeight <= 0) return null

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(
                width = sourceWidth,
                height = sourceHeight,
                maxDimension = normalizedOptions.maxDimensionPx
            )
        }

        val decodedBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
            ?: return null

        var scaledBitmap: Bitmap? = null
        return try {
            val maxSide = max(decodedBitmap.width, decodedBitmap.height)
            scaledBitmap = if (maxSide > normalizedOptions.maxDimensionPx) {
                val scale = normalizedOptions.maxDimensionPx.toFloat() / maxSide.toFloat()
                val scaledWidth = (decodedBitmap.width * scale).roundToInt().coerceAtLeast(1)
                val scaledHeight = (decodedBitmap.height * scale).roundToInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(decodedBitmap, scaledWidth, scaledHeight, true)
            } else {
                decodedBitmap
            }

            val outputStream = ByteArrayOutputStream()
            if (!scaledBitmap.compress(Bitmap.CompressFormat.JPEG, normalizedOptions.jpegQuality, outputStream)) {
                return null
            }

            val compressedBytes = outputStream.toByteArray()
            val dimensionsChanged = scaledBitmap.width != sourceWidth || scaledBitmap.height != sourceHeight
            val useCompressedBytes = compressedBytes.isNotEmpty() && (
                compressedBytes.size < rawBytes.size || dimensionsChanged
            )

            ProcessedImage(
                bytes = if (useCompressedBytes) compressedBytes else rawBytes,
                width = scaledBitmap.width,
                height = scaledBitmap.height,
                wasCompressed = useCompressedBytes
            )
        } finally {
            if (scaledBitmap != null && scaledBitmap !== decodedBitmap && !scaledBitmap.isRecycled) {
                scaledBitmap.recycle()
            }
            if (!decodedBitmap.isRecycled) {
                decodedBitmap.recycle()
            }
        }
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        maxDimension: Int
    ): Int {
        var sampleSize = 1
        var sampledWidth = width
        var sampledHeight = height

        while (sampledWidth > maxDimension * 2 || sampledHeight > maxDimension * 2) {
            sampleSize *= 2
            sampledWidth /= 2
            sampledHeight /= 2
        }

        return sampleSize.coerceAtLeast(1)
    }
}

data class ForgeResponse(
    val success: Boolean,
    val code: Int,
    val contentType: String,
    val body: String,
    val bytes: ByteArray? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
    val originalByteCount: Int? = null,
    val compressedByteCount: Int? = null,
    val imageWasCompressed: Boolean = false
)

private data class ProcessedImage(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
    val wasCompressed: Boolean
)
