package com.example.forgeint.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weargemini.data.SettingsManager
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
import java.net.InetSocketAddress
import java.net.Socket
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.GzipSource
import okio.buffer
import java.util.concurrent.TimeUnit
import org.json.JSONObject

enum class LocalModelStatus {
    NotPresent,
    Downloading,
    Present
}

class GeminiViewModel(application: Application) : AndroidViewModel(application) {
    init {
        System.loadLibrary("forgeint_local")
    }

    external fun parseStreamChunkNative(rawData: String): String?

    private val dao = ChatDatabase.getDatabase(application).chatDao()
    private val settingsManager = SettingsManager(application)
    private val traitDao = ChatDatabase.getDatabase(application)

    private val localEngine = LocalInferenceEngine(application)
    private var isModelLoaded = false

    private val openRouterKey = "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"
    private val huggingFaceToken = "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"
    private val tavilyApiKey = "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"

    private val _streamingMessage = MutableStateFlow<String?>(null)
    val streamingMessage = _streamingMessage.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking = _isThinking.asStateFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        // Manual GZIP interceptor removed to allow OkHttp's transparent GZIP handling
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val appTheme = settingsManager.appTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Default")

    private val selectedPersonaId = settingsManager.selectedPersonaId
        .stateIn(viewModelScope, SharingStarted.Eagerly, "default")

