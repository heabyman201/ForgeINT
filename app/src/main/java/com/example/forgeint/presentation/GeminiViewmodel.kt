package com.example.forgeint.presentation
import Persona
import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weargemini.data.SettingsManager
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
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
import java.io.IOException
import java.net.URLEncoder
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.asResponseBody
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject

enum class ConnectionMode {
    Standalone, // Uses Watch Wi-Fi/LTE directly
    Phone // Proxies requests via Phone
}

data class PendingPcAction(
    val command: String,
    val title: String,
    val prompt: String,
    val confirmLabel: String
)

class GeminiViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "GeminiViewModel"
        private const val STREAM_UI_FRAME_MS = 50L
        @Volatile private var nativeParserAvailable = false
        @Volatile private var nativeNumericOpsAvailable = false
    }

    init {
        val nativeLoaded = try {
            System.loadLibrary("forgeint_local")
            true
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.e(TAG, "Failed to load forgeint_local; using Kotlin parser fallback", e)
            false
        } catch (e: Throwable) {
            android.util.Log.e(TAG, "Unexpected native load failure; using Kotlin parser fallback", e)
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
                android.util.Log.e(TAG, "Native parser unavailable at runtime; falling back to Kotlin parser", e)
            } catch (e: Throwable) {
                nativeParserAvailable = false
                android.util.Log.e(TAG, "Native parser failed at runtime; falling back to Kotlin parser", e)
            }
        }

        if (!rawData.startsWith("data: ") || rawData.contains("[DONE]")) return null
        val contentKey = "\"content\":\""
        val start = rawData.indexOf(contentKey)
        if (start == -1) return null

        val src = rawData.substring(start + contentKey.length)
        val out = StringBuilder(minOf(src.length, 2048))
        var escaped = false

        for (c in src) {
            if (escaped) {
                when (c) {
                    'n' -> out.append('\n')
                    't' -> out.append('\t')
                    'r' -> {}
                    '"', '\\' -> out.append(c)
                    else -> out.append(c)
                }
                escaped = false
                continue
            }
            if (c == '\\') {
                escaped = true
                continue
            }
            if (c == '"') break
            out.append(c)
            if (out.length >= 2048) break
        }
        return out.takeIf { it.isNotEmpty() }?.toString()
    }

    private fun decodeVectorSafe(blob: ByteArray): FloatArray {
        if (blob.isEmpty()) return FloatArray(0)
        return decodeVectorJvm(blob)
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
        if (vectorBlobs.isEmpty()) return DoubleArray(0)
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
            cosineSimilaritySafe(queryVector, decodeVectorSafe(vectorBlobs[idx]))
        }
    }

    private val dao = ChatDatabase.getDatabase(application).chatDao()
    private val settingsManager = SettingsManager(application)
    private val traitDao = ChatDatabase.getDatabase(application)
    private val customPersonaRepository = com.example.forgeint.data.CustomPersonaRepository(application)

    private val phoneCommunicator = PhoneCommunicator(application)

    private val _connectionMode = MutableStateFlow(ConnectionMode.Standalone)
    val connectionMode = _connectionMode.asStateFlow()

    fun toggleConnectionMode() {
        _connectionMode.value = if (_connectionMode.value == ConnectionMode.Standalone) {
            ConnectionMode.Phone
        } else {
            ConnectionMode.Standalone
        }
    }

    fun requestPcAction(action: PendingPcAction) {
        _pendingPcAction.value = action
    }

    fun clearPcActionRequest() {
        _pendingPcAction.value = null
    }

    private val openRouterKey = "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"
    private val tavilyApiKey = "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"
    private val embeddingModelId = "text-embedding-qwen3-embedding-0.6b"

    private val fallbackEmbeddingDim = 256

    private val _streamingMessage = MutableStateFlow<String?>(null)
    val streamingMessage = _streamingMessage.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking = _isThinking.asStateFlow()

    private val _isWebSearching = MutableStateFlow(false)
    val isWebSearching = _isWebSearching.asStateFlow()

    private val _pendingPcAction = MutableStateFlow<PendingPcAction?>(null)
    val pendingPcAction = _pendingPcAction.asStateFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)

        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://openrouter.ai/api/v1/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(GeminiApiService::class.java)

    val currentModelId = settingsManager.selectedModel
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsManager.DEFAULT_CLOUD_MODEL_ID)

    val memory_monitor = settingsManager.isMemoryMonitorEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val thermal_monitor = settingsManager.isSystemTelemetryEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    var currentHostIp = settingsManager.hostIp
        .stateIn(viewModelScope, SharingStarted.Eagerly, "https://covers-headers-inclusion-footage.trycloudflare.com")

    val currentPort = settingsManager.serverPort
        .stateIn(viewModelScope, SharingStarted.Eagerly, "1234")

    val hardwareHostIp = settingsManager.hardwareHostIp
        .stateIn(viewModelScope, SharingStarted.Eagerly, "192.168.1.1")

    val hardwarePort = settingsManager.hardwarePort
        .stateIn(viewModelScope, SharingStarted.Eagerly, "8080")

    val remoteHostIp = settingsManager.remoteHostIp
        .stateIn(viewModelScope, SharingStarted.Eagerly, "100.96.101.62")

    val remotePort = settingsManager.remotePort
        .stateIn(viewModelScope, SharingStarted.Eagerly, "1235")

    val isFunnelEnabled = settingsManager.isFunnelEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val localAuthToken = settingsManager.localAuthToken
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsManager.DEFAULT_LOCAL_AUTH_TOKEN)

    val isLocalEnabled = settingsManager.isLocalEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val appTheme = settingsManager.appTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Default")

    val selectedPersonaId = settingsManager.selectedPersonaId
        .stateIn(viewModelScope, SharingStarted.Eagerly, "default")

    private val _customPersonas = MutableStateFlow(customPersonaRepository.getCustomPersonas())
    
    val allPersonas = _customPersonas.map { custom ->
        Personas.list + custom
    }.stateIn(viewModelScope, SharingStarted.Lazily, Personas.list)

    fun createPersona(name: String, description: String, prompt: String) {
        val newPersona = Persona(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            description = description,
            systemInstruction = prompt
        )
        customPersonaRepository.addCustomPersona(newPersona)
        _customPersonas.value = customPersonaRepository.getCustomPersonas()
    }

    fun deletePersona(id: String) {
        customPersonaRepository.deleteCustomPersona(id)
        _customPersonas.value = customPersonaRepository.getCustomPersonas()
        // If the deleted persona was selected, revert to default
        if (selectedPersonaId.value == id) {
             setPersona("default")
        }
    }
    
    fun setPersona(personaId: String) {
        viewModelScope.launch { settingsManager.setSelectedPersona(personaId) }
    }

    val isLiteMode = settingsManager.isLiteMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(4500), true)

    val isVoiceDominantMode = settingsManager.isVoiceDominantMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(4500), false)

    val messageLength = settingsManager.messageLength
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Normal")

    val apiKey = settingsManager.apiKey
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val isCustomApiKeyEnabled = settingsManager.isCustomApiKeyEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val history = dao.getConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentConversationId = MutableStateFlow<Long?>(null)

    private var _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    private val _loadingBurst = MutableStateFlow(false)
    val loadingBurst = _loadingBurst.asStateFlow()
    private var loadingBurstJob: Job? = null

    private val _isTesting = MutableStateFlow(false)
    val isTesting = _isTesting.asStateFlow()

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult = _testResult.asStateFlow()

    private val _messageLimit = MutableStateFlow(20)

    private val _isHistoryLoading = MutableStateFlow(false)
    val isHistoryLoading = _isHistoryLoading.asStateFlow()

    private val _isWebSearchEnabled = MutableStateFlow(false)
    val isWebSearchEnabled = _isWebSearchEnabled.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _pendingAction = MutableStateFlow<String?>(null)
    val pendingAction = _pendingAction.asStateFlow()
    private var activeResponseJob: Job? = null
    private var isContextRolloverActive = false
    private var temporaryCloudRoutingSnapshot: Pair<Boolean, String>? = null
    private var stopRequested = false

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
            // Give the overlay a softer ramp while still skipping very short requests.
            delay(180)
            if (!_isLoading.value) return@launch

            _loadingBurst.value = true
            delay(780)
            _loadingBurst.value = false
        }
    }

    fun setPendingAction(action: String) {
        _pendingAction.value = action
    }

    fun clearPendingAction() {
        _pendingAction.value = null
    }

    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels = _availableModels.asStateFlow()

    private var sessionStartModelId: String? = null
    private var lastMemoryMaintenanceAt: Long = 0L
    private var didInitialConnectionCheck = false

    private val shortTermExpiryMs = 12 * 60 * 60 * 1000L
    private val shortTermRelevanceAgeMs = 2 * 60 * 60 * 1000L
    private val memoryMaintenanceIntervalMs = 30 * 60 * 1000L
    private val shortTermMinRecentHits = 1
    private val shortTermPromotionHits = 4
    private val shortTermMinPromotionAgeMs = 30 * 60 * 1000L

    fun toggleMemoryMonitor() {
        viewModelScope.launch {
            settingsManager.setMemoryMonitorEnabled(!memory_monitor.value)
        }
    }

    suspend fun fetchAvailableModels() = withContext(Dispatchers.IO) {
        val urlString = buildLocalUrl("/v1/models")
        val headers = buildLocalHeaders()

        try {
            val bodyStr: String?
            if (connectionMode.value == ConnectionMode.Phone) {
                 val remoteRequest = com.example.forgeint.RemoteRequest(
                    url = urlString,
                    method = "GET",
                    headers = headers
                )
                bodyStr = phoneCommunicator.sendRequest(remoteRequest)
            } else {
                val requestBuilder = Request.Builder().url(urlString)
                headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
                val request = requestBuilder.build()
                val response = okHttpClient.newCall(request).execute()
                bodyStr = response.body?.string()
                response.close()
            }
            
            if (bodyStr != null) {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleThermalMonitor() {
        viewModelScope.launch {
            settingsManager.setSystemTelemetryEnabled(!thermal_monitor.value)
        }
    }

    fun toggleWebSearch() {
        _isWebSearchEnabled.value = !_isWebSearchEnabled.value
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
        checkForEmulator()
        viewModelScope.launch {
            if (!didInitialConnectionCheck && settingsManager.isLocalEnabled.first()) {
                didInitialConnectionCheck = true
                performConnectionCheck()
            }
        }
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
        activeResponseJob?.cancel()
        phoneCommunicator.cleanup()
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

    fun setModel(modelId: String) {
        viewModelScope.launch { settingsManager.setModel(modelId) }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { settingsManager.setAppTheme(theme) }
    }

    fun setLiteMode(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setLiteMode(enabled) }
    }

    fun setVoiceDominantMode(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setVoiceDominantMode(enabled) }
    }

    fun toggleLocalMode(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setLocalEnabled(enabled) }
    }

    suspend fun setTemporaryCloudRouting(
        enabled: Boolean,
        telemetry: SystemStabilityTelemetry? = null
    ): Boolean {
        return withContext(Dispatchers.IO) {
            if (enabled) {
                if (temporaryCloudRoutingSnapshot != null) return@withContext false

                val previousLocalMode = settingsManager.isLocalEnabled.first()
                val previousModel = settingsManager.selectedModel.first()
                temporaryCloudRoutingSnapshot = previousLocalMode to previousModel

                if (previousLocalMode) {
                    settingsManager.setLocalEnabled(false)
                }

                val targetModel = SettingsManager.DEFAULT_CLOUD_MODEL_ID
                if (previousModel != targetModel) {
                    settingsManager.setModel(targetModel)
                }

                telemetry?.let {
                    android.util.Log.w(TAG, "Temporary OpenRouter routing enabled due to instability: ${it.describe()}")
                }
                true
            } else {
                val snapshot = temporaryCloudRoutingSnapshot ?: return@withContext false
                temporaryCloudRoutingSnapshot = null

                settingsManager.setLocalEnabled(snapshot.first)
                if (snapshot.second.isNotBlank()) {
                    settingsManager.setModel(snapshot.second)
                }

                telemetry?.let {
                    android.util.Log.i(TAG, "Temporary OpenRouter routing cleared after stability recovered: ${it.describe()}")
                }
                true
            }
        }
    }

    fun setMessageLength(length: String) {
        viewModelScope.launch { settingsManager.setMessageLength(length) }
    }

    fun updateApiKey(key: String) {
        viewModelScope.launch { settingsManager.setApiKey(key) }
    }

    fun toggleCustomApiKey(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setCustomApiKeyEnabled(enabled) }
    }

    fun updateHostIp(newIp: String) {
        viewModelScope.launch {
            val cleanIp = newIp
                .replace("https://", "")
                .replace("http://", "")
                .replace("/", "")
                .trim()
            val useHttps = cleanIp.endsWith(".ts.net", ignoreCase = true) || isFunnelEnabled.value
            val newUrl = if (useHttps) "https://$cleanIp" else "http://$cleanIp"

            settingsManager.setHostIp(newUrl)

            _testResult.value = null
            _testResult.value = null
        }
    }

    fun updatePort(newPort: String) {
        viewModelScope.launch {
            settingsManager.setServerPort(newPort)
            _testResult.value = null
        }
    }

    fun updateHardwareHostIp(newIp: String) {
        viewModelScope.launch {
            val cleanIp = newIp
                .replace("https://", "")
                .replace("http://", "")
                .replace("/", "")
                .trim()
            val useHttps = cleanIp.endsWith(".ts.net", ignoreCase = true) ||
                cleanIp.contains("cloudflare", ignoreCase = true) ||
                cleanIp.contains("ngrok", ignoreCase = true) ||
                cleanIp.contains("loclx", ignoreCase = true) ||
                isFunnelEnabled.value
            val newUrl = if (useHttps) "https://$cleanIp" else "http://$cleanIp"

            settingsManager.setHardwareHostIp(newUrl)
        }
    }

    fun updateHardwarePort(newPort: String) {
        viewModelScope.launch {
            settingsManager.setHardwarePort(newPort)
        }
    }

    fun updateRemoteHostIp(newIp: String) {
        viewModelScope.launch {
            val cleanIp = newIp
                .replace("https://", "")
                .replace("http://", "")
                .replace("/", "")
                .trim()
            val useHttps = cleanIp.endsWith(".ts.net", ignoreCase = true) ||
                    cleanIp.contains("cloudflare", ignoreCase = true) ||
                    cleanIp.contains("ngrok", ignoreCase = true) ||
                    cleanIp.contains("loclx", ignoreCase = true) ||
                    isFunnelEnabled.value
            val newUrl = if (useHttps) "https://$cleanIp" else "http://$cleanIp"

            settingsManager.setRemoteHostIp(newUrl)
        }
    }

    fun updateRemotePort(newPort: String) {
        viewModelScope.launch {
            settingsManager.setRemotePort(newPort)
        }
    }

    fun setFunnelEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setFunnelEnabled(enabled)
            _testResult.value = null
        }
    }

    fun updateLocalAuthToken(token: String) {
        viewModelScope.launch {
            settingsManager.setLocalAuthToken(token)
        }
    }

    private fun isTunnelHost(cleanHost: String): Boolean {
        val lowered = cleanHost.lowercase()
        return isFunnelEnabled.value ||
            lowered.endsWith(".ts.net") ||
            lowered.contains("cloudflare") ||
            lowered.contains("ngrok") ||
            lowered.contains("loclx")
    }

    private fun buildLocalUrl(path: String): String {
        val cleanHost = currentHostIp.value.removePrefix("https://").removePrefix("http://").trim('/')
        return if (isTunnelHost(cleanHost)) {
            "https://$cleanHost$path"
        } else {
            "http://$cleanHost:${currentPort.value}$path"
        }
    }

    private fun buildLocalHeaders(): HashMap<String, String> {
        val headers = HashMap<String, String>()
        val cleanHost = currentHostIp.value.removePrefix("https://").removePrefix("http://").trim('/').lowercase()
        val isTunnel = isTunnelHost(cleanHost)
        if (isTunnel) {
            if (cleanHost.contains("ngrok")) {
                headers["ngrok-skip-browser-warning"] = "true"
            }
            headers["User-Agent"] = "ForgeIntApp"
        }
        if (localAuthToken.value.isNotBlank()) {
            headers["Authorization"] = "Bearer ${localAuthToken.value.trim()}"
        }
        return headers
    }

    val allTraits = traitDao.traitDao().getAllTraitsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteTrait(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            traitDao.traitDao().deleteTrait(id)
        }
    }

    fun clearAllTraits() {
        viewModelScope.launch(Dispatchers.IO) {
            traitDao.traitDao().deleteAllTraits()
        }
    }

    fun addManualMemory(rawContent: String, type: MemoryType) {
        viewModelScope.launch(Dispatchers.IO) {
            val content = rawContent.trim().removeSurrounding("\"").trim()
            val normalized = normalizeMemoryText(content)

            if (normalized.length < 3 || normalized.length > 180) return@launch
            if (isInstructionLikeMemory(normalized)) return@launch
            if (!isUsefulManualMemoryCandidate(normalized, type)) return@launch

            val traitApi = traitDao.traitDao()
            val existing = traitApi.getAllTraits().map { normalizeMemoryText(it.content) }.toSet()
            if (existing.contains(normalized)) return@launch

            traitApi.insertTrait(
                UserTrait(
                    content = content,
                    type = type,
                    source = "manual_user_input"
                )
            )
            android.util.Log.d("MemoryDebug", "MEMORY SAVED (manual $type): $content")
        }
    }

    fun startNewChat(prompt: String) {
        if (_isLoading.value) return

        activeResponseJob = viewModelScope.launch {
            stopRequested = false
            sessionStartModelId = currentModelId.value
            _messageLimit.value = 20
            val newId = dao.insertConversation(Conversation(summary = prompt))
            _currentConversationId.value = newId
            val userMessageId = dao.insertMessage(Message(conversationId = newId, text = prompt, isUser = true))
            queueEmbeddingForMessage(userMessageId, prompt)
            generateResponse(newId, titleSeedUserPrompt = prompt)
            launch(Dispatchers.IO) { extractAndStoreTraits(prompt) }
        }
    }

    fun startEmptyChat() {
        if (_isLoading.value) return

        activeResponseJob?.cancel()
        stopRequested = false
        sessionStartModelId = currentModelId.value
        _messageLimit.value = 20
        _streamingMessage.value = null
        _isThinking.value = false
        _isWebSearching.value = false
        setLoadingState(false)

        viewModelScope.launch {
            val newId = dao.insertConversation(Conversation(summary = "New Conversation"))
            _currentConversationId.value = newId
        }
    }

    fun startVoiceChat() {
        if (_isLoading.value) return

        activeResponseJob?.cancel()
        stopRequested = false
        sessionStartModelId = currentModelId.value
        _messageLimit.value = 20
        _streamingMessage.value = null
        _isThinking.value = false
        _isWebSearching.value = false
        setLoadingState(false)

        viewModelScope.launch {
            val newId = dao.insertConversation(Conversation(summary = "New Voice Chat"))
            _currentConversationId.value = newId
        }
    }

    suspend fun summarizeCurrentChatAndRollContext(telemetry: VramTelemetry? = null): Boolean {
        if (_isLoading.value) return false
        if (isContextRolloverActive) return false

        val chatId = _currentConversationId.value ?: return false
        val activeChat = currentChat.value ?: return false
        val history = dao.getMessages(chatId, 100).first().reversed()
        if (history.isEmpty()) return false

        isContextRolloverActive = true
        return try {
            withContext(Dispatchers.IO) {
                val summary = generateContextCarryoverSummary(activeChat.conversation.summary, history, telemetry)
                    ?: return@withContext false

                val newConversationTitle = buildString {
                    append("Continued")
                    activeChat.conversation.summary
                        .takeIf { it.isNotBlank() }
                        ?.let { append(": ${it.take(40)}") }
                }

                val newId = dao.insertConversation(Conversation(summary = newConversationTitle))
                dao.insertMessage(
                    Message(
                        conversationId = newId,
                        text = buildCarryoverSeedMessage(summary, telemetry),
                        isUser = false
                    )
                )
                _messageLimit.value = 20
                _streamingMessage.value = null
                _isThinking.value = false
                _isWebSearching.value = false
                _currentConversationId.value = newId
                true
            }
        } catch (t: Throwable) {
            android.util.Log.e(TAG, "Context rollover failed", t)
            false
        } finally {
            isContextRolloverActive = false
        }
    }

    fun continueChat(prompt: String) {
        if (_isLoading.value) return

        val chatId = _currentConversationId.value ?: return
        activeResponseJob = viewModelScope.launch(Dispatchers.IO) {
            stopRequested = false
            val shouldSeedTitle = dao.getRecentMessagesForConversation(chatId, limit = 1).isEmpty()
            val userMessageId = dao.insertMessage(Message(conversationId = chatId, text =  prompt, isUser = true))
            queueEmbeddingForMessage(userMessageId, prompt)
            launch(Dispatchers.IO) { extractAndStoreTraits(prompt) }

            generateResponse(
                chatId,
                titleSeedUserPrompt = prompt.takeIf { shouldSeedTitle }
            )
        }
    }

    fun submitVoiceInput(prompt: String) {
        val trimmedPrompt = prompt.trim()
        if (trimmedPrompt.isEmpty() || _isLoading.value) return

        val chatId = _currentConversationId.value
        if (chatId == null) {
            startNewChat(trimmedPrompt)
            return
        }

        activeResponseJob = viewModelScope.launch(Dispatchers.IO) {
            stopRequested = false
            val shouldSeedTitle = dao.getRecentMessagesForConversation(chatId, limit = 1).isEmpty()

            val userMessageId = dao.insertMessage(
                Message(conversationId = chatId, text = trimmedPrompt, isUser = true)
            )
            queueEmbeddingForMessage(userMessageId, trimmedPrompt)
            launch(Dispatchers.IO) { extractAndStoreTraits(trimmedPrompt) }

            generateResponse(
                chatId,
                titleSeedUserPrompt = trimmedPrompt.takeIf { shouldSeedTitle }
            )
        }
    }

    fun stopResponse() {
        stopRequested = true
        activeResponseJob?.cancel(CancellationException("Stopped by user"))
        _isThinking.value = false
        _isWebSearching.value = false
        _streamingMessage.value = null
        setLoadingState(false)
    }

    fun openChat(id: Long) {
        _messageLimit.value = 20
        _currentConversationId.value = id
        sessionStartModelId = currentModelId.value
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
        if (userText.length < 8) {
            android.util.Log.d("MemoryDebug", "Skipping memory: Text too short (${userText.length} < 8)")
            return
        }

        val explicitMemory = extractExplicitMemoryInstruction(userText)
        if (explicitMemory != null) {
            val (forcedType, forcedContent) = explicitMemory
            if (isUsefulMemoryCandidate(forcedContent, forcedType)) {
                val normalizedForced = normalizeMemoryText(forcedContent)
                val existing = traitDao.traitDao().getAllTraits()
                    .map { normalizeMemoryText(it.content) }
                    .toSet()
                if (!existing.contains(normalizedForced)) {
                    traitDao.traitDao().insertTrait(
                        UserTrait(
                            content = forcedContent,
                            type = forcedType,
                            source = "explicit_user_instruction"
                        )
                    )
                    android.util.Log.d("MemoryDebug", "MEMORY SAVED (explicit $forcedType): $forcedContent")
                }
            } else {
                android.util.Log.d("MemoryDebug", "Rejected explicit memory as low-quality: $forcedContent")
            }
            return
        }

        val modelToUse = currentModelId.value
        android.util.Log.d("MemoryDebug", "Analyzing memory for: '$userText' using $modelToUse")
        
        val apiMessages = listOf(
            ApiMessage("system", MemoryPrompter.extractionPrompt),
            ApiMessage("user", "User Message: \"$userText\"")
        )

        try {
            val request = ChatRequest(model = modelToUse, messages = apiMessages, stream = false,
                max_tokens = 256)

            val responseText: String? = if (connectionMode.value == ConnectionMode.Phone) {
                val gson = Gson()
                // Determine URL and Headers based on local/cloud setting
                val useLocalNet = isLocalEnabled.value
                val url: String
                val headers: Map<String, String>

                if (useLocalNet) {
                    url = buildLocalUrl("/v1/chat/completions")
                    headers = buildLocalHeaders()
                } else {
                    val currentKey = if (isCustomApiKeyEnabled.value && apiKey.value.isNotBlank()) apiKey.value else openRouterKey
                    url = "https://openrouter.ai/api/v1/chat/completions"
                    headers = mapOf("Authorization" to "Bearer $currentKey", "HTTP-Referer" to "https://forgeint.app", "X-Title" to "ForgeInt")
                }

                val remoteRequest = com.example.forgeint.RemoteRequest(url = url, method = "POST", headers = headers, body = gson.toJson(request))
                
                val jsonResponse = phoneCommunicator.sendRequest(remoteRequest)
                // Parse the JSON response to get content
                if (jsonResponse != null) {
                    try {
                        val jsonObj = JSONObject(jsonResponse)
                        val choices = jsonObj.optJSONArray("choices")
                        val content = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
                        android.util.Log.d("MemoryDebug", "Raw Phone Response: $content")
                        content
                    } catch (e: Exception) { 
                        android.util.Log.e("MemoryDebug", "JSON Parse Error: ${e.message}")
                        null 
                    }
                } else {
                    android.util.Log.e("MemoryDebug", "Phone Response is NULL")
                    null
                }

            } else if (isLocalEnabled.value) {
                val urlString = buildLocalUrl("/v1/chat/completions")
                val headers = buildLocalHeaders()

                val response = apiService.chatLocalBlocking(urlString, headers, request)
                val content = response.choices.firstOrNull()?.message?.content
                android.util.Log.d("MemoryDebug", "Raw Local/Cloud Response: $content")
                content
            } else if (!isLocalEnabled.value) {
                val currentKey = if (isCustomApiKeyEnabled.value && apiKey.value.isNotBlank()) apiKey.value else openRouterKey
                val response = apiService.chatOpenRouterBlocking(
                    "Bearer $currentKey",
                    "https://forgeint.app",
                    "ForgeInt",
                    request
                )
                val content = response.choices.firstOrNull()?.message?.content
                android.util.Log.d("MemoryDebug", "Raw OpenRouter Response: $content")
                content
            } else {
                null
            }

            responseText?.let { text ->
                if (!text.contains("NULL")) {
                    val traitsList = traitDao.traitDao().getAllTraits()
                    val existingTraits = traitsList.map { normalizeMemoryText(it.content) }.toMutableSet()
                    
                    text.lines().forEach { line ->
                        val trimmed = line.trim().removePrefix("- ").trim()
                        if (trimmed.isBlank() || trimmed.length <= 3) return@forEach

                        val type = when {
                            trimmed.startsWith("[SHORT_TERM]", ignoreCase = true) -> MemoryType.SHORT_TERM
                            trimmed.startsWith("[LONG_TERM]", ignoreCase = true) -> MemoryType.LONG_TERM
                            else -> MemoryType.SHORT_TERM // Default to STM if not specified
                        }
                        
                        val cleanLine = trimmed
                            .removePrefix("[SHORT_TERM]")
                            .removePrefix("[short_term]")
                            .removePrefix("[LONG_TERM]")
                            .removePrefix("[long_term]")
                            .trim()

                        if (cleanLine.isNotBlank()) {
                             if (!isUsefulMemoryCandidate(cleanLine, type)) {
                                 android.util.Log.d("MemoryDebug", "Skipping low-quality memory: $cleanLine")
                                 return@forEach
                             }

                             val normalizedNewLine = normalizeMemoryText(cleanLine)

                             // Basic contradiction check: If a new fact is similar but different, 
                             // we could potentially remove the old one. 
                             // For now, we'll check if any existing trait's content is contained 
                             // within the new line or vice versa to update it.
                             val contradictoryTrait = traitsList.find { 
                                 it.content.contains(cleanLine, ignoreCase = true) || 
                                 cleanLine.contains(it.content, ignoreCase = true) 
                             }
                             
                             if (contradictoryTrait != null) {
                                 traitDao.traitDao().deleteTrait(contradictoryTrait.id)
                                 android.util.Log.d("MemoryDebug", "Removing contradictory memory: ${contradictoryTrait.content}")
                                 existingTraits.remove(normalizeMemoryText(contradictoryTrait.content))
                             }

                             if (!existingTraits.contains(normalizedNewLine)) {
                                 traitDao.traitDao().insertTrait(UserTrait(content = cleanLine, type = type))
                                 android.util.Log.d("MemoryDebug", "MEMORY SAVED ($type): $cleanLine")
                                 existingTraits.add(normalizedNewLine)
                             }
                        }
                    }
                    // Baseline STM expiry cleanup (full relevance/promotion runs in maintenance pass).
                    val expiryTime = System.currentTimeMillis() - shortTermExpiryMs
                    traitDao.traitDao().deleteExpiredShortTermMemory(expiryTime)
                } else {
                    android.util.Log.d("MemoryDebug", "Response contained NULL, no memory extracted.")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("MemoryDebug", "Exception: ${e.message}")
        }
    }

    private fun normalizeMemoryText(text: String): String {
        return text
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun isUsefulMemoryCandidate(text: String, type: MemoryType): Boolean {
        val normalized = normalizeMemoryText(text)
        if (normalized.length < 8 || normalized.length > 180) return false

        if (isInstructionLikeMemory(normalized)) return false

        val genericNoise = listOf(
            "user is talking",
            "user is speaking",
            "user is discussing",
            "user is asking",
            "the user",
            "talking about something",
            "discussing something",
            "something",
            "anything",
            "stuff",
            "random topic",
            "conversation",
            "used the word",
            "used word",
            "said the word",
            "save it to long term",
            "save to long term",
            "save it to short term",
            "save to short term"
        )
        if (genericNoise.any { normalized.contains(it) }) return false

        val emotionMeta = Regex("\\buser\\s+is\\s+(frustrated|angry|mad|upset|sad|annoyed|confused)\\b")
        if (emotionMeta.containsMatchIn(normalized)) return false

        val meaningfulKeywords = Regex(
            "\\b(name|called|live|from|work|job|student|study|age|birthday|allergic|prefer|like|dislike|love|hate|use|using|owns|has|currently|today|tomorrow|feeling|mood|working on|building|debugging|learning)\\b"
        )
        val hasDigits = normalized.any { it.isDigit() }
        val enoughWords = normalized.split(" ").size >= 4
        if (!meaningfulKeywords.containsMatchIn(normalized) && !hasDigits && !enoughWords) return false

        if (type == MemoryType.LONG_TERM) {
            val shortTermMarkers = listOf(
                "today",
                "right now",
                "currently",
                "this morning",
                "this evening",
                "tonight",
                "at the moment",
                "feeling",
                "mood"
            )
            if (shortTermMarkers.any { normalized.contains(it) }) return false
        }

        return true
    }

    private fun isInstructionLikeMemory(normalizedText: String): Boolean {
        val instructionPatterns = listOf(
            "\\bsave\\b",
            "\\bremember\\b",
            "\\bstore\\b",
            "\\blong term\\b",
            "\\bshort term\\b",
            "\\bmemory\\b",
            "\\bextract\\b"
        )
        return instructionPatterns.any { Regex(it).containsMatchIn(normalizedText) }
    }

    private fun isUsefulManualMemoryCandidate(normalizedText: String, type: MemoryType): Boolean {
        val genericNoise = listOf(
            "user is talking",
            "user is speaking",
            "user is discussing",
            "user is asking",
            "talking about something",
            "used the word",
            "used word",
            "conversation"
        )
        if (genericNoise.any { normalizedText.contains(it) }) return false

        val emotionMeta = Regex("\\buser\\s+is\\s+(frustrated|angry|mad|upset|sad|annoyed|confused)\\b")
        if (emotionMeta.containsMatchIn(normalizedText)) return false

        if (type == MemoryType.LONG_TERM) {
            val shortTermMarkers = listOf(
                "today",
                "right now",
                "currently",
                "this morning",
                "this evening",
                "tonight",
                "at the moment"
            )
            if (shortTermMarkers.any { normalizedText.contains(it) }) return false
        }
        return true
    }

    private fun extractExplicitMemoryInstruction(userText: String): Pair<MemoryType, String>? {
        val trimmed = userText.trim()
        if (trimmed.length < 12) return null

        val quotedPattern = Regex(
            "(?i)\\b(?:save|remember|store)\\b\\s*\"([^\"]{3,180})\"\\s*(?:as|to|in)?\\s*(long\\s*term|short\\s*term)"
        )
        quotedPattern.find(trimmed)?.let { match ->
            val content = match.groupValues[1].trim()
            val type = if (match.groupValues[2].contains("long", ignoreCase = true)) {
                MemoryType.LONG_TERM
            } else {
                MemoryType.SHORT_TERM
            }
            return type to content
        }

        val colonPattern = Regex(
            "(?i)\\b(?:save|remember|store)\\b\\s*(?:this|that)?\\s*(?:as|to|in)?\\s*(long\\s*term|short\\s*term)\\s*[:\\-]\\s*(.{3,180})$"
        )
        colonPattern.find(trimmed)?.let { match ->
            val type = if (match.groupValues[1].contains("long", ignoreCase = true)) {
                MemoryType.LONG_TERM
            } else {
                MemoryType.SHORT_TERM
            }
            val content = match.groupValues[2].trim()
            return type to content
        }

        val inlinePattern = Regex(
            "(?i)\\b(?:save|remember|store)\\b\\s+(.{3,180}?)\\s+(?:as|to|in)\\s+(long\\s*term|short\\s*term)\\b"
        )
        inlinePattern.find(trimmed)?.let { match ->
            val content = match.groupValues[1]
                .trim()
                .removePrefix("that ")
                .removePrefix("this ")
                .trim()
            if (content.equals("it", ignoreCase = true)) return null
            val type = if (match.groupValues[2].contains("long", ignoreCase = true)) {
                MemoryType.LONG_TERM
            } else {
                MemoryType.SHORT_TERM
            }
            return type to content
        }

        return null
    }

    private suspend fun runMemoryMaintenance(chatId: Long, recentMessages: List<Message>) {
        val now = System.currentTimeMillis()
        if (now - lastMemoryMaintenanceAt < memoryMaintenanceIntervalMs) return
        lastMemoryMaintenanceAt = now

        val traitApi = traitDao.traitDao()
        val allTraits = traitApi.getAllTraits()
        val shortTermTraits = allTraits.filter { it.type == MemoryType.SHORT_TERM }
        if (shortTermTraits.isEmpty()) return

        val longTermNormalized = allTraits
            .filter { it.type == MemoryType.LONG_TERM }
            .map { normalizeMemoryText(it.content) }
            .toMutableSet()

        val normalizedRecentUserMessages = recentMessages
            .filter { it.isUser }
            .map { normalizeMemoryText(it.text) }

        shortTermTraits.forEach { trait ->
            val normalizedTrait = normalizeMemoryText(trait.content)
            val ageMs = now - trait.timestamp

            if (ageMs >= shortTermExpiryMs) {
                traitApi.deleteTrait(trait.id)
                android.util.Log.d("MemoryDebug", "Deleting expired STM: ${trait.content}")
                return@forEach
            }

            val keywords = extractMemoryKeywords(normalizedTrait)
            val hitCount = if (keywords.isEmpty()) 0 else normalizedRecentUserMessages.count { msg ->
                keywords.any { keyword -> msg.contains(keyword) }
            }

            if (ageMs >= shortTermMinPromotionAgeMs &&
                hitCount >= shortTermPromotionHits &&
                !isLikelyEphemeralMemory(normalizedTrait)
            ) {
                if (!longTermNormalized.contains(normalizedTrait)) {
                    traitApi.insertTrait(
                        UserTrait(
                            content = trait.content,
                            type = MemoryType.LONG_TERM,
                            source = "promoted_from_stm",
                            timestamp = now
                        )
                    )
                    longTermNormalized.add(normalizedTrait)
                    android.util.Log.d("MemoryDebug", "Promoted STM to LTM: ${trait.content}")
                }
                traitApi.deleteTrait(trait.id)
                return@forEach
            }

            if (ageMs >= shortTermRelevanceAgeMs && hitCount < shortTermMinRecentHits) {
                traitApi.deleteTrait(trait.id)
                android.util.Log.d("MemoryDebug", "Pruned irrelevant STM: ${trait.content}")
            }
        }

        android.util.Log.d("MemoryDebug", "Memory maintenance complete for chatId=$chatId")
    }

    private fun extractMemoryKeywords(normalizedText: String): Set<String> {
        val stopWords = setOf(
            "the", "and", "for", "with", "from", "that", "this", "there", "about",
            "have", "has", "had", "been", "were", "will", "just", "very", "into",
            "your", "you", "user", "today", "now", "then", "what", "when", "where",
            "while", "them", "they", "their", "like", "want", "need"
        )

        return normalizedText
            .split(" ")
            .asSequence()
            .map { it.trim() }
            .filter { it.length >= 4 && it !in stopWords && it.any { ch -> ch.isLetterOrDigit() } }
            .take(6)
            .toSet()
    }

    private fun isLikelyEphemeralMemory(normalizedText: String): Boolean {
        val markers = listOf(
            "today",
            "right now",
            "currently",
            "this morning",
            "this evening",
            "tonight",
            "at the moment",
            "feeling",
            "mood"
        )
        return markers.any { normalizedText.contains(it) }
    }

    fun toggleBookmark(id: Long, currentStatus: Boolean) {
        viewModelScope.launch {
            dao.toggleBookmark(id, !currentStatus)
        }
    }

    private suspend fun performConnectionCheck() {
        _isTesting.value = true
        _testResult.value = "Checking connection..."
        val cleanHost = currentHostIp.value.removePrefix("https://").removePrefix("http://").trim('/')
        val urlString = buildLocalUrl("/v1/models")
        val headers = buildLocalHeaders()

        try {
            val result = withContext(Dispatchers.IO) {
                try {
                    val requestBuilder = Request.Builder().url(urlString)
                    headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
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

    fun testConnection() {
        if (_isTesting.value) return
        viewModelScope.launch {
            performConnectionCheck()
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
            val encoded = encodeVector(vec)
            val saved = MessageEmbedding(
                messageId = msg.id,
                vector = encoded,
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
        val bytesToRead = count * 4
        val buffer = ByteBuffer.wrap(encoded, 0, bytesToRead).order(ByteOrder.LITTLE_ENDIAN)
        val out = FloatArray(count)
        for (i in 0 until count) {
            out[i] = buffer.float
        }
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

        val useLocalNet = isLocalEnabled.value
        val request = EmbeddingRequest(
            model = embeddingModelId,
            input = clean.take(2500)
        )

        val remoteVector = try {
            when {
                connectionMode.value == ConnectionMode.Phone -> {
                    val gson = Gson()
                    if (useLocalNet) {
                        val rr = com.example.forgeint.RemoteRequest(
                            url = buildLocalUrl("/v1/embeddings"),
                            method = "POST",
                            headers = buildLocalHeaders(),
                            body = gson.toJson(request)
                        )
                        parseEmbeddingFromJson(phoneCommunicator.sendRequest(rr))
                    } else {
                        val currentKey = if (isCustomApiKeyEnabled.value && apiKey.value.isNotBlank()) apiKey.value else openRouterKey
                        val rr = com.example.forgeint.RemoteRequest(
                            url = "https://openrouter.ai/api/v1/embeddings",
                            method = "POST",
                            headers = mapOf(
                                "Authorization" to "Bearer $currentKey",
                                "HTTP-Referer" to "https://forgeint.app",
                                "X-Title" to "ForgeInt"
                            ),
                            body = gson.toJson(request)
                        )
                        parseEmbeddingFromJson(phoneCommunicator.sendRequest(rr))
                    }
                }
                useLocalNet -> {
                    apiService.embeddingsLocal(
                        buildLocalUrl("/v1/embeddings"),
                        buildLocalHeaders(),
                        request
                    ).data.firstOrNull()?.embedding?.map { it.toFloat() }?.toFloatArray()
                }
                else -> {
                    val currentKey = if (isCustomApiKeyEnabled.value && apiKey.value.isNotBlank()) apiKey.value else openRouterKey
                    apiService.embeddingsOpenRouter(
                        "Bearer $currentKey",
                        "https://forgeint.app",
                        "ForgeInt",
                        request
                    ).data.firstOrNull()?.embedding?.map { it.toFloat() }?.toFloatArray()
                }
            }
        } catch (_: Exception) {
            null
        }

        return normalizeVectorSafe(remoteVector ?: fallbackHashEmbeddingSafe(clean, fallbackEmbeddingDim))
    }

    private fun parseEmbeddingFromJson(raw: String?): FloatArray? {
        if (raw.isNullOrBlank()) return null
        return try {
            val root = JSONObject(raw)
            val data = root.optJSONArray("data") ?: return null
            if (data.length() <= 0) return null
            val first = data.optJSONObject(0) ?: return null
            val embedding = first.optJSONArray("embedding") ?: return null
            val vec = FloatArray(embedding.length())
            for (i in 0 until embedding.length()) {
                vec[i] = embedding.optDouble(i, 0.0).toFloat()
            }
            vec
        } catch (_: Exception) {
            null
        }
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

    private suspend fun performWebSearch(query: String): String {
        return withContext(Dispatchers.IO) {
            val key = tavilyApiKey.trim()
            val keyConfigured = key.startsWith("tvly-") &&
                !key.contains("REPLACE_WITH_YOUR_TAVILY_API_KEY", ignoreCase = true)
            if (!keyConfigured) {
                return@withContext performDuckDuckGoSearch(
                    query,
                    priorError = "Tavily API key missing. Set tavilyApiKey in GeminiViewModel."
                )
            }
            performTavilySearch(query, key)
        }
    }

    private suspend fun performWebSearchByConnection(query: String): String {
        // Use same provider in both modes for deterministic behavior.
        return performWebSearch(query)
    }

    private suspend fun performTavilySearch(query: String, key: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = JSONObject()
                    .put("query", query)
                    .put("topic", "general")
                    .put("search_depth", "basic")
                    .put("max_results", 5)
                    .put("include_answer", "advanced")
                    .put("include_raw_content", false)
                    .put("include_images", false)
                    .put("include_image_descriptions", false)
                    .toString()

                val request = Request.Builder()
                    .url("https://api.tavily.com/search")
                    .addHeader("Authorization", "Bearer $key")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val bodyStr = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    return@withContext performDuckDuckGoSearch(
                        query,
                        priorError = "Tavily HTTP ${response.code}: ${bodyStr.take(240)}"
                    )
                }

                val json = JSONObject(bodyStr)
                val answer = json.optString("answer", "")
                val sourceResults = json.optJSONArray("results") ?: JSONArray()
                val normalizedResults = JSONArray()

                for (i in 0 until sourceResults.length()) {
                    val item = sourceResults.optJSONObject(i) ?: continue
                    normalizedResults.put(
                        JSONObject()
                            .put("title", item.optString("title"))
                            .put("url", item.optString("url"))
                            .put("content", item.optString("content"))
                            .put("score", item.optDouble("score", 0.0))
                            .put("favicon", item.optString("favicon"))
                    )
                }

                JSONObject()
                    .put("query", query)
                    .put("summary", answer)
                    .put("result_count", normalizedResults.length())
                    .put("results", normalizedResults)
                    .put("source", "tavily")
                    .put("request_id", json.optString("request_id", ""))
                    .put("response_time", json.optString("response_time", ""))
                    .toString()
            } catch (e: Exception) {
                performDuckDuckGoSearch(query, priorError = "Tavily request failed: ${e.message}")
            }
        }
    }

    private fun parseDuckTopicsInto(
        topics: JSONArray?,
        out: JSONArray,
        maxResults: Int
    ) {
        if (topics == null) return
        for (i in 0 until topics.length()) {
            if (out.length() >= maxResults) return
            val item = topics.optJSONObject(i) ?: continue
            val nested = item.optJSONArray("Topics")
            if (nested != null) {
                parseDuckTopicsInto(nested, out, maxResults)
                continue
            }
            val text = item.optString("Text")
            val url = item.optString("FirstURL")
            if (text.isBlank()) continue
            out.put(
                JSONObject()
                    .put("title", text.take(96))
                    .put("url", url)
                    .put("content", text)
            )
        }
    }

    private suspend fun performDuckDuckGoSearch(query: String, priorError: String? = null): String {
        return withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = "https://api.duckduckgo.com/?q=$encoded&format=json&no_redirect=1&no_html=1&skip_disambig=1"
                val request = Request.Builder().url(url).get().build()
                val response = okHttpClient.newCall(request).execute()
                val bodyStr = response.body?.string().orEmpty()

                val json = JSONObject(bodyStr)
                val summary = json.optString("AbstractText", "")
                val heading = json.optString("Heading", "")
                val abstractUrl = json.optString("AbstractURL", "")
                val results = JSONArray()

                if (summary.isNotBlank()) {
                    results.put(
                        JSONObject()
                            .put("title", if (heading.isBlank()) "DuckDuckGo Summary" else heading)
                            .put("url", abstractUrl)
                            .put("content", summary)
                    )
                }
                parseDuckTopicsInto(json.optJSONArray("RelatedTopics"), results, maxResults = 5)

                JSONObject()
                    .put("query", query)
                    .put("summary", summary)
                    .put("result_count", results.length())
                    .put("results", results)
                    .put("source", "duckduckgo_fallback")
                    .put("fallback_reason", priorError ?: "")
                    .toString()
            } catch (e: Exception) {
                JSONObject()
                    .put("query", query)
                    .put("summary", "")
                    .put("result_count", 0)
                    .put("results", JSONArray())
                    .put("error", "Search failed: ${e.message}")
                    .put("fallback_reason", priorError ?: "")
                    .toString()
            }
        }
    }

    private fun getWebSearchTools(): List<Tool> {
        return listOf(
            Tool(
                "function",
                FunctionDetail(
                    "web_search",
                    "Search the web for current events or facts",
                    mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "query" to mapOf(
                                "type" to "string",
                                "description" to "The search query"
                            )
                        ),
                        "required" to listOf("query")
                    )
                )
            )
        )
    }

    private fun extractToolQuery(argumentsRaw: String?, fallbackQuery: String): String {
        val raw = argumentsRaw?.trim().orEmpty()
        if (raw.isBlank()) return fallbackQuery

        try {
            val obj = JSONObject(raw)
            val fromJson = obj.optString("query", "").trim()
            if (fromJson.isNotEmpty()) return fromJson
        } catch (_: Exception) {
        }

        val quotedMatch = Regex("\"query\"\\s*:\\s*\"([^\"]+)\"").find(raw)?.groupValues?.getOrNull(1)?.trim()
        if (!quotedMatch.isNullOrEmpty()) return quotedMatch

        val looseMatch = Regex("query\\s*[:=]\\s*['\"]?(.+?)['\"]?$").find(raw)?.groupValues?.getOrNull(1)?.trim()
        if (!looseMatch.isNullOrEmpty()) return looseMatch

        return fallbackQuery
    }

    private suspend fun checkToolsAndComplete(
        apiMessages: MutableList<ApiMessage>,
        modelToUse: String,
        maxTokensValue: Int,
        useLocalEndpoint: Boolean,
        fallbackQuery: String
    ): String? {
        val toolCheckRequest = ChatRequest(
            model = modelToUse,
            messages = apiMessages,
            stream = false,
            max_tokens = maxTokensValue,
            tools = getWebSearchTools(),
            tool_choice = "auto"
        )

        val blockingResponse: ChatResponse? = if (useLocalEndpoint) {
            val urlString = buildLocalUrl("/v1/chat/completions")
            val headers = buildLocalHeaders()
            if (connectionMode.value == ConnectionMode.Phone) {
                val remoteRequest = com.example.forgeint.RemoteRequest(
                    url = urlString,
                    method = "POST",
                    headers = headers,
                    body = Gson().toJson(toolCheckRequest)
                )
                val jsonStr = phoneCommunicator.sendRequest(remoteRequest)
                if (jsonStr != null) try {
                    Gson().fromJson(jsonStr, ChatResponse::class.java)
                } catch (_: Exception) {
                    null
                } else null
            } else {
                apiService.chatLocalBlocking(urlString, headers, toolCheckRequest)
            }
        } else {
            if (connectionMode.value == ConnectionMode.Phone) {
                val currentKey = if (isCustomApiKeyEnabled.value && apiKey.value.isNotBlank()) apiKey.value else openRouterKey
                val headers = mapOf(
                    "Authorization" to "Bearer $currentKey",
                    "HTTP-Referer" to "https://forgeint.app",
                    "X-Title" to "ForgeInt"
                )
                val remoteRequest = com.example.forgeint.RemoteRequest(
                    url = "https://openrouter.ai/api/v1/chat/completions",
                    method = "POST",
                    headers = headers,
                    body = Gson().toJson(toolCheckRequest)
                )
                val jsonStr = phoneCommunicator.sendRequest(remoteRequest)
                if (jsonStr != null) try {
                    Gson().fromJson(jsonStr, ChatResponse::class.java)
                } catch (_: Exception) {
                    null
                } else null
            } else {
                val currentKey = if (isCustomApiKeyEnabled.value && apiKey.value.isNotBlank()) apiKey.value else openRouterKey
                apiService.chatOpenRouterBlocking(
                    "Bearer $currentKey",
                    "https://forgeint.app",
                    "ForgeInt",
                    toolCheckRequest
                )
            }
        }

        val choice = blockingResponse?.choices?.firstOrNull()
        val toolCall = choice?.message?.tool_calls?.firstOrNull()
        if (toolCall?.function?.name == "web_search") {
            _isWebSearching.value = true
            val query = extractToolQuery(toolCall.function.arguments, fallbackQuery)
            val searchResult = try {
                if (query.isBlank()) {
                    "Search failed: missing query argument."
                } else {
                    performWebSearchByConnection(query)
                }
            } finally {
                _isWebSearching.value = false
            }

            apiMessages.add(choice.message)
            apiMessages.add(
                ApiMessage(
                    role = "tool",
                    content = searchResult,
                    tool_call_id = toolCall.id,
                    name = "web_search"
                )
            )

            return if (useLocalEndpoint) {
                callLmStudioStream(apiMessages, modelToUse, maxTokensValue)
            } else {
                callOpenRouterStream(apiMessages, modelToUse, maxTokensValue)
            }
        }

        if (!choice?.message?.content.isNullOrBlank()) {
            return choice?.message?.content
        }
        _isWebSearching.value = false
        return if (useLocalEndpoint) {
            callLmStudioStream(apiMessages, modelToUse, maxTokensValue)
        } else {
            callOpenRouterStream(apiMessages, modelToUse, maxTokensValue)
        }
    }

    private suspend fun generateResponse(chatId: Long, titleSeedUserPrompt: String? = null) {
        setLoadingState(true)
        _streamingMessage.value = ""

        val currentId = selectedPersonaId.value
        val persona = allPersonas.value.find { it.id == currentId } ?: Personas.findById(currentId)

        val modelToUse = currentModelId.value.trim()
        val useLocal = isLocalEnabled.value
        val webSearch = _isWebSearchEnabled.value
        // Pruning integration: Fetch more messages (e.g., 50)
        val rawHistory = dao.getMessages(chatId, 50).first().reversed()

        runMemoryMaintenance(chatId, rawHistory)
        
        // Only prune if the model has been swapped mid-session to save tokens/context on the new model.
        // Otherwise, trust the model's native context window (sending up to 50 messages).
        val conversationHistory = if (sessionStartModelId != null && sessionStartModelId != currentModelId.value) {
            ContextPruner.pruneMessages(rawHistory, maxContextMessages = 15)
        } else {
            rawHistory
        }
        val latestUserPrompt = rawHistory.lastOrNull { it.isUser }?.text.orEmpty()
        val relevantConversationContext = buildRelevantConversationContext(chatId, latestUserPrompt)

        val traits = traitDao.traitDao().getAllTraits()
        val ltm = traits.filter { it.type == MemoryType.LONG_TERM }
        val stm = traits.filter { it.type == MemoryType.SHORT_TERM }

        val memoryContext = StringBuilder().apply {
            if (ltm.isNotEmpty()) {
                append("\n[USER LONG-TERM MEMORY (Stable Facts)]:\n")
                ltm.forEach { append("- ${it.content}\n") }
            }
            if (stm.isNotEmpty()) {
                append("\n[USER SHORT-TERM MEMORY (Current Context/Mood/Ephemeral)]:\n")
                stm.forEach { append("- ${it.content}\n") }
            }
        }.toString()

        val tokenToWordFactor = 0.75
        val maxTokensValue = when (messageLength.value) {
            "Shorter" -> 312
            "Normal" -> 1800
            "Longer" -> 2042
            else -> 1024
        }
        val approxWordLimit = (maxTokensValue * tokenToWordFactor).toInt()
        val memoryInstruction = "\n\n[MEMORY_PROTOCOL]: The above memory sections contain facts about the user. LONG-TERM memory is stable, while SHORT-TERM memory reflects current status or mood. If SHORT-TERM memory contradicts LONG-TERM memory, trust the user's latest message or current context. Use this info to personalize responses naturally."

        // Unique Session ID to bust KV Cache on Local Servers
        val sessionTag = "\n\n[SESSION_ID: $chatId]"

        val fullSystemPrompt = StringBuilder()
            .append(persona.systemInstruction)
            .append(memoryContext)
            .append(relevantConversationContext)
            .append(memoryInstruction)
            .append(sessionTag)
            .append("\n\n[CONSTRAINTS]: Your response must be concise. Aim for a maximum of approximately $approxWordLimit words.")
            .toString()

        try {
            val fullResponse = if (useLocal) {
                val apiMessages = mutableListOf<ApiMessage>()
                apiMessages.add(ApiMessage("system", fullSystemPrompt, null))
                conversationHistory.forEach { message ->
                    apiMessages.add(ApiMessage(if (message.isUser) "user" else "assistant", message.text.trim(), null))
                }
                if (webSearch) {
                    checkToolsAndComplete(
                        apiMessages,
                        modelToUse,
                        maxTokensValue,
                        useLocalEndpoint = true,
                        fallbackQuery = latestUserPrompt
                    )
                } else {
                    callLmStudioStream(apiMessages, modelToUse, maxTokensValue)
                }
            } else {
                val apiMessages = mutableListOf<ApiMessage>()
                apiMessages.add(ApiMessage("system", fullSystemPrompt, null))
                conversationHistory.forEach { message ->
                    apiMessages.add(ApiMessage(if (message.isUser) "user" else "assistant", message.text.trim(), null))
                }

                if (webSearch) {
                    checkToolsAndComplete(
                        apiMessages,
                        modelToUse,
                        maxTokensValue,
                        useLocalEndpoint = false,
                        fallbackQuery = latestUserPrompt
                    )
                } else {
                    callOpenRouterStream(apiMessages, modelToUse, maxTokensValue)
                }
            }

            if (!fullResponse.isNullOrBlank()) {
                val assistantMessageId = dao.insertMessage(Message(conversationId = chatId, text = fullResponse, isUser = false))
                queueEmbeddingForMessage(assistantMessageId, fullResponse)
                if (!titleSeedUserPrompt.isNullOrBlank()) {
                    val aiTitle = generateConversationTitle(
                        userPrompt = titleSeedUserPrompt,
                        assistantResponse = fullResponse
                    )
                    if (!aiTitle.isNullOrBlank()) {
                        dao.updateConversationSummary(chatId, aiTitle)
                    }
                }
            } else {
                val assistantMessageId = dao.insertMessage(Message(conversationId = chatId, text = "No response generated.", isUser = false))
                queueEmbeddingForMessage(assistantMessageId, "No response generated.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!stopRequested) {
                val assistantMessageId = dao.insertMessage(Message(conversationId = chatId, text = "Error: ${e.message}", isUser = false))
                queueEmbeddingForMessage(assistantMessageId, "Error: ${e.message}")
            }
        } finally {
            _isWebSearching.value = false
            _isThinking.value = false
            _streamingMessage.value = null
            setLoadingState(false)
            stopRequested = false
            activeResponseJob = null
        }
    }

    private suspend fun generateConversationTitle(userPrompt: String, assistantResponse: String): String? {
        val modelToUse = currentModelId.value.trim()
        val useLocal = isLocalEnabled.value
        val prompt = """
            Create a concise title for this conversation.
            Rules:
            - 3 to 7 words
            - No quotes
            - No punctuation except hyphen if needed
            - Return only the title

            User: ${userPrompt.trim().take(320)}
            Assistant: ${assistantResponse.trim().take(480)}
        """.trimIndent()

        val request = ChatRequest(
            model = modelToUse,
            messages = listOf(ApiMessage("user", prompt, null)),
            stream = false,
            max_tokens = 24,
            temperature = 0.2
        )

        val rawTitle: String? = try {
            if (useLocal) {
                val urlString = buildLocalUrl("/v1/chat/completions")
                val headers = buildLocalHeaders()

                if (connectionMode.value == ConnectionMode.Phone) {
                    val remoteRequest = com.example.forgeint.RemoteRequest(
                        url = urlString,
                        method = "POST",
                        headers = headers,
                        body = Gson().toJson(request)
                    )
                    val jsonStr = phoneCommunicator.sendRequest(remoteRequest)
                    if (jsonStr != null) {
                        try {
                            Gson().fromJson(jsonStr, ChatResponse::class.java)
                                ?.choices?.firstOrNull()?.message?.content
                        } catch (_: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                } else {
                    apiService.chatLocalBlocking(urlString, headers, request)
                        .choices.firstOrNull()?.message?.content
                }
            } else {
                val currentKey = if (isCustomApiKeyEnabled.value && apiKey.value.isNotBlank()) apiKey.value else openRouterKey
                if (connectionMode.value == ConnectionMode.Phone) {
                    val headers = mapOf(
                        "Authorization" to "Bearer $currentKey",
                        "HTTP-Referer" to "https://forgeint.app",
                        "X-Title" to "ForgeInt"
                    )
                    val remoteRequest = com.example.forgeint.RemoteRequest(
                        url = "https://openrouter.ai/api/v1/chat/completions",
                        method = "POST",
                        headers = headers,
                        body = Gson().toJson(request)
                    )
                    val jsonStr = phoneCommunicator.sendRequest(remoteRequest)
                    if (jsonStr != null) {
                        try {
                            Gson().fromJson(jsonStr, ChatResponse::class.java)
                                ?.choices?.firstOrNull()?.message?.content
                        } catch (_: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                } else {
                    apiService.chatOpenRouterBlocking(
                        "Bearer $currentKey",
                        "https://forgeint.app",
                        "ForgeInt",
                        request
                    ).choices.firstOrNull()?.message?.content
                }
            }
        } catch (_: Exception) {
            null
        }

        return rawTitle
            ?.takeUnless { it.startsWith("Error:", ignoreCase = true) }
            ?.lineSequence()
            ?.firstOrNull { it.isNotBlank() }
            ?.replace("\"", "")
            ?.replace("'", "")
            ?.replace(Regex("[^A-Za-z0-9\\-\\s]"), "")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.take(56)
            ?.takeIf { it.length >= 3 }
    }

    private suspend fun generateContextCarryoverSummary(
        conversationTitle: String,
        history: List<Message>,
        telemetry: VramTelemetry?
    ): String? {
        val modelToUse = currentModelId.value.trim()
        val useLocal = isLocalEnabled.value
        val summaryRequestMessages = mutableListOf<ApiMessage>().apply {
            add(
                ApiMessage(
                    "system",
                    """
                    Summarize the conversation so it can continue in a fresh chat with lower context usage.
                    Keep the output compact and practical.
                    Include:
                    - key user goals or unresolved questions
                    - important factual details and preferences
                    - current task status or next step
                    - anything the assistant must remember to continue accurately
                    Return plain text only.
                    """.trimIndent(),
                    null
                )
            )
            add(
                ApiMessage(
                    "user",
                    buildString {
                        appendLine("Conversation title: ${conversationTitle.ifBlank { "Untitled conversation" }}")
                        telemetry?.let { appendLine("VRAM trigger: ${it.describePressure()}") }
                        appendLine("Conversation transcript:")
                        history.forEach { message ->
                            append(if (message.isUser) "User: " else "Assistant: ")
                            appendLine(message.text.trim())
                        }
                    },
                    null
                )
            )
        }

        val request = ChatRequest(
            model = modelToUse,
            messages = summaryRequestMessages,
            stream = false,
            max_tokens = 500,
            temperature = 0.2
        )

        val rawSummary = executeBlockingChatRequest(request, useLocal)
            ?.choices
            ?.firstOrNull()
            ?.message
            ?.content
            ?.trim()
            .orEmpty()

        return rawSummary
            .takeIf { it.isNotBlank() }
            ?.takeUnless { it.startsWith("Error:", ignoreCase = true) }
    }

    private fun buildCarryoverSeedMessage(summary: String, telemetry: VramTelemetry?): String {
        return buildString {
            appendLine("[Automatic context rollover]")
            appendLine("This chat was continued in a fresh thread after sustained high VRAM usage.")
            telemetry?.let { appendLine("VRAM trigger: ${it.describePressure()}") }
            appendLine()
            append(summary.trim())
        }.trim()
    }

    private suspend fun executeBlockingChatRequest(
        request: ChatRequest,
        useLocal: Boolean
    ): ChatResponse? {
        return try {
            if (useLocal) {
                val urlString = buildLocalUrl("/v1/chat/completions")
                val headers = buildLocalHeaders()

                if (connectionMode.value == ConnectionMode.Phone) {
                    val remoteRequest = com.example.forgeint.RemoteRequest(
                        url = urlString,
                        method = "POST",
                        headers = headers,
                        body = Gson().toJson(request)
                    )
                    val jsonStr = phoneCommunicator.sendRequest(remoteRequest)
                    if (jsonStr != null) Gson().fromJson(jsonStr, ChatResponse::class.java) else null
                } else {
                    apiService.chatLocalBlocking(urlString, headers, request)
                }
            } else {
                val currentKey = if (isCustomApiKeyEnabled.value && apiKey.value.isNotBlank()) apiKey.value else openRouterKey
                if (connectionMode.value == ConnectionMode.Phone) {
                    val headers = mapOf(
                        "Authorization" to "Bearer $currentKey",
                        "HTTP-Referer" to "https://forgeint.app",
                        "X-Title" to "ForgeInt"
                    )
                    val remoteRequest = com.example.forgeint.RemoteRequest(
                        url = "https://openrouter.ai/api/v1/chat/completions",
                        method = "POST",
                        headers = headers,
                        body = Gson().toJson(request)
                    )
                    val jsonStr = phoneCommunicator.sendRequest(remoteRequest)
                    if (jsonStr != null) Gson().fromJson(jsonStr, ChatResponse::class.java) else null
                } else {
                    apiService.chatOpenRouterBlocking(
                        "Bearer $currentKey",
                        "https://forgeint.app",
                        "ForgeInt",
                        request
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Blocking chat request failed", e)
            null
        }
    }

    private suspend fun callLmStudioStream(messages: List<ApiMessage>, modelId: String, maxTokens: Int): String? {
        val urlString = buildLocalUrl("/v1/chat/completions")
        val headers = buildLocalHeaders()
        if (isTunnelHost(currentHostIp.value.removePrefix("https://").removePrefix("http://").trim('/').lowercase())) {
            headers["cf-terminate-connection"] = "true"
        }

        val request = ChatRequest(model = modelId, messages = messages, temperature = 0.7, stream = true, max_tokens = maxTokens)

        if (connectionMode.value == ConnectionMode.Phone) {
            val gson = Gson()
            val remoteRequest = com.example.forgeint.RemoteRequest(
                url = urlString,
                method = "POST",
                headers = headers,
                body = gson.toJson(request)
            )
            
            _streamingMessage.value = "Requesting via Phone..."
            val responseString = phoneCommunicator.sendRequest(remoteRequest) ?: ""
            
            // Treat the whole response as a single chunk for simplicity, or try to parse if it's JSON lines
            // Since our phone service returns the whole body string, we just dump it.
            // However, the existing 'processStream' expects SSE format (data: {...}).
            // If the phone returns the raw accumulated SSE stream, processStream *might* work if we wrap it.
            return processStream(responseString.toResponseBody("text/event-stream".toMediaType()))
        }

        val responseBody = apiService.chatLocalStream(urlString, headers, request)
        return processStream(responseBody)
    }

    private fun String.toResponseBody(mediaType: okhttp3.MediaType? = null): ResponseBody {
        return okio.Buffer().writeUtf8(this).asResponseBody(mediaType)
    }

    private suspend fun callOpenRouterStream(messages: List<ApiMessage>, modelId: String, maxTokens: Int): String? {
        val request = ChatRequest(model = modelId, messages = messages, stream = true,
            max_tokens = maxTokens)
        val currentKey = if (isCustomApiKeyEnabled.value && apiKey.value.isNotBlank()) apiKey.value else openRouterKey

        if (connectionMode.value == ConnectionMode.Phone) {
            val gson = Gson()
            val headers = mapOf(
                "Authorization" to "Bearer $currentKey",
                "HTTP-Referer" to "https://forgeint.app",
                "X-Title" to "ForgeInt"
            )
            
            val remoteRequest = com.example.forgeint.RemoteRequest(
                url = "https://openrouter.ai/api/v1/chat/completions",
                method = "POST",
                headers = headers,
                body = gson.toJson(request)
            )

            _streamingMessage.value = "Requesting via Phone..."
            val responseString = phoneCommunicator.sendRequest(remoteRequest) ?: ""
            return processStream(responseString.toResponseBody("text/event-stream".toMediaType()))
        }

        val responseBody = apiService.chatOpenRouterStream(
            "Bearer $currentKey",
            "https://forgeint.app",
            "ForgeInt",
            request
        )
        return processStream(responseBody)
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
    @SerializedName("temperature") val temperature: Double? = 0.7,
    @SerializedName("max_tokens") val max_tokens: Int? = null,
    @SerializedName("stream") val stream: Boolean? = false,
    @SerializedName("tools") val tools: List<Tool>? = null,
    @SerializedName("tool_choice") val tool_choice: String? = null
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
    @SerializedName("content") val content: String?,
    @SerializedName("tool_calls") val tool_calls: List<ToolCall>? = null,
    @SerializedName("tool_call_id") val tool_call_id: String? = null,
    @SerializedName("name") val name: String? = null
)

data class Tool(
    @SerializedName("type") val type: String,
    @SerializedName("function") val function: FunctionDetail
)

data class FunctionDetail(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("parameters") val parameters: Map<String, Any>
)

data class ToolCall(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("function") val function: FunctionCall
)

data class FunctionCall(
    @SerializedName("name") val name: String,
    @SerializedName("arguments") val arguments: String
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
