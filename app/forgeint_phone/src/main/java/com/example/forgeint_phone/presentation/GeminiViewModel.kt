package com.example.forgeint.presentation

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.forgeint.BuildConfig
import com.example.forgeint.data.PersonaRepository
import com.example.forgeint.data.SettingsManager
import com.example.forgeint.data.SettingsStore
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.BufferedSink
import okio.GzipSink
import okio.GzipSource
import okio.buffer
import java.util.concurrent.TimeUnit
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import com.example.forgeint.data.ChatDatabase
import com.example.forgeint.data.Conversation
import com.example.forgeint.data.Message
import com.example.forgeint.data.MessageEmbedding
import com.example.forgeint.data.UserTrait
import com.example.forgeint.domain.LocalInferenceEngine
import com.example.forgeint.domain.Personas
import com.example.forgeint.data.MemoryPrompter
import org.json.JSONObject

enum class LocalModelStatus {
    NotPresent,
    Downloading,
    Present
}

class GeminiViewModel(
    application: Application,
    private val settingsManager: SettingsStore,
    private val customPersonaRepository: PersonaRepository<com.example.forgeint.domain.Persona>
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "GeminiViewModel"
        private const val STREAM_UI_FRAME_MS = 50L
        @Volatile private var nativeParserAvailable = false
        @Volatile private var nativeNumericOpsAvailable = false
    }

    constructor(application: Application) : this(
        application = application,
        settingsManager = SettingsManager(application),
        customPersonaRepository = com.example.forgeint.data.CustomPersonaRepository(application)
    )

    init {
        val nativeLoaded = try {
            System.loadLibrary("forgeint_local")
            true
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.e(TAG, "Failed to load forgeint_local; using Kotlin fallbacks", e)
            false
        } catch (e: Throwable) {
            android.util.Log.e(TAG, "Unexpected native load failure; using Kotlin fallbacks", e)
            false
        }
        nativeParserAvailable = nativeLoaded
        nativeNumericOpsAvailable = nativeLoaded
    }

    external fun parseStreamChunkNative(rawData: String): String?
    external fun normalizeVectorNative(raw: FloatArray): FloatArray
    external fun cosineSimilarityNative(a: FloatArray, b: FloatArray): Double
    external fun fallbackHashEmbeddingNative(text: String, dim: Int): FloatArray
    external fun scoreMessageVectorsNative(queryVector: FloatArray, vectorBlobs: Array<ByteArray>): DoubleArray

    private fun parseStreamChunkSafe(rawData: String): String? {
        if (nativeParserAvailable) {
            try {
                return parseStreamChunkNative(rawData)
            } catch (e: UnsatisfiedLinkError) {
                nativeParserAvailable = false
                android.util.Log.e(TAG, "Native parser unavailable; using Kotlin fallback", e)
            } catch (e: Throwable) {
                nativeParserAvailable = false
                android.util.Log.e(TAG, "Native parser failed; using Kotlin fallback", e)
            }
        }

        if (!rawData.startsWith("data: ") || rawData.contains("[DONE]")) return null
        val jsonPart = rawData.removePrefix("data: ").trim()
        if (jsonPart.isBlank()) return null

        return try {
            val json = JSONObject(jsonPart)
            val choices = json.optJSONArray("choices")
            val choice = choices?.optJSONObject(0)
            val delta = choice?.optJSONObject("delta")
            val content = delta?.optString("content") ?: choice?.optJSONObject("message")?.optString("content")
            content?.replace(Regex("nn\\d+"), "")
        } catch (_: Exception) {
            null
        }
    }

    private fun normalizeVectorSafe(raw: FloatArray): FloatArray {
        if (raw.isEmpty()) return raw
        if (nativeNumericOpsAvailable) {
            try {
                return normalizeVectorNative(raw)
            } catch (e: UnsatisfiedLinkError) {
                nativeNumericOpsAvailable = false
                android.util.Log.e(TAG, "normalizeVectorNative unavailable; using Kotlin fallback", e)
            } catch (e: Throwable) {
                nativeNumericOpsAvailable = false
                android.util.Log.e(TAG, "normalizeVectorNative failed; using Kotlin fallback", e)
            }
        }
        return normalizeVectorJvm(raw)
    }

    private fun cosineSimilaritySafe(a: FloatArray, b: FloatArray): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (nativeNumericOpsAvailable) {
            try {
                return cosineSimilarityNative(a, b)
            } catch (e: UnsatisfiedLinkError) {
                nativeNumericOpsAvailable = false
                android.util.Log.e(TAG, "cosineSimilarityNative unavailable; using Kotlin fallback", e)
            } catch (e: Throwable) {
                nativeNumericOpsAvailable = false
                android.util.Log.e(TAG, "cosineSimilarityNative failed; using Kotlin fallback", e)
            }
        }
        return cosineSimilarityJvm(a, b)
    }

    private fun fallbackHashEmbeddingSafe(text: String, dim: Int): FloatArray {
        if (dim <= 0) return FloatArray(0)
        if (nativeNumericOpsAvailable) {
            try {
                return fallbackHashEmbeddingNative(text, dim)
            } catch (e: UnsatisfiedLinkError) {
                nativeNumericOpsAvailable = false
                android.util.Log.e(TAG, "fallbackHashEmbeddingNative unavailable; using Kotlin fallback", e)
            } catch (e: Throwable) {
                nativeNumericOpsAvailable = false
                android.util.Log.e(TAG, "fallbackHashEmbeddingNative failed; using Kotlin fallback", e)
            }
        }
        return fallbackHashEmbeddingJvm(text, dim)
    }

    private fun scoreMessageVectorsSafe(queryVector: FloatArray, vectorBlobs: Array<ByteArray>): DoubleArray {
        if (queryVector.isEmpty() || vectorBlobs.isEmpty()) return DoubleArray(0)
        if (nativeNumericOpsAvailable) {
            try {
                return scoreMessageVectorsNative(queryVector, vectorBlobs)
            } catch (e: UnsatisfiedLinkError) {
                nativeNumericOpsAvailable = false
                android.util.Log.e(TAG, "scoreMessageVectorsNative unavailable; using Kotlin fallback", e)
            } catch (e: Throwable) {
                nativeNumericOpsAvailable = false
                android.util.Log.e(TAG, "scoreMessageVectorsNative failed; using Kotlin fallback", e)
            }
        }
        return DoubleArray(vectorBlobs.size) { idx ->
            cosineSimilaritySafe(queryVector, decodeVectorJvm(vectorBlobs[idx]))
        }
    }

    private val dao = ChatDatabase.getDatabase(application).chatDao()
    private val traitDao = ChatDatabase.getDatabase(application)

    private val localEngine = LocalInferenceEngine(application)
    private var isModelLoaded = false

    private val embeddingModelId = "text-embedding-qwen3-embedding-0.6b"
    private val fallbackEmbeddingDim = 256
    private var pendingAttachmentsForNextSend: List<ChatAttachment> = emptyList()
    private var activeResponseJob: Job? = null
    private var stopRequested = false

    private val _streamingMessage = MutableStateFlow<String?>(null)
    val streamingMessage = _streamingMessage.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking = _isThinking.asStateFlow()

    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels = _availableModels.asStateFlow()

    private val _customPersonas = MutableStateFlow(customPersonaRepository.getCustomPersonas())

    private fun refreshCustomPersonas() {
        _customPersonas.value = customPersonaRepository.getCustomPersonas()
    }

    private fun gzipRequestBody(body: RequestBody): RequestBody = object : RequestBody() {
        override fun contentType() = body.contentType()

        override fun contentLength(): Long = -1

        override fun writeTo(sink: BufferedSink) {
            val gzipSink = GzipSink(sink).buffer()
            body.writeTo(gzipSink)
            gzipSink.close()
        }
    }
    
    val allPersonas = _customPersonas.map { custom ->
        Personas.list + custom
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Personas.list)

    fun createPersona(name: String, description: String, prompt: String) {
        val newPersona = com.example.forgeint.domain.Persona(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            description = description,
            systemInstruction = prompt
        )
        customPersonaRepository.addCustomPersona(newPersona)
        refreshCustomPersonas()
    }

    fun deletePersona(id: String) {
        customPersonaRepository.deleteCustomPersona(id)
        refreshCustomPersonas()
        // If the deleted persona was selected, revert to default
        if (selectedPersonaId.value == id) {
             setPersona("default")
        }
    }

    fun fetchAvailableModels() {
        viewModelScope.launch(Dispatchers.IO) {
            val host = currentHostIp.value
            val port = currentPort.value
            val cleanHost = host.removePrefix("https://").removePrefix("http://").trim('/')
            val isTunnel = isTunnelHost(cleanHost) || isFunnelEnabled.value

            val urlString = if (isTunnel) {
                "https://$cleanHost/v1/models"
            } else {
                "http://$cleanHost:$port/v1/models"
            }

            try {
                val requestBuilder = Request.Builder().url(urlString)
                buildLocalHeaders(cleanHost, isTunnel).forEach { (k, v) -> requestBuilder.addHeader(k, v) }
                val response = okHttpClient.newCall(requestBuilder.build()).execute()
                val bodyStr = response.body?.string()

                if (response.isSuccessful && bodyStr != null) {
                    val json = JSONObject(bodyStr)
                    val data = json.optJSONArray("data")
                    val models = mutableListOf<String>()
                    if (data != null) {
                        for (i in 0 until data.length()) {
                            val item = data.getJSONObject(i)
                            val id = item.optString("id")
                            if (id.isNotEmpty()) models.add(id)
                        }
                    }
                    if (models.isNotEmpty()) {
                        _availableModels.value = models
                    }
                }
                response.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val originalBody = originalRequest.body
            val requestBuilder = originalRequest.newBuilder()
                .header("Accept-Encoding", "gzip")

            if (originalBody != null && originalRequest.header("Content-Encoding") == null) {
                requestBuilder
                    .header("Content-Encoding", "gzip")
                    .method(originalRequest.method, gzipRequestBody(originalBody))
            }

            val request = requestBuilder.build()

            val response = chain.proceed(request)
            val body = response.body

            if (response.header("Content-Encoding") == "gzip" && body != null) {
                val gzipSource = GzipSource(body.source())
                val responseBody = gzipSource.buffer().asResponseBody(body.contentType(), -1L)

                response.newBuilder()
                    .removeHeader("Content-Encoding")
                    .removeHeader("Content-Length")
                    .body(responseBody)
                    .build()
            } else {
                response
            }
        }
        .build()

    private val downloadHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.MINUTES)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://openrouter.ai/api/v1/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(GeminiApiService::class.java)

    val currentModelId = settingsManager.selectedModel
        .stateIn(viewModelScope, SharingStarted.Eagerly, "google/gemma-2-9b-it:free")
val memory_monitor = settingsManager.isMemoryMonitorEnabled
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val thermal_monitor = settingsManager.isSystemTelemetryEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    var currentHostIp = settingsManager.hostIp
        .stateIn(viewModelScope, SharingStarted.Eagerly, "https://covers-headers-inclusion-footage.trycloudflare.com")

    val currentPort = settingsManager.serverPort
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(4500), "1234")

    val isLocalEnabled = settingsManager.isLocalEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isFunnelEnabled = settingsManager.isFunnelEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val localAuthToken = settingsManager.localAuthToken
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsManager.DEFAULT_LOCAL_AUTH_TOKEN)

    val selectedPersonaId: StateFlow<String> = settingsManager.selectedPersonaId
        .stateIn(viewModelScope, SharingStarted.Eagerly, "default")

    val appTheme = settingsManager.appTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Default")

    val isLiteMode = settingsManager.isLiteMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(4500), true)

    val isVoiceDominantMode = settingsManager.isVoiceDominantMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(4500), false)

    val apiKey = settingsManager.apiKey
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val isCustomApiKeyEnabled = settingsManager.isCustomApiKeyEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val messageLength = settingsManager.messageLength
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(4500), "Normal")

    val history = dao.getConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentConversationId = MutableStateFlow<Long?>(null)
    val currentConversationId = _currentConversationId.asStateFlow()

    private var _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    private val _loadingBurst = MutableStateFlow(false)
    val loadingBurst = _loadingBurst.asStateFlow()
    private var loadingBurstJob: Job? = null

    private val _isTesting = MutableStateFlow(false)
    val isTesting = _isTesting.asStateFlow()

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult = _testResult.asStateFlow()
    private val _pendingAction = MutableStateFlow<String?>(null)
    val pendingAction = _pendingAction.asStateFlow()

    private val _messageLimit = MutableStateFlow(20)

    private val _isHistoryLoading = MutableStateFlow(false)
    val isHistoryLoading = _isHistoryLoading.asStateFlow()

    private val _localModelStatus = MutableStateFlow<LocalModelStatus>(LocalModelStatus.NotPresent)
    val localModelStatus = _localModelStatus.asStateFlow()

    private val _modelDownloadProgress = MutableStateFlow(0f)
    val modelDownloadProgress = _modelDownloadProgress.asStateFlow()

    private val localModelFile = File(application.filesDir, "gemma-2-2b-it-IQ2_XXS.gguf")
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
fun toggleMemoryMonitor() {
    viewModelScope.launch {
        settingsManager.setMemoryMonitorEnabled(!memory_monitor.value)
    }
}

    fun setPendingAction(action: String) {
        _pendingAction.value = action
    }

    fun clearPendingAction() {
        _pendingAction.value = null
    }
    fun toggleThermalMonitor() {
        viewModelScope.launch {
            settingsManager.setSystemTelemetryEnabled(!thermal_monitor.value)
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults: StateFlow<List<Conversation>> = _searchQuery
        .debounce(300L)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                dao.getConversations()
            } else {

                dao.searchConversations("$query*")
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
    init {
        checkLocalModelStatus()
        checkForEmulator()
        startMemoryMaintenanceLoop()
        observeLocalConnectivityConfig()
    }

    private fun checkForEmulator() {
        val buildModel = android.os.Build.MODEL.lowercase()
        val buildProduct = android.os.Build.PRODUCT.lowercase()
        val buildHardware = android.os.Build.HARDWARE.lowercase()
        val buildFingerprint = android.os.Build.FINGERPRINT.lowercase()

        val isEmulator = (buildFingerprint.startsWith("generic")
                || buildFingerprint.startsWith("unknown")
                || buildModel.contains("google_sdk")
                || buildModel.contains("emulator")
                || buildModel.contains("android sdk built for x86")
                || buildModel.contains("sdk_gphone")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || "google_sdk" == android.os.Build.PRODUCT
                || buildProduct.contains("sdk")
                || buildProduct.contains("emulator")
                || buildHardware.contains("goldfish")
                || buildHardware.contains("ranchu"))

        if (isEmulator) {
            viewModelScope.launch {
                val currentIp = settingsManager.hostIp.first()
                if (currentIp == "192.168.1.1" || currentIp.contains("trycloudflare") || currentIp.isBlank()) {
                    settingsManager.setHostIp("http://10.0.2.2")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadingBurstJob?.cancel()
        activeResponseJob?.cancel()
        if (isModelLoaded) {
            localEngine.unloadModel()
            isModelLoaded = false
        }
    }

    private fun observeLocalConnectivityConfig() {
        viewModelScope.launch {
            settingsManager.localConnectivitySettings
                .debounce(850)
                .collect { cfg ->
                    if (cfg.isLocalEnabled) autoConnectLocalServer(silent = true)
                }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        _isLoading.value = isLoading
        if (!isLoading) {
            loadingBurstJob?.cancel()
            loadingBurstJob = null
            _loadingBurst.value = false
            return
        }

        loadingBurstJob?.cancel()
        loadingBurstJob = viewModelScope.launch {
            delay(180)
            if (!_isLoading.value) return@launch
            _loadingBurst.value = true
            delay(450)
            _loadingBurst.value = false
        }
    }

    private fun checkLocalModelStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            if (localModelFile.exists()) {
                _localModelStatus.value = LocalModelStatus.Present
            } else {
                _localModelStatus.value = LocalModelStatus.NotPresent
            }
        }
    }

    fun downloadLocalModel() {
        if (_localModelStatus.value == LocalModelStatus.Downloading) return

        viewModelScope.launch(Dispatchers.IO) {
            _localModelStatus.value = LocalModelStatus.Downloading
            _modelDownloadProgress.value = 0f


            val modelUrl = "https://huggingface.co/duyntnet/gemma-2-2b-it-imatrix-GGUF/resolve/main/gemma-2-2b-it-IQ2_XXS.gguf?download=true"
            val tempFile = File(localModelFile.path + ".part")

            try {
                val request = Request.Builder()
                    .url(modelUrl)
                    .header("Authorization", "Bearer ${requireHuggingFaceToken()}")
                    .build()

                val response = downloadHttpClient.newCall(request).execute()

                if (response.code == 401 || response.code == 403) {
                    throw IOException("Unauthorized. Check HF Token and License Agreement.")
                }
                if (!response.isSuccessful) throw IOException("Failed: ${response.code} ${response.message}")

                val body = response.body ?: throw IOException("Body is null")
                val contentLength = body.contentLength()

                body.source().use { source ->
                    FileOutputStream(tempFile).use { outputStream ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesCopied: Long = 0
                        var bytesRead: Int

                        while (source.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            bytesCopied += bytesRead

                            if (contentLength > 0) {
                                _modelDownloadProgress.value = (bytesCopied.toFloat() / contentLength).coerceIn(0f, 1f)
                            } else {
                                _modelDownloadProgress.value = -1f
                            }
                        }
                    }
                }

                if (tempFile.length() < 100 * 1024 * 1024) {
                    throw IOException("File too small. Likely an error page.")
                }

                if (localModelFile.exists()) {
                    localModelFile.delete()
                }

                if (!tempFile.renameTo(localModelFile)) {
                    throw IOException("Failed to move file to final destination.")
                }

                _localModelStatus.value = LocalModelStatus.Present

            } catch (e: Exception) {
                e.printStackTrace()
                _localModelStatus.value = LocalModelStatus.NotPresent
                tempFile.delete()
            } finally {
                _modelDownloadProgress.value = 0f
            }
        }
    }

    fun deleteLocalModel() {
        viewModelScope.launch(Dispatchers.IO) {
            if (isModelLoaded) {
                localEngine.unloadModel()
                isModelLoaded = false
            }
            if (localModelFile.exists()) {
                localModelFile.delete()
            }
            _localModelStatus.value = LocalModelStatus.NotPresent
        }
    }

    val currentChat = _currentConversationId.flatMapLatest { id ->
        if (id == null) flowOf(null) else dao.getConversationWithMessages(id)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val activeMessages = _currentConversationId.flatMapLatest { id ->
        if (id == null) {
            flowOf(emptyList())
        } else {
            _messageLimit.flatMapLatest { limit ->
                dao.getMessages(id, limit)
                    .map { list -> list.reversed() }
                    .onEach { _isHistoryLoading.value = false }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadNextPage() {
        if (_isHistoryLoading.value) return
        _isHistoryLoading.value = true
        _messageLimit.value += 20
    }

    fun setModel(modelId: String) = viewModelScope.launch { settingsManager.setModel(modelId) }
    fun setPersona(personaId: String) = viewModelScope.launch { settingsManager.setSelectedPersona(personaId) }
    fun setTheme(theme: String) = viewModelScope.launch { settingsManager.setAppTheme(theme) }
    fun prepareForNewChat() {
        _currentConversationId.value = null
    }

    fun setLiteMode(enabled: Boolean) = viewModelScope.launch { settingsManager.setLiteMode(enabled) }
    fun setVoiceDominantMode(enabled: Boolean) = viewModelScope.launch { settingsManager.setVoiceDominantMode(enabled) }
    fun toggleLocalMode(enabled: Boolean) = viewModelScope.launch { settingsManager.setLocalEnabled(enabled) }
    fun setMessageLength(length: String) = viewModelScope.launch { settingsManager.setMessageLength(length) }
    fun updateApiKey(key: String) = viewModelScope.launch { settingsManager.setApiKey(key) }
    fun toggleCustomApiKey(enabled: Boolean) = viewModelScope.launch { settingsManager.setCustomApiKeyEnabled(enabled) }
    fun setFunnelEnabled(enabled: Boolean) = viewModelScope.launch { settingsManager.setFunnelEnabled(enabled) }
    fun updateLocalAuthToken(token: String) = viewModelScope.launch { settingsManager.setLocalAuthToken(token) }

    fun updateHostIp(newIp: String) = viewModelScope.launch {
        val cleanHost = newIp
            .trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .trim('/')

        if (cleanHost.isBlank()) return@launch
        settingsManager.setHostIp(cleanHost)
        _testResult.value = null
    }

    fun updatePort(newPort: String) = viewModelScope.launch {
        settingsManager.setServerPort(newPort)
        _testResult.value = null
    }
    val allTraits = traitDao.traitDao().getAllTraitsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteTrait(traitKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            traitDao.traitDao().deleteTrait(traitKey)
        }
    }

    fun clearAllTraits() {
        viewModelScope.launch(Dispatchers.IO) {
            traitDao.traitDao().deleteAllTraits()
        }
    }

    fun addManualMemory(content: String, type: String) {
        val trimmed = content.trim()
        if (trimmed.length < 3) return
        val category = if (type.equals("long_term", ignoreCase = true)) "LONG_TERM" else "SHORT_TERM"
        val prefix = if (category == "LONG_TERM") "MANUAL_LT" else "MANUAL_ST"
        val normalizedKey = "${prefix}_${trimmed.uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_').take(32)}"
        viewModelScope.launch(Dispatchers.IO) {
            traitDao.traitDao().updateTrait(UserTrait(normalizedKey, trimmed.take(180), category))
        }
    }

    fun startNewChat(prompt: String, attachments: List<ChatAttachment> = emptyList()) {
        if (_isLoading.value) return

        activeResponseJob = viewModelScope.launch {
            stopRequested = false
            _messageLimit.value = 20
            pendingAttachmentsForNextSend = attachments
            val normalizedPrompt = normalizePromptForSend(prompt, attachments)
            val storedPrompt = buildStoredPrompt(normalizedPrompt, attachments)
            val newId = dao.insertConversation(Conversation(summary = normalizedPrompt))
            _currentConversationId.value = newId
            val userMessageId = dao.insertMessage(Message(conversationId = newId, text = storedPrompt, isUser = true))
            queueEmbeddingForMessage(userMessageId, storedPrompt)
            generateResponse(newId)

            if (!isLocalEnabled.value || !localModelFile.exists()) {
                launch(Dispatchers.IO) { extractAndStoreTraits(normalizedPrompt) }
            }
        }
    }

    fun continueChat(prompt: String, attachments: List<ChatAttachment> = emptyList()) {
        if (_isLoading.value) return

        val chatId = _currentConversationId.value ?: return
        activeResponseJob = viewModelScope.launch {
            stopRequested = false
            pendingAttachmentsForNextSend = attachments
            val normalizedPrompt = normalizePromptForSend(prompt, attachments)
            val storedPrompt = buildStoredPrompt(normalizedPrompt, attachments)
            val userMessageId = dao.insertMessage(Message(conversationId = chatId, text = storedPrompt, isUser = true))
            queueEmbeddingForMessage(userMessageId, storedPrompt)

            if (!isLocalEnabled.value || !localModelFile.exists()) {
                launch(Dispatchers.IO) { extractAndStoreTraits(normalizedPrompt) }
            }

            generateResponse(chatId)
        }
    }

    fun stopResponse() {
        stopRequested = true
        activeResponseJob?.cancel(CancellationException("Stopped by user"))
        _isThinking.value = false
        _streamingMessage.value = null
        setLoadingState(false)
    }

    fun openChat(id: Long) {
        _messageLimit.value = 20
        _currentConversationId.value = id
    }

    fun deleteChat(id: Long) {
        viewModelScope.launch {
            dao.deleteConversationById(id)
            if (_currentConversationId.value == id) {
                _currentConversationId.value = null
            }
        }
    }

    private suspend fun extractAndStoreTraits(userText: String) {
        if (!memory_monitor.value) return
        if (userText.length < 10) return

        val explicitMemory = extractExplicitStructuredMemoryInstruction(userText)
        if (explicitMemory != null) {
            val (key, value, category) = explicitMemory
            if (isUsefulStructuredMemory(key, value, category)) {
                traitDao.traitDao().updateTrait(UserTrait(key, value, category))
                android.util.Log.d("MemoryDebug", "MEMORY SAVED (explicit): $key - $value")
            } else {
                android.util.Log.d("MemoryDebug", "Rejected explicit structured memory: $key|$value|$category")
            }
            return
        }

        val modelToUse = currentModelId.value
        val prompt = MemoryPrompter.extractionPrompt + "\n\nUser Message: \"$userText\""

        val apiMessages = listOf(
            ApiMessage("system", "You are a background memory analyzer."),
            ApiMessage("user", prompt)
        )

        try {
            val request = ChatRequest(model = modelToUse, messages = apiMessages, stream = false, max_tokens = 256)

            val responseText: String? = (if (isLocalEnabled.value && !localModelFile.exists()) {
                val host = currentHostIp.value
                val port = currentPort.value
                val cleanHost = host.removePrefix("https://").removePrefix("http://").trim('/')
                val isTunnel = isTunnelHost(cleanHost) || isFunnelEnabled.value

                val urlString = if (isTunnel) {
                    "https://$cleanHost/v1/chat/completions"
                } else {
                    "http://$cleanHost:$port/v1/chat/completions"
                }

                val headers = buildLocalHeaders(cleanHost, isTunnel)

                val response = apiService.chatLocalBlocking(urlString, headers, request)
                response.choices.firstOrNull()?.message?.content
            } else if (!isLocalEnabled.value) {
                val response = apiService.chatOpenRouterBlocking(
                    "Bearer ${resolveOpenRouterKey()}",
                    "https://forgeint.app",
                    "ForgeInt",
                    request
                )
                response.choices.firstOrNull()?.message?.content
            } else {
                null
            }) as String?

            responseText?.let { text ->
                if (text.contains("|") && !text.contains("NULL")) {
                    val parts = text.trim().split("|")
                    if (parts.size >= 3) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        val category = parts[2].trim()

                        if (!isUsefulStructuredMemory(key, value, category)) {
                            android.util.Log.d("MemoryDebug", "Skipping low-quality memory: $key|$value|$category")
                            return@let
                        }

                        traitDao.traitDao().updateTrait(UserTrait(key, value, category))
                        println("MEMORY SAVED: $key - $value")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isUsefulStructuredMemory(key: String, value: String, category: String): Boolean {
        val normalizedKey = key.trim().uppercase()
        val normalizedValue = value.trim().lowercase()
        val normalizedCategory = category.trim().uppercase()

        if (normalizedKey.length !in 3..48) return false
        if (!Regex("^[A-Z0-9_]+$").matches(normalizedKey)) return false
        if (normalizedValue.length !in 3..180) return false
        if (normalizedCategory.isBlank()) return false

        val bannedKeys = setOf("USER", "USER_INFO", "MESSAGE", "FACT", "CONTEXT", "TOPIC")
        if (normalizedKey in bannedKeys) return false

        val genericNoise = listOf(
            "user is talking",
            "user is speaking",
            "user is discussing",
            "user asked",
            "used the word",
            "used word",
            "save this to long term",
            "save it to long term",
            "something",
            "anything",
            "random topic",
            "conversation"
        )
        if (genericNoise.any { normalizedValue.contains(it) }) return false

        if (Regex("\\b(save|remember|store|long term|short term|memory)\\b").containsMatchIn(normalizedValue)) return false

        val meaningfulKeywords = Regex(
            "\\b(name|called|live|from|work|job|student|study|age|birthday|allergic|prefer|like|dislike|love|hate|use|using|owns|has|currently|today|tomorrow|feeling|mood|working on|building|debugging|learning)\\b"
        )
        val hasDigits = normalizedValue.any { it.isDigit() }
        val enoughWords = normalizedValue.split(" ").size >= 4

        return meaningfulKeywords.containsMatchIn(normalizedValue) || hasDigits || enoughWords
    }

    private fun extractExplicitStructuredMemoryInstruction(userText: String): Triple<String, String, String>? {
        val trimmed = userText.trim()
        if (trimmed.length < 12) return null

        val pattern = Regex(
            "(?i)\\b(?:save|remember|store)\\b\\s+(.{3,180}?)\\s+(?:as|to|in)\\s+(long\\s*term|short\\s*term)\\b"
        )
        val match = pattern.find(trimmed) ?: return null
        val rawContent = match.groupValues[1]
            .trim()
            .removePrefix("that ")
            .removePrefix("this ")
            .trim()
        if (rawContent.equals("it", ignoreCase = true)) return null

        val typePart = match.groupValues[2]
        val key = if (typePart.contains("long", ignoreCase = true)) "USER_FACT_EXPLICIT" else "USER_CONTEXT_EXPLICIT"
        val category = if (typePart.contains("long", ignoreCase = true)) "LONG_TERM" else "SHORT_TERM"
        return Triple(key, rawContent, category)
    }

    private var lastMemoryMaintenanceAt: Long = 0L
    private val shortTermExpiryMs = 12 * 60 * 60 * 1000L
    private val memoryMaintenanceIntervalMs = 30 * 60 * 1000L

    private fun startMemoryMaintenanceLoop() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                runMemoryMaintenanceIfNeeded(force = false)
                delay(15 * 60 * 1000L)
            }
        }
    }

    private suspend fun runMemoryMaintenanceIfNeeded(force: Boolean) {
        if (!memory_monitor.value) return
        val now = System.currentTimeMillis()
        if (!force && now - lastMemoryMaintenanceAt < memoryMaintenanceIntervalMs) return
        lastMemoryMaintenanceAt = now

        val all = traitDao.traitDao().getAllTraits()
        val shortTerm = all.filter { it.category.equals("SHORT_TERM", ignoreCase = true) }
        if (shortTerm.size <= 15) return

        val staleCutoff = now - shortTermExpiryMs
        val stale = shortTerm.filter { it.traitKey.startsWith("USER_CONTEXT") || it.traitKey.startsWith("MANUAL_ST") }
        val toDelete = stale.sortedBy { it.traitKey.hashCode() }.take((shortTerm.size - 15).coerceAtLeast(0))
        toDelete.forEach { traitDao.traitDao().deleteTrait(it.traitKey) }

        if (toDelete.isEmpty() && staleCutoff > 0) {
            shortTerm.take((shortTerm.size - 15).coerceAtLeast(0)).forEach {
                traitDao.traitDao().deleteTrait(it.traitKey)
            }
        }
    }

    fun toggleBookmark(id: Long, currentStatus: Boolean) {
        viewModelScope.launch {
            dao.toggleBookmark(id, !currentStatus)
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _isTesting.value = true
            _testResult.value = "Checking connection..."
            val host = currentHostIp.value
            val cleanHost = host.removePrefix("https://").removePrefix("http://").trim('/')

            val isTunnel = isTunnelHost(cleanHost) || isFunnelEnabled.value
            val finalPort = currentPort.value
            _testResult.value = if (isTunnel) "Testing funnel/tunnel endpoint..." else "Testing local endpoint..."

            val urlString = if (isTunnel) {
                "https://$cleanHost/v1/models"
            } else {
                "http://$cleanHost:$finalPort/v1/models"
            }

            try {
                val result = withContext(Dispatchers.IO) {
                    try {
                        val requestBuilder = Request.Builder().url(urlString)
                        buildLocalHeaders(cleanHost, isTunnel).forEach { (k, v) -> requestBuilder.addHeader(k, v) }
                        val response = okHttpClient.newCall(requestBuilder.build()).execute()
                        val code = response.code
                        response.close()
                        if (code == 200) {
                            fetchAvailableModels()
                            "Success! Connected to $cleanHost"
                        } else {
                            "Connected, but Server Error $code"
                        }
                    } catch (e: Exception) {
                        "Failed to connect: ${e.message}"
                    }
                }
                _testResult.value = result
            } catch (e: Exception) {
                _testResult.value = "Error: ${e.message}"
            } finally {
                _isTesting.value = false
            }
        }
    }

    private suspend fun autoConnectLocalServer(silent: Boolean) {
        val host = currentHostIp.value
        val cleanHost = host.removePrefix("https://").removePrefix("http://").trim('/')
        if (cleanHost.isBlank()) return

        val port = currentPort.value
        val isTunnel = isTunnelHost(cleanHost) || isFunnelEnabled.value
        val urlString = if (isTunnel) {
            "https://$cleanHost/v1/models"
        } else {
            "http://$cleanHost:$port/v1/models"
        }

        withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder().url(urlString)
                buildLocalHeaders(cleanHost, isTunnel).forEach { (k, v) -> requestBuilder.addHeader(k, v) }
                val response = okHttpClient.newCall(requestBuilder.build()).execute()
                val bodyStr = response.body?.string()

                if (response.isSuccessful && !bodyStr.isNullOrBlank()) {
                    val json = JSONObject(bodyStr)
                    val data = json.optJSONArray("data")
                    val models = mutableListOf<String>()
                    if (data != null) {
                        for (i in 0 until data.length()) {
                            val id = data.optJSONObject(i)?.optString("id").orEmpty()
                            if (id.isNotBlank()) models.add(id)
                        }
                    }
                    if (models.isNotEmpty()) {
                        _availableModels.value = models
                        if (!silent) _testResult.value = "Success! Connected to $cleanHost"
                    } else if (!silent) {
                        _testResult.value = "Connected, but no models exposed."
                    }
                } else if (!silent) {
                    _testResult.value = "Connected, but Server Error ${response.code}"
                }
                response.close()
            } catch (e: Exception) {
                if (!silent) _testResult.value = "Failed to connect: ${e.message}"
            }
        }
    }

    private fun formatGemmaPrompt(systemPrompt: String, history: List<Message>): String {
        val sb = StringBuilder()
        sb.append("<start_of_turn>system\n$systemPrompt<end_of_turn>\n")

        val safeHistory = history.takeLast(6)
        safeHistory.forEach { msg ->
            val role = if (msg.isUser) "user" else "model"
            sb.append("<start_of_turn>$role\n${msg.text}<end_of_turn>\n")
        }

        sb.append("<start_of_turn>model\n")
        return sb.toString()
    }

    private fun buildStoredPrompt(prompt: String, attachments: List<ChatAttachment>): String {
        if (attachments.isEmpty()) return prompt
        val attachmentSummary = buildString {
            attachments.forEach { attachment ->
                appendLine()
                append("[Attachment: ${attachment.label} | ${attachment.mimeType}]")
                attachment.textContent
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        appendLine()
                        append(it.take(6000))
                    }
            }
        }
        return (prompt + attachmentSummary).trim()
    }

    private fun normalizePromptForSend(prompt: String, attachments: List<ChatAttachment>): String {
        val trimmed = prompt.trim()
        return if (trimmed.isNotBlank()) {
            trimmed
        } else if (attachments.isNotEmpty()) {
            "Please analyze the attached content."
        } else {
            trimmed
        }
    }

    private fun buildLatestUserApiContent(prompt: String, attachments: List<ChatAttachment>): Any {
        val imageParts = attachments
            .mapNotNull { attachment ->
                attachment.imageDataUrl?.let { dataUrl ->
                    ContentImagePart(image_url = ImageUrlPayload(dataUrl))
                }
            }

        if (imageParts.isEmpty()) return prompt

        return buildList<Any> {
            add(ContentTextPart(text = prompt))
            addAll(imageParts)
        }
    }

    private suspend fun buildRelevantConversationContext(currentChatId: Long, latestUserPrompt: String): String {
        if (latestUserPrompt.isBlank()) return ""

        val recentConversations = dao.getRecentConversationsForMemory(
            excludeConversationId = currentChatId,
            limit = 40
        )
        if (recentConversations.isEmpty()) return ""

        val messagesByConversation = recentConversations.associate { conversation ->
            conversation.id to dao.getRecentMessagesForConversation(conversation.id, limit = 12)
        }
        val vectorScores = buildConversationVectorScores(latestUserPrompt, messagesByConversation)

        val ranked = MemoryRetrievalEngine.rankConversations(
            query = latestUserPrompt,
            conversations = recentConversations,
            messagesByConversation = messagesByConversation,
            vectorScoresByConversation = vectorScores,
            maxResults = 7
        )
        return MemoryRetrievalEngine.buildPromptContext(ranked)
    }

    private suspend fun buildConversationVectorScores(
        query: String,
        messagesByConversation: Map<Long, List<Message>>
    ): Map<Long, Double> {
        val conversationIds = messagesByConversation.keys.toList()
        val queryVector = generateEmbeddingVector(query) ?: return conversationIds.associateWith { 0.0 }
        val allMessages = messagesByConversation.values.flatten().filter { it.id > 0L }
        if (allMessages.isEmpty()) return conversationIds.associateWith { 0.0 }

        val messageIds = allMessages.map { it.id }
        val existingEmbeddings = dao.getEmbeddingsForMessages(messageIds)
        val embeddingByMessageId = existingEmbeddings.associateBy { it.messageId }.toMutableMap()

        val missingMessages = allMessages
            .filter { it.id !in embeddingByMessageId }
            .takeLast(12)

        missingMessages.forEach { msg ->
            val vec = generateEmbeddingVector(msg.text) ?: return@forEach
            val saved = MessageEmbedding(
                messageId = msg.id,
                vector = encodeVector(vec),
                model = embeddingModelId
            )
            dao.upsertMessageEmbedding(saved)
            embeddingByMessageId[msg.id] = saved
        }

        val messageConversationIds = ArrayList<Long>()
        val vectorBlobs = ArrayList<ByteArray>()
        messagesByConversation.forEach { (conversationId, messages) ->
            messages.forEach { msg ->
                val stored = embeddingByMessageId[msg.id] ?: return@forEach
                messageConversationIds.add(conversationId)
                vectorBlobs.add(stored.vector)
            }
        }
        if (vectorBlobs.isEmpty()) return conversationIds.associateWith { 0.0 }

        val sims = scoreMessageVectorsSafe(queryVector, vectorBlobs.toTypedArray())
        val top3ByConversation = HashMap<Long, DoubleArray>()
        for (i in sims.indices) {
            val conversationId = messageConversationIds.getOrNull(i) ?: continue
            val top = top3ByConversation.getOrPut(conversationId) {
                doubleArrayOf(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY)
            }
            val score = sims[i]
            when {
                score > top[0] -> {
                    top[2] = top[1]
                    top[1] = top[0]
                    top[0] = score
                }
                score > top[1] -> {
                    top[2] = top[1]
                    top[1] = score
                }
                score > top[2] -> top[2] = score
            }
        }

        return conversationIds.associateWith { conversationId ->
            val top = top3ByConversation[conversationId] ?: return@associateWith 0.0
            var sum = 0.0
            var count = 0
            for (score in top) {
                if (score.isFinite()) {
                    sum += score
                    count++
                }
            }
            if (count == 0) 0.0 else (sum / count).coerceIn(0.0, 1.0)
        }
    }

    private fun encodeVector(vector: FloatArray): ByteArray {
        if (vector.isEmpty()) return ByteArray(0)
        val buffer = ByteBuffer.allocate(vector.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        vector.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    private fun decodeVectorJvm(encoded: ByteArray): FloatArray {
        if (encoded.isEmpty()) return FloatArray(0)
        val count = encoded.size / 4
        if (count == 0) return FloatArray(0)
        val buffer = ByteBuffer.wrap(encoded, 0, count * 4).order(ByteOrder.LITTLE_ENDIAN)
        val out = FloatArray(count)
        for (i in 0 until count) out[i] = buffer.float
        return out
    }

    private fun normalizeVectorJvm(raw: FloatArray): FloatArray {
        if (raw.isEmpty()) return raw
        val norm = sqrt(raw.sumOf { (it * it).toDouble() }).toFloat()
        if (norm <= 1e-8f) return raw
        return FloatArray(raw.size) { idx -> raw[idx] / norm }
    }

    private fun cosineSimilarityJvm(a: FloatArray, b: FloatArray): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val size = minOf(a.size, b.size)
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in 0 until size) {
            val va = a[i].toDouble()
            val vb = b[i].toDouble()
            dot += va * vb
            normA += va * va
            normB += vb * vb
        }
        if (normA <= 1e-10 || normB <= 1e-10) return 0.0
        return (dot / (sqrt(normA) * sqrt(normB))).coerceIn(0.0, 1.0)
    }

    private fun fallbackHashEmbeddingJvm(text: String, dim: Int = fallbackEmbeddingDim): FloatArray {
        val vec = FloatArray(dim)
        val tokens = Regex("[a-zA-Z0-9']+").findAll(text.lowercase()).map { it.value }.toList()
        tokens.forEachIndexed { index, token ->
            val hash = token.hashCode()
            val idx = kotlin.math.abs(hash) % dim
            val sign = if (((hash ushr 1) and 1) == 0) 1f else -1f
            vec[idx] += sign * (1f + (index % 3) * 0.1f)
        }
        return normalizeVectorJvm(vec)
    }

    private suspend fun generateEmbeddingVector(text: String): FloatArray? {
        val clean = text.trim()
        if (clean.isBlank()) return null

        val request = EmbeddingRequest(
            model = embeddingModelId,
            input = clean.take(2500)
        )
        val useRemoteLocalEndpoint = isLocalEnabled.value && !localModelFile.exists()

        val remoteVector = try {
            when {
                useRemoteLocalEndpoint -> {
                    val host = currentHostIp.value
                    val port = currentPort.value
                    val cleanHost = host.removePrefix("https://").removePrefix("http://").trim('/')
                    val isTunnel = isTunnelHost(cleanHost) || isFunnelEnabled.value
                    val urlString = if (isTunnel) {
                        "https://$cleanHost/v1/embeddings"
                    } else {
                        "http://$cleanHost:$port/v1/embeddings"
                    }
                    apiService.embeddingsLocal(
                        urlString,
                        buildLocalHeaders(cleanHost, isTunnel),
                        request
                    ).data.firstOrNull()?.embedding?.map { it.toFloat() }?.toFloatArray()
                }
                !isLocalEnabled.value -> {
                    apiService.embeddingsOpenRouter(
                        "Bearer ${resolveOpenRouterKey()}",
                        "https://forgeint.app",
                        "ForgeInt",
                        request
                    ).data.firstOrNull()?.embedding?.map { it.toFloat() }?.toFloatArray()
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }

        return normalizeVectorSafe(remoteVector ?: fallbackHashEmbeddingSafe(clean, fallbackEmbeddingDim))
    }

    private fun queueEmbeddingForMessage(messageId: Long, text: String) {
        if (messageId <= 0L || text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val vec = generateEmbeddingVector(text) ?: return@launch
            dao.upsertMessageEmbedding(
                MessageEmbedding(
                    messageId = messageId,
                    vector = encodeVector(vec),
                    model = embeddingModelId
                )
            )
        }
    }

    private suspend fun generateResponse(chatId: Long) {
        setLoadingState(true)
        _streamingMessage.value = ""
        val attachmentsForThisTurn = pendingAttachmentsForNextSend

        val currentId = selectedPersonaId.value
        val persona = allPersonas.value.find { it.id == currentId } ?: Personas.findById(currentId)
        
        val modelToUse = currentModelId.value.trim()
        val useLocal = isLocalEnabled.value
        val conversationHistory = dao.getMessages(chatId, 10).first().reversed()
        val latestUserPrompt = conversationHistory.lastOrNull { it.isUser }?.text.orEmpty()
        val relevantConversationContext = buildRelevantConversationContext(chatId, latestUserPrompt)

        val traits = if (memory_monitor.value) traitDao.traitDao().getAllTraits() else emptyList()
        val memoryContext = if (memory_monitor.value && traits.isNotEmpty()) {
            "\n[KNOWN USER TRAITS]:\n" + traits.joinToString("\n") { "- ${it.category}: ${it.traitValue}" }
        } else ""

        val currentTimestamp = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date())
        val fullSystemPrompt = persona.systemInstruction + memoryContext + relevantConversationContext +
            "\n\n[CURRENT_TIME]: $currentTimestamp"

        val maxTokensValue = when (messageLength.value) {
            "Shorter" -> 256
            "Normal" -> 1024
            "Longer" -> 4096
            else -> 1024
        }

        try {
            val fullResponse = if (useLocal) {
                if (localModelFile.exists()) {
                    _streamingMessage.value = "Initializing Neural Core..."

                    withContext(Dispatchers.IO) {
                        if (!isModelLoaded) {
                            System.gc()
                            val loaded = localEngine.loadModel(localModelFile.absolutePath)
                            if (!loaded) throw IOException("Native Core Failed to Load")
                            isModelLoaded = true
                        }

                        val formattedPrompt = formatGemmaPrompt(fullSystemPrompt, conversationHistory)

                        withContext(Dispatchers.Main) { _streamingMessage.value = "Thinking..." }

                        val responseText = localEngine.generateResponse(formattedPrompt)

                        if (responseText.startsWith("Error:")) {
                            throw IOException(responseText)
                        } else {
                            responseText
                        }
                    }
                } else {
                    val apiMessages = mutableListOf<ApiMessage>()
                    apiMessages.add(ApiMessage("system", fullSystemPrompt))
                    val latestMessageId = conversationHistory.lastOrNull()?.id
                    conversationHistory.forEach { message ->
                        val content: Any = if (
                            message.isUser &&
                            message.id == latestMessageId &&
                            attachmentsForThisTurn.isNotEmpty()
                        ) {
                            buildLatestUserApiContent(message.text.trim(), attachmentsForThisTurn)
                        } else {
                            message.text.trim()
                        }
                        apiMessages.add(ApiMessage(if (message.isUser) "user" else "assistant", content))
                    }
                    callLmStudioStream(apiMessages, modelToUse, maxTokensValue)
                }
            } else {
                val apiMessages = mutableListOf<ApiMessage>()
                apiMessages.add(ApiMessage("system", fullSystemPrompt))
                val latestMessageId = conversationHistory.lastOrNull()?.id
                conversationHistory.forEach { message ->
                    val content: Any = if (
                        message.isUser &&
                        message.id == latestMessageId &&
                        attachmentsForThisTurn.isNotEmpty()
                    ) {
                        buildLatestUserApiContent(message.text.trim(), attachmentsForThisTurn)
                    } else {
                        message.text.trim()
                    }
                    apiMessages.add(ApiMessage(if (message.isUser) "user" else "assistant", content))
                }
                callOpenRouterStream(apiMessages, modelToUse, maxTokensValue)
            }

            if (!fullResponse.isNullOrBlank()) {
                val assistantMessageId = dao.insertMessage(Message(conversationId = chatId, text = fullResponse, isUser = false))
                queueEmbeddingForMessage(assistantMessageId, fullResponse)
            } else {
                val assistantMessageId = dao.insertMessage(Message(conversationId = chatId, text = "No response generated.", isUser = false))
                queueEmbeddingForMessage(assistantMessageId, "No response generated.")
            }
        } catch (e: CancellationException) {
            if (stopRequested && useLocal && localModelFile.exists() && isModelLoaded) {
                runCatching { localEngine.unloadModel() }
                isModelLoaded = false
            }
            throw e
        } catch (e: Exception) {
            if (!stopRequested) {
                val assistantMessageId = dao.insertMessage(Message(conversationId = chatId, text = "Error: ${e.message}", isUser = false))
                queueEmbeddingForMessage(assistantMessageId, "Error: ${e.message}")
            }

            if (useLocal && localModelFile.exists()) {
                isModelLoaded = false
            }
        } finally {
            pendingAttachmentsForNextSend = emptyList()
            _streamingMessage.value = null
            _isThinking.value = false
            setLoadingState(false)
            stopRequested = false
            activeResponseJob = null
        }
    }

    private suspend fun callLmStudioStream(messages: List<ApiMessage>, modelId: String, maxTokens: Int): String? {
        val host = currentHostIp.value
        val port = currentPort.value
        val cleanHost = host.removePrefix("https://").removePrefix("http://").trim('/')
        val isTunnel = isTunnelHost(cleanHost) || isFunnelEnabled.value

        val urlString = if (isTunnel) {
            "https://$cleanHost/v1/chat/completions"
        } else {
            "http://$cleanHost:$port/v1/chat/completions"
        }

        val headers = buildLocalHeaders(cleanHost, isTunnel)

        val request = ChatRequest(model = modelId, messages = messages, temperature = 0.7, stream = true, max_tokens = maxTokens)

        val responseBody = apiService.chatLocalStream(urlString, headers, request)
        return processStream(responseBody)
    }

    private suspend fun callOpenRouterStream(messages: List<ApiMessage>, modelId: String, maxTokens: Int): String? {
        val request = ChatRequest(model = modelId, messages = messages, stream = true, max_tokens = maxTokens)
        val authHeader = "Bearer ${resolveOpenRouterKey()}"
        val responseBody = apiService.chatOpenRouterStream(
            authHeader,
            "https://forgeint.app",
            "ForgeInt",
            request
        )
        return processStream(responseBody)
    }

    private fun resolveOpenRouterKey(): String {
        val customKey = apiKey.value.trim().takeIf {
            isCustomApiKeyEnabled.value && it.isNotBlank()
        }
        val bundledKey = BuildConfig.OPENROUTER_API_KEY.trim().takeIf { it.isNotBlank() }
        return customKey ?: bundledKey
            ?: throw IllegalStateException(
                "OpenRouter API key missing. Add OPENROUTER_API_KEY to local.properties or enter a custom key in Settings."
            )
    }

    private fun requireHuggingFaceToken(): String {
        return BuildConfig.HUGGING_FACE_TOKEN.trim().takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(
                "Hugging Face token missing. Add HUGGING_FACE_TOKEN to local.properties."
            )
    }

    private fun isTunnelHost(cleanHost: String): Boolean {
        return cleanHost.contains("cloudflare") ||
            cleanHost.contains("ngrok") ||
            cleanHost.contains("loclx") ||
            cleanHost.endsWith(".ts.net", ignoreCase = true)
    }

    private fun buildLocalHeaders(cleanHost: String, isTunnel: Boolean): HashMap<String, String> {
        val headers = HashMap<String, String>()
        if (isTunnel) {
            headers["cf-terminate-connection"] = "true"
            headers["User-Agent"] = "ForgeIntApp"
            if (cleanHost.contains("ngrok", ignoreCase = true)) {
                headers["ngrok-skip-browser-warning"] = "true"
            }
        }
        val token = localAuthToken.value.trim()
        if (token.isNotBlank()) {
            headers["Authorization"] = "Bearer $token"
        }
        return headers
    }

    private suspend fun processStream(responseBody: ResponseBody): String {
        val fullResponse = StringBuilder()
        var lastUiUpdateAt = 0L
        var lastRenderedText: String? = null
        var lastThinkingState = false

        fun buildRenderState(raw: String): Pair<Boolean, String> {
            val thinkStart = raw.indexOf("<think>")
            val thinkEnd = raw.indexOf("</think>")
            return if (thinkStart != -1 && thinkEnd == -1) {
                true to raw.substring(0, thinkStart)
            } else if (thinkStart != -1 && thinkEnd != -1) {
                false to (raw.substring(0, thinkStart) + raw.substring(thinkEnd + 8))
            } else {
                false to raw
            }
        }

        withContext(Dispatchers.IO) {
            responseBody.source().use { source ->
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break

                    val content = parseStreamChunkSafe(line)

                    if (content != null) {
                        fullResponse.append(content)
                        val (thinkingNow, visibleText) = buildRenderState(fullResponse.toString())
                        val now = SystemClock.elapsedRealtime()
                        val frameOpen = (now - lastUiUpdateAt) >= STREAM_UI_FRAME_MS
                        val thinkingChanged = thinkingNow != lastThinkingState
                        val textChanged = visibleText != lastRenderedText

                        if (thinkingChanged || (frameOpen && textChanged)) {
                            _isThinking.value = thinkingNow
                            _streamingMessage.value = visibleText
                            lastThinkingState = thinkingNow
                            lastRenderedText = visibleText
                            lastUiUpdateAt = now
                        }
                    }
                }
            }
        }
        val (_, finalVisibleText) = buildRenderState(fullResponse.toString())
        if (finalVisibleText != lastRenderedText) {
            _streamingMessage.value = finalVisibleText
        }
        _isThinking.value = false
        return fullResponse.toString()
    }
}

interface GeminiApiService {
    @Streaming
    @POST("chat/completions")
    suspend fun chatOpenRouterStream(
        @Header("Authorization") auth: String,
        @Header("HTTP-Referer") referer: String,
        @Header("X-Title") title: String,
        @Body request: ChatRequest
    ): ResponseBody

    @Streaming
    @POST
    suspend fun chatLocalStream(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body request: ChatRequest
    ): ResponseBody
    @POST("chat/completions")
    suspend fun chatOpenRouterBlocking(
        @Header("Authorization") auth: String,
        @Header("HTTP-Referer") referer: String,
        @Header("X-Title") title: String,
        @Body request: ChatRequest
    ): ChatResponse

    @POST
    suspend fun chatLocalBlocking(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body request: ChatRequest
    ): ChatResponse

    @POST("embeddings")
    suspend fun embeddingsOpenRouter(
        @Header("Authorization") auth: String,
        @Header("HTTP-Referer") referer: String,
        @Header("X-Title") title: String,
        @Body request: EmbeddingRequest
    ): EmbeddingResponse

    @POST
    suspend fun embeddingsLocal(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body request: EmbeddingRequest
    ): EmbeddingResponse
}

data class ChatRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<ApiMessage>,
    @SerializedName("temperature") val temperature: Double = 0.7,
    @SerializedName("stream") val stream: Boolean = false,
    @SerializedName("max_tokens") val max_tokens: Int? = null
)

data class EmbeddingRequest(
    @SerializedName("model") val model: String,
    @SerializedName("input") val input: String
)

data class EmbeddingResponse(
    @SerializedName("data") val data: List<EmbeddingData>
)

data class EmbeddingData(
    @SerializedName("embedding") val embedding: List<Double>
)

data class ChatResponse(
    @SerializedName("choices") val choices: List<Choice>
)

data class Choice(
    @SerializedName("message") val message: ApiMessage
)
data class ApiMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: Any
)
data class ChatStreamResponse(
    @SerializedName("choices") val choices: List<StreamChoice>
)

data class StreamChoice(
    @SerializedName("delta") val delta: Delta
)

data class Delta(
    @SerializedName("content") val content: String?
)