    val isLiteMode = settingsManager.isLiteMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(4500), true)

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

    private val _isTesting = MutableStateFlow(false)
    val isTesting = _isTesting.asStateFlow()

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult = _testResult.asStateFlow()

    private val _messageLimit = MutableStateFlow(20)

    private val _isHistoryLoading = MutableStateFlow(false)
    val isHistoryLoading = _isHistoryLoading.asStateFlow()

    private val _isWebSearchEnabled = MutableStateFlow(false)
    val isWebSearchEnabled = _isWebSearchEnabled.asStateFlow()

    private val priorityPorts = listOf(1234, 5000, 8080, 8000, 3000, 11434)
    private val _localModelStatus = MutableStateFlow<LocalModelStatus>(LocalModelStatus.NotPresent)
    val localModelStatus = _localModelStatus.asStateFlow()

    private val _modelDownloadProgress = MutableStateFlow(0f)
    val modelDownloadProgress = _modelDownloadProgress.asStateFlow()

    private val localModelFile = File(application.filesDir, "gemma-2-2b-it-IQ2_XXS.gguf")
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels = _availableModels.asStateFlow()

    private var sessionStartModelId: String? = null

    fun toggleMemoryMonitor() {
        viewModelScope.launch {
            settingsManager.setMemoryMonitorEnabled(!memory_monitor.value)
        }
    }

    fun fetchAvailableModels() {
        viewModelScope.launch(Dispatchers.IO) {
            val host = currentHostIp.value
            val port = currentPort.value
            val cleanHost = host.removePrefix("https://").removePrefix("http://").trim('/')
            val isTunnel = cleanHost.contains("cloudflare") || cleanHost.contains("ngrok") || cleanHost.contains("loclx")

            val urlString = if (isTunnel) {
                "https://$cleanHost/v1/models"
            } else {
                "http://$cleanHost:$port/v1/models"
            }

            try {
                val request = Request.Builder().url(urlString).build()
                val response = okHttpClient.newCall(request).execute()
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
        checkLocalModelStatus()
        checkForEmulator()
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
        if (isModelLoaded) {
            localEngine.unloadModel()
            isModelLoaded = false
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
                    .header("Authorization", "Bearer $huggingFaceToken")
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
    fun setTheme(theme: String) = viewModelScope.launch { settingsManager.setAppTheme(theme) }
    fun setLiteMode(enabled: Boolean) = viewModelScope.launch { settingsManager.setLiteMode(enabled) }
    fun toggleLocalMode(enabled: Boolean) = viewModelScope.launch { settingsManager.setLocalEnabled(enabled) }
    fun setMessageLength(length: String) = viewModelScope.launch { settingsManager.setMessageLength(length) }
    fun updateApiKey(key: String) = viewModelScope.launch { settingsManager.setApiKey(key) }
    fun toggleCustomApiKey(enabled: Boolean) = viewModelScope.launch { settingsManager.setCustomApiKeyEnabled(enabled) }

    fun updateHostIp(newIp: String) = viewModelScope.launch {
        val cleanIp = newIp
            .replace("https://", "")
            .replace("http://", "")
            .replace("/", "")
            .trim()

        val newUrl = "http://$cleanIp"

        settingsManager.setHostIp(newUrl)

        _testResult.value = null
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

    fun startNewChat(prompt: String) {
        if (_isLoading.value) return

        viewModelScope.launch {
            sessionStartModelId = currentModelId.value
            _messageLimit.value = 20
            val newId = dao.insertConversation(Conversation(summary = prompt))
            _currentConversationId.value = newId
            dao.insertMessage(Message(conversationId = newId, text = prompt, isUser = true))
            generateResponse(newId)

            if ((!isLocalEnabled.value || !localModelFile.exists()) && !isLiteMode.value) {
                launch(Dispatchers.IO) { extractAndStoreTraits(prompt) }
            }
        }
    }

    fun continueChat(prompt: String) {
        if (_isLoading.value) return

        val chatId = _currentConversationId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertMessage(Message(conversationId = chatId, text =  prompt, isUser = true))

            if ((!isLocalEnabled.value || !localModelFile.exists()) && !isLiteMode.value) {
                launch(Dispatchers.IO) { extractAndStoreTraits(prompt) }
            }

            generateResponse(chatId)
        }
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
        if (isLiteMode.value) return
        if (userText.length < 10) return

        val modelToUse = currentModelId.value
        val prompt = MemoryPrompter.extractionPrompt + "\n\nUser Message: \"$userText\""

        val apiMessages = listOf(
            ApiMessage("system", "You are a background memory processor. Extract ALL specific, long-term facts about the user from the provided message. \n" +
                    "Categories to look for:\n" +
                    "- Hardware/Tech: Devices, GPUs, OS, software stacks, preferred tools.\n" +
                    "- Preferences: Music taste, gaming, food, aesthetics, workflow habits.\n" +
                    "- Personal: Name, age, location, profession, education level.\n" +
                    "- Routines: Gym schedules, study times, sleep patterns.\n" +
                    "- Entities: Project names, pet names, friend/family names mentioned.\n" +
                    "- Opinions: Strong views on specific topics (e.g., 'I hate Python').\n" +
                    "\n" +
                    "Format: Key | Value | Category\n" +
                    "Return multiple lines for multiple facts.\n" +
                    "If no new or significant information is present, return: NULL\n" +
                    "\n" +
                    "Examples:\n" +
                    "\"My S25 Ultra is fast\" -> S25 Ultra | Owned Device | Hardware\n" +
                    "\"Calculus is killing me\" -> Calculus | Currently Studying | Academics\n" +
                    "\"Need to hit legs today\" -> Leg Day | Workout Routine | Fitness\n" +
                    "\"Working on my Compose UI\" -> Jetpack Compose | Development Tool | Tech Stack\n" +
                    "\"I prefer dark mode everywhere\" -> Dark Mode | UI Preference | Aesthetics\n" +
                    "\n" +
                    "Rules:\n" +
                    "1. Extract implicit facts even if the user doesn't say \"Remember\".\n" +
                    "2. Use specific, uppercase Title Case for Keys.\n" +
                    "3. Keep Values concise but descriptive.\n" +
                    "4. Capture EVERYTHING that seems personal or contextually relevant.\n" +
                    "5. Do not hallucinate facts not present in the text."),
            ApiMessage("user", prompt)
        )

        try {
            val request = ChatRequest(model = modelToUse, messages = apiMessages, stream = false,
                max_tokens = 256)

            val responseText: String? = if (isLocalEnabled.value && !localModelFile.exists()) {
                val host = currentHostIp.value
                val port = currentPort.value
                val cleanHost = host.removePrefix("https://").removePrefix("http://").trim('/')
                val isTunnel = cleanHost.contains("cloudflare") || cleanHost.contains("ngrok")

                val urlString = if (isTunnel) {
                    "https://$cleanHost/v1/chat/completions"
                } else {
                    "http://$cleanHost:$port/v1/chat/completions"
                }

                val headers = HashMap<String, String>()
                if (isTunnel) {
                    headers["ngrok-skip-browser-warning"] = "true"
                    headers["User-Agent"] = "ForgeIntApp"
                }

                val response = apiService.chatLocalBlocking(urlString, headers, request)
                response.choices.firstOrNull()?.message?.content
            } else if (!isLocalEnabled.value) {
                val currentKey = if (isCustomApiKeyEnabled.value && apiKey.value.isNotBlank()) apiKey.value else openRouterKey
                val response = apiService.chatOpenRouterBlocking(
                    "Bearer $currentKey",
                    "https://forgeint.app",
                    "ForgeInt",
                    request
                )
                response.choices.firstOrNull()?.message?.content
            } else {
                null
            }

            responseText?.let { text ->
                if (!text.contains("NULL")) {
                    val existingKeys = traitDao.traitDao().getAllTraits().map { it.traitKey }.toMutableSet()
                    
                    text.lines().forEach { line ->
                        if (line.contains("|")) {
                            val parts = line.trim().split("|")
                            if (parts.size >= 3) {
                                val key = parts[0].trim()
                                val value = parts[1].trim()
                                val category = parts[2].trim()

                                if (key.isNotBlank() && value.isNotBlank()) {
                                    var uniqueKey = key
                                    var counter = 2
                                    while (existingKeys.contains(uniqueKey)) {
                                        uniqueKey = "$key ($counter)"
                                        counter++
                                    }
                                    existingKeys.add(uniqueKey)

                                    traitDao.traitDao().updateTrait(UserTrait(uniqueKey, value, category))
                                    println("MEMORY SAVED: $uniqueKey - $value")
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

            val isTunnel = cleanHost.contains("cloudflare") || cleanHost.contains("ngrok") || cleanHost.contains("loclx")
            val finalPort = if (isTunnel) {
                _testResult.value = "Tunnel detected. Skipping port scan..."
                ""
            } else {
                _testResult.value = "Scanning local network..."
                val scanned = scanAllPorts(cleanHost)
                if (scanned != null) {
                    settingsManager.setServerPort(scanned)
                    scanned
                } else {
                    currentPort.value
                }
            }

            val urlString = if (isTunnel) {
                "https://$cleanHost/v1/models"
            } else {
                "http://$cleanHost:$finalPort/v1/models"
            }

            try {
                val result = withContext(Dispatchers.IO) {
                    try {
                        val requestBuilder = Request.Builder().url(urlString)
                        if (isTunnel) {
                            requestBuilder.addHeader("ngrok-skip-browser-warning", "true")
                            requestBuilder.addHeader("User-Agent", "ForgeIntApp")
                        }
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

    private suspend fun scanAllPorts(host: String): String? = withContext(Dispatchers.IO) {
        for (port in priorityPorts) {
            if (isPortOpen(host, port)) return@withContext port.toString()
        }

        val maxPort = 65535
        val chunkSize = 500

        for (start in 1..maxPort step chunkSize) {
            val end = (start + chunkSize - 1).coerceAtMost(maxPort)
            _testResult.value = "Scanning ports $start - $end..."

            val deferredResults = (start..end).map { port ->
                async { if (isPortOpen(host, port)) port else null }
            }

            val found = deferredResults.awaitAll().filterNotNull().firstOrNull()
            if (found != null) return@withContext found.toString()
        }

        _testResult.value = "No local ports found."
        return@withContext null
    }

    private fun isPortOpen(host: String, port: Int): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), 150)
            socket.close()
            true
        } catch (e: Exception) {
            false
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

    private suspend fun performWebSearch(query: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject()
                json.put("query", query)
                json.put("include_answer", true)
                json.put("topic", "general")

                val requestBody = json.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("https://api.tavily.com/search")
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer $tavilyApiKey")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val bodyStr = response.body?.string()

                if (bodyStr != null) {
                    val responseJson = JSONObject(bodyStr)
                    val results = responseJson.optJSONArray("results")
                    val answer = responseJson.optString("answer", "")

                    val sb = StringBuilder()
                    if (answer.isNotEmpty()) sb.append("Summary: $answer\n\n")

                    if (results != null) {
                        for (i in 0 until results.length()) {
                            val item = results.getJSONObject(i)
                            sb.append("Title: ${item.optString("title")}\n")
                            sb.append("URL: ${item.optString("url")}\n")
                            sb.append("Content: ${item.optString("content")}\n\n")
                        }
                    }
                    sb.toString()
                } else {
                    "No results returned."
                }
            } catch (e: Exception) {
                "Search failed: ${e.message}"
            }
        }
    }

    private suspend fun generateResponse(chatId: Long) {
        _isLoading.value = true
        _streamingMessage.value = ""

        val persona = Personas.findById(selectedPersonaId.value)
        val modelToUse = currentModelId.value.trim()
        val useLocal = isLocalEnabled.value
        val webSearch = _isWebSearchEnabled.value && !useLocal
        // Pruning integration: Fetch more messages (e.g., 50)
        val rawHistory = dao.getMessages(chatId, 50).first().reversed()
        
        // Only prune if the model has been swapped mid-session to save tokens/context on the new model.
        // Otherwise, trust the model's native context window (sending up to 50 messages).
        val conversationHistory = if (sessionStartModelId != null && sessionStartModelId != currentModelId.value) {
            ContextPruner.pruneMessages(rawHistory, maxContextMessages = 15)
        } else {
            rawHistory
        }

        val traits = traitDao.traitDao().getAllTraits()
        val memoryContext = if (traits.isNotEmpty()) {
            "\n[KNOWN USER TRAITS]:\n" + traits.joinToString("\n") { "- ${it.category}: ${it.traitValue}" }
        } else ""
        val tokenToWordFactor = 0.75
        val maxTokensValue = when (messageLength.value) {
            "Shorter" -> 312
            "Normal" -> 1800
            "Longer" -> 2042
            else -> 1024
        }
        val approxWordLimit = (maxTokensValue * tokenToWordFactor).toInt()
        val memoryInstruction = "\n\n[MEMORY_PROTOCOL]: Never explicitly mention that you are accessing memory context or stored history. Do not bring up the user's interests, personal background, or established hobbies unless the user explicitly initiates a topic where that information is required for a logical or functional response."

        val fullSystemPrompt = StringBuilder()
            .append(persona.systemInstruction)
            .append(memoryContext)
            .append(memoryInstruction)
            .append("\n\n[CONSTRAINTS]: Your response must be concise. Aim for a maximum of approximately $approxWordLimit words.")
            .toString()

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
                        if (responseText.startsWith("Error:")) throw IOException(responseText) else responseText
                    }
                } else {
                    val apiMessages = mutableListOf<ApiMessage>()
                    apiMessages.add(ApiMessage("system", fullSystemPrompt, null))
                    conversationHistory.forEach { message ->
                        apiMessages.add(ApiMessage(if (message.isUser) "user" else "assistant", message.text.trim(), null))
                    }
                    callLmStudioStream(apiMessages, modelToUse, maxTokensValue)
                }
            } else {
                val apiMessages = mutableListOf<ApiMessage>()
                apiMessages.add(ApiMessage("system", fullSystemPrompt, null))
                conversationHistory.forEach { message ->
                    apiMessages.add(ApiMessage(if (message.isUser) "user" else "assistant", message.text.trim(), null))
                }

                if (webSearch) {
                    val tools = listOf(
                        Tool("function", FunctionDetail(
                            "web_search",
                            "Search the web for current events or facts",
                            mapOf(
                                "type" to "object",
                                "properties" to mapOf("query" to mapOf("type" to "string", "description" to "The search query")),
                                "required" to listOf("query")
                            )
                        ))
                    )

                    val toolCheckRequest = ChatRequest(
                        model = modelToUse,
                        messages = apiMessages,
                        stream = false,
                        max_tokens = maxTokensValue,
                        tools = tools,
                        tool_choice = "auto"
                    )

                    val currentKey = if (isCustomApiKeyEnabled.value && apiKey.value.isNotBlank()) apiKey.value else openRouterKey
                    val blockingResponse = apiService.chatOpenRouterBlocking(
                        "Bearer $currentKey", "https://forgeint.app", "ForgeInt", toolCheckRequest
                    )

                    val choice = blockingResponse.choices.firstOrNull()
                    val toolCalls = choice?.message?.tool_calls

                    if (toolCalls != null && toolCalls.isNotEmpty()) {
                        val toolCall = toolCalls.first()
                        if (toolCall.function.name == "web_search") {
                            _streamingMessage.value = "Searching the web..."
                            val args = JSONObject(toolCall.function.arguments)
                            val query = args.optString("query")

                            val searchResult = performWebSearch(query)

                            apiMessages.add(choice.message)
                            apiMessages.add(ApiMessage(
                                role = "tool",
                                content = searchResult,
                                tool_call_id = toolCall.id,
                                name = "web_search"
                            ))

                            callOpenRouterStream(apiMessages, modelToUse, maxTokensValue)
                        } else {
                            choice.message.content ?: ""
                        }
                    } else {
                        if (choice?.message?.content != null) {
                            choice.message.content
                        } else {
                            callOpenRouterStream(apiMessages, modelToUse, maxTokensValue)
                        }
                    }
                } else {
                    callOpenRouterStream(apiMessages, modelToUse, maxTokensValue)
                }
            }

            if (!fullResponse.isNullOrBlank()) {
                dao.insertMessage(Message(conversationId = chatId, text = fullResponse, isUser = false))
            } else {
                dao.insertMessage(Message(conversationId = chatId, text = "No response generated.", isUser = false))
            }
        } catch (e: Exception) {
            dao.insertMessage(Message(conversationId = chatId, text = "Error: ${e.message}", isUser = false))
            if (useLocal && localModelFile.exists()) {
                isModelLoaded = false
            }
        } finally {
            _streamingMessage.value = null
            _isLoading.value = false
        }
    }

    private suspend fun callLmStudioStream(messages: List<ApiMessage>, modelId: String, maxTokens: Int): String? {
        val host = currentHostIp.value
        val port = currentPort.value
        val cleanHost = host.removePrefix("https://").removePrefix("http://").trim('/')
        val isTunnel = cleanHost.contains("cloudflare") || cleanHost.contains("ngrok") || cleanHost.contains("loclx")

        val urlString = if (isTunnel) {
            "https://$cleanHost/v1/chat/completions"
        } else {
            "http://$cleanHost:$port/v1/chat/completions"
        }

        val headers = HashMap<String, String>()
        if (isTunnel) {
            headers["ngrok-skip-browser-warning"] = "true"
            headers["cf-terminate-connection"] = "true"
            headers["User-Agent"] = "ForgeIntApp"
        }

        val request = ChatRequest(model = modelId, messages = messages, temperature = 0.7, stream = true, max_tokens = maxTokens)

        val responseBody = apiService.chatLocalStream(urlString, headers, request)
        return processStream(responseBody)
    }

    private suspend fun callOpenRouterStream(messages: List<ApiMessage>, modelId: String, maxTokens: Int): String? {
        val request = ChatRequest(model = modelId, messages = messages, stream = true,
            max_tokens = maxTokens)
        val currentKey = if (isCustomApiKeyEnabled.value && apiKey.value.isNotBlank()) apiKey.value else openRouterKey
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
        withContext(Dispatchers.IO) {
            responseBody.source().use { source ->
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    val content = parseStreamChunkNative(line)
                    if (content != null) {
                        fullResponse.append(content)
                        val currentString = fullResponse.toString()

                        val thinkStart = currentString.indexOf("<think>")
                        val thinkEnd = currentString.indexOf("</think>")

                        if (thinkStart != -1 && thinkEnd == -1) {
                            // Thinking in progress
                            if (!_isThinking.value) _isThinking.value = true
                            // Show text before the tag
                            _streamingMessage.value = currentString.substring(0, thinkStart)
                        } else if (thinkStart != -1 && thinkEnd != -1) {
                            // Thinking finished
                            if (_isThinking.value) _isThinking.value = false
                            
                            val preThink = currentString.substring(0, thinkStart)
                            val postThink = currentString.substring(thinkEnd + 8) // </think> is 8 chars
                            _streamingMessage.value = preThink + postThink
                        } else {
                            // No thinking tags
                            _isThinking.value = false
                            _streamingMessage.value = currentString
                        }
                    }
                }
            }
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