package com.example.forgeint.presentation
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody
import java.util.concurrent.TimeUnit
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import retrofit2.http.GET
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.annotations.SerializedName
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.gestures.rememberDraggableState
import kotlinx.coroutines.Dispatchers
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.TimeText
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.lifecycle.viewModelScope
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.GzipSink
import okio.buffer
import java.io.IOException

private object GzipRequestCompression {
    val interceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val originalBody = originalRequest.body

        val compressedRequest =
            if (originalBody != null && originalRequest.header("Content-Encoding") == null) {
                originalRequest.newBuilder()
                    .header("Content-Encoding", "gzip")
                    .method(originalRequest.method, gzipRequestBody(originalBody))
                    .build()
            } else {
                originalRequest
            }

        chain.proceed(compressedRequest)
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
}

private object LiveDataRequestPolicy {
    val interceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val request = originalRequest.newBuilder()
            .header("Cache-Control", "no-cache, no-store, max-age=0")
            .header("Pragma", "no-cache")
            .build()

        chain.proceed(request)
    }
}

@Keep
data class HardwareNode(
    @SerializedName("id") val id: Int,
    @SerializedName("Text") val Text: String,
    @SerializedName("Children") val Children: List<HardwareNode>? = null,
    @SerializedName("Min") val Min: String? = null,
    @SerializedName("Value") val Value: String? = null,
    @SerializedName("Max") val Max: String? = null,
    @SerializedName("ImageURL") val ImageURL: String? = null,
    @SerializedName("SensorId") val SensorId: String? = null
)

@Keep
data class HardwareResponse(
    @SerializedName("Children") val Children: List<HardwareNode>?
)

data class G14Stats(
    val cpuTemp: String = "N/A",
    val cpuPower: String = "N/A",
    val cpuTempSensor: String = "",
    val cpuPowerSensor: String = "",
    val dgpuTemp: String = "N/A",
    val dgpuPower: String = "N/A",
    val dgpuTempSensor: String = "",
    val dgpuPowerSensor: String = "",
    val dgpuVram: String = "N/A",
    val dgpuVramTotal: String = "N/A",
    val igpuVram: String = "N/A",
    val igpuVramTotal: String = "N/A",
    val batteryWattage: String = "N/A",
    val ramUsed: String = "N/A",
    val ramTotal: String = "N/A",
    val sourceLabel: String = "Unknown",
    val endpointLabel: String = "",
    val updatedAtMillis: Long = 0L,
    val errorMessage: String? = null
)

data class VramTelemetry(
    val dgpuUsedMb: Float = 0f,
    val dgpuTotalMb: Float = 0f,
    val igpuUsedMb: Float = 0f,
    val igpuTotalMb: Float = 0f,
    val updatedAtMillis: Long = 0L
) {
    fun isUnderPressure(thresholdRatio: Float = 0.95f): Boolean {
        return usageRatio(dgpuUsedMb, dgpuTotalMb) >= thresholdRatio ||
            usageRatio(igpuUsedMb, igpuTotalMb) >= thresholdRatio
    }

    fun describePressure(): String {
        val parts = buildList {
            if (dgpuTotalMb > 0f) add("dGPU ${formatMemoryFromMb(dgpuUsedMb)} / ${formatMemoryFromMb(dgpuTotalMb)}")
            if (igpuTotalMb > 0f) add("iGPU ${formatMemoryFromMb(igpuUsedMb)} / ${formatMemoryFromMb(igpuTotalMb)}")
        }
        return if (parts.isEmpty()) "VRAM telemetry unavailable" else parts.joinToString(separator = ", ")
    }

    companion object {
        fun fromStats(stats: G14Stats): VramTelemetry {
            return VramTelemetry(
                dgpuUsedMb = parseMemoryToMb(stats.dgpuVram),
                dgpuTotalMb = parseMemoryToMb(stats.dgpuVramTotal),
                igpuUsedMb = parseMemoryToMb(stats.igpuVram),
                igpuTotalMb = parseMemoryToMb(stats.igpuVramTotal),
                updatedAtMillis = System.currentTimeMillis()
            )
        }

        private fun usageRatio(usedMb: Float, totalMb: Float): Float {
            if (usedMb <= 0f || totalMb <= 0f) return 0f
            return usedMb / totalMb
        }
    }
}

enum class SystemStabilityStatus {
    UNKNOWN,
    OPTIMAL,
    CAUTION,
    CRITICAL
}

data class SystemStabilityTelemetry(
    val status: SystemStabilityStatus = SystemStabilityStatus.UNKNOWN,
    val cpuTempC: Float = 0f,
    val gpuTempC: Float = 0f,
    val cpuPowerW: Float = 0f,
    val gpuPowerW: Float = 0f,
    val updatedAtMillis: Long = 0L
) {
    fun shouldRouteToCloud(): Boolean = status == SystemStabilityStatus.CRITICAL || status == SystemStabilityStatus.CAUTION

    fun describe(): String {
        return "status=$status cpu=${cpuTempC.toInt()}C gpu=${gpuTempC.toInt()}C cpuPower=${cpuPowerW.toInt()}W gpuPower=${gpuPowerW.toInt()}W"
    }
}

object HardwareTelemetryState {
    private val _vramTelemetry = MutableStateFlow(VramTelemetry())
    val vramTelemetry: StateFlow<VramTelemetry> = _vramTelemetry.asStateFlow()
    private val _systemStability = MutableStateFlow(SystemStabilityTelemetry())
    val systemStability: StateFlow<SystemStabilityTelemetry> = _systemStability.asStateFlow()

    fun update(stats: G14Stats) {
        _vramTelemetry.value = VramTelemetry.fromStats(stats)
        _systemStability.value = deriveSystemStabilityTelemetry(stats)
    }
}

@Keep
interface HardwareApi {
    @GET("data.json")
    suspend fun getHardwareStats(): HardwareResponse

    companion object {
        fun create(baseUrl: String, defaultHeaders: Map<String, String> = emptyMap()): HardwareApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val requestBuilder = chain.request().newBuilder()
                    defaultHeaders.forEach { (key, value) ->
                        requestBuilder.header(key, value)
                    }
                    chain.proceed(requestBuilder.build())
                }
                .addInterceptor(LiveDataRequestPolicy.interceptor)
                .addInterceptor(GzipRequestCompression.interceptor)
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(HardwareApi::class.java)
        }
    }
}
class HardwareRepository(
    private val api: HardwareApi,
    private val baseUrl: String,
    private val authToken: String = "",
    private val phoneCommunicator: PhoneCommunicator? = null
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(LiveDataRequestPolicy.interceptor)
        .addInterceptor(GzipRequestCompression.interceptor)
        .build()

    suspend fun fetchG14Status(usePhoneProxy: Boolean = false): G14Stats {
        val endpointUrl = buildHardwareUrl(baseUrl)
        val sourceLabel = if (usePhoneProxy && phoneCommunicator != null) "Phone relay" else "Direct"
        return try {
            val url = endpointUrl
            val headers = buildRequestHeaders(baseUrl)

            if (usePhoneProxy && phoneCommunicator != null) {
                val request = com.example.forgeint.RemoteRequest(
                    url = url,
                    method = "GET",
                    headers = headers
                )
                val responseStr = phoneCommunicator.sendRequest(request)

                if (responseStr == null || responseStr.startsWith("Error")) {
                    throw Exception(responseStr ?: "Unknown Proxy Error")
                }

                val gson = com.google.gson.Gson()
                val response = gson.fromJson(responseStr, HardwareResponse::class.java)
                parseNodes(response.Children ?: emptyList()).copy(
                    sourceLabel = sourceLabel,
                    endpointLabel = endpointUrl,
                    updatedAtMillis = System.currentTimeMillis()
                )
            } else if (headers.isNotEmpty()) {
                val request = Request.Builder()
                    .url(url)
                    .apply {
                        headers.forEach { (key, value) -> addHeader(key, value) }
                    }
                    .build()
                val response = httpClient.newCall(request).execute()
                val bodyStr = response.body?.string()
                response.close()

                if (bodyStr.isNullOrBlank()) {
                    throw Exception("Empty response")
                }

                val gson = com.google.gson.Gson()
                val parsed = gson.fromJson(bodyStr, HardwareResponse::class.java)
                parseNodes(parsed.Children ?: emptyList()).copy(
                    sourceLabel = sourceLabel,
                    endpointLabel = endpointUrl,
                    updatedAtMillis = System.currentTimeMillis()
                )
            } else {
                val response = api.getHardwareStats()
                // If response.Children is null, treat as empty
                parseNodes(response.Children ?: emptyList()).copy(
                    sourceLabel = sourceLabel,
                    endpointLabel = endpointUrl,
                    updatedAtMillis = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            G14Stats(
                sourceLabel = sourceLabel,
                endpointLabel = endpointUrl,
                updatedAtMillis = System.currentTimeMillis(),
                errorMessage = "${e.javaClass.simpleName}: ${e.message ?: "Unknown Error"}"
            )
        }
    }

    private fun buildHardwareUrl(baseUrl: String): String {
        val cleanBase = baseUrl.trimEnd('/')
        return "$cleanBase/data.json"
    }

    private fun buildRequestHeaders(baseUrl: String): Map<String, String> {
        val cleanHost = baseUrl
            .removePrefix("https://")
            .removePrefix("http://")
            .trim('/')
            .lowercase()

        val isTunnel = cleanHost.endsWith(".ts.net") ||
            cleanHost.contains("cloudflare") ||
            cleanHost.contains("ngrok") ||
            cleanHost.contains("loclx")

        val headers = mutableMapOf<String, String>()
        headers["Cache-Control"] = "no-cache, no-store, max-age=0"
        headers["Pragma"] = "no-cache"
        if (isTunnel) {
            headers["User-Agent"] = "ForgeIntApp"
            headers["cf-terminate-connection"] = "true"
            if (cleanHost.contains("ngrok")) {
                headers["ngrok-skip-browser-warning"] = "true"
            }
        }
        if (authToken.isNotBlank()) {
            headers["Authorization"] = "Bearer ${authToken.trim()}"
        }
        return headers
    }

    private fun parseNodes(nodes: List<HardwareNode>): G14Stats {
        var cpuTemp = "N/A"
        var cpuPower = "N/A"
        var cpuTempSensor = ""
        var cpuPowerSensor = ""
        var cpuTempPriority = -1
        var cpuPowerPriority = -1
        var dgpuTemp = "N/A"
        var dgpuPower = "N/A"
        var dgpuTempSensor = ""
        var dgpuPowerSensor = ""
        var dgpuPowerPriority = -1
        var dgpuVram = "N/A"
        var dgpuVramTotal = "N/A"
        var dgpuVramMetric = ""
        var igpuVram = "N/A"
        var igpuVramTotal = "N/A"
        var batt = "N/A"
        var ram = "N/A"
        var ramTotal = "N/A"
        var ramUsedMb: Float? = null
        var ramAvailMb: Float? = null
        var dgpuUsedMb: Float? = null
        var dgpuAvailMb: Float? = null
        var igpuUsedMb: Float? = null
        var igpuAvailMb: Float? = null

        fun containsTemperatureUnit(rawValue: String): Boolean {
            val normalized = rawValue.lowercase().replace("Â", "")
            return Regex("""\d+(?:\.\d+)?\s*(?:°\s*)?c\b""").containsMatchIn(normalized) ||
                normalized.contains("deg")
        }

        fun hasTemperatureSensorUnit(rawValue: String): Boolean {
            val normalized = rawValue
                .replace("\u00c2", "")
                .lowercase()
            return Regex("""\d+(?:\.\d+)?\s*(?:\u00b0\s*)?c\b""").containsMatchIn(normalized) ||
                normalized.contains("deg")
        }

        fun containsPowerUnit(rawValue: String): Boolean {
            val normalized = rawValue.lowercase()
            return Regex("""\d+(?:\.\d+)?\s*w(?:atts?)?\b""").containsMatchIn(normalized) ||
                normalized.contains("watt")
        }

        fun isLikelyStaticPowerSensor(sensorName: String, sensorId: String): Boolean {
            val staticTerms = listOf(
                "power limit",
                "power cap",
                "limit",
                "maximum",
                "max",
                "default",
                "design",
                "rated",
                "target",
                "allowed",
                "throttle"
            )
            return staticTerms.any { term ->
                sensorName.contains(term) || sensorId.contains(term)
            }
        }

        fun buildSensorLabel(node: HardwareNode): String {
            val text = node.Text.trim()
            val sensorId = node.SensorId?.trim().orEmpty()
            return when {
                text.isNotEmpty() && sensorId.isNotEmpty() -> "$text [$sensorId]"
                text.isNotEmpty() -> text
                sensorId.isNotEmpty() -> sensorId
                else -> "Unknown sensor"
            }
        }

        fun traverse(node: HardwareNode, parentType: String = "", parentPath: String = "") {
            var currentType = parentType
            val textLower = node.Text.lowercase()
            val imageLower = node.ImageURL?.lowercase() ?: ""
            val sensorIdLower = node.SensorId?.lowercase() ?: ""
            val currentPath = if (parentPath.isEmpty()) textLower else "$parentPath/$textLower"
            val isSensorIdGpu =
                sensorIdLower.contains("/amddpu/") ||
                    sensorIdLower.contains("/amdgpu/") ||
                    sensorIdLower.contains("/nvidiagpu/") ||
                    sensorIdLower.contains("/gpu/")
            val isSensorIdCpu =
                sensorIdLower.contains("/amdcpu/") ||
                    sensorIdLower.contains("/intelcpu/") ||
                    sensorIdLower.contains("/cpu/")
            
            // Detect Context
            if (isSensorIdGpu || imageLower.contains("nvidia") || textLower.contains("nvidia") || textLower.contains("geforce") || textLower.contains("rtx") || textLower.contains("gtx")) {
                currentType = "dGPU"
            } else if ((textLower.contains("radeon") || textLower.contains("intel uhd") || textLower.contains("iris") || (textLower.contains("graphics") && parentType == "")) && !isSensorIdCpu) {
                currentType = "iGPU"
            } else if (isSensorIdCpu || imageLower.contains("cpu") || textLower.contains("ryzen") || textLower.contains("intel core") || textLower.contains("processor") || textLower.contains("cpu")) {
                currentType = "CPU"
            } else if (textLower.contains("memory") && parentType == "" && !textLower.contains("gpu") && !textLower.contains("graphics")) {
                currentType = "RAM"
            } else if (textLower.contains("battery")) {
                currentType = "Battery"
            }

            val value = node.Value ?: "N/A"
            val valueLower = value.lowercase()
            val hasTempUnit = hasTemperatureSensorUnit(value)
            val hasPowerUnit = containsPowerUnit(value)
            if (value != "N/A" && value.any { it.isDigit() }) {
                val hasTempUnitNormalized = hasTempUnit
                val hasPowerUnitNormalized = hasPowerUnit
                val isCpuNamedSensor =
                    isSensorIdCpu ||
                    textLower.contains("cpu") ||
                    textLower.contains("ryzen") ||
                    textLower.contains("tdie") ||
                    textLower.contains("tctl") ||
                    textLower.contains("ccd")
                val isGpuNamedSensor =
                    isSensorIdGpu ||
                    textLower.contains("gpu") ||
                    textLower.contains("nvidia") ||
                    textLower.contains("geforce") ||
                    textLower.contains("rtx") ||
                    textLower.contains("gtx")
                val isCpuPath = isSensorIdCpu || currentPath.contains("cpu") || currentPath.contains("ryzen") || currentPath.contains("processor")
                val isGpuPath = isSensorIdGpu || currentPath.contains("gpu") || currentPath.contains("nvidia") || currentPath.contains("geforce") || currentPath.contains("rtx") || currentPath.contains("gtx")
                val isCpuLikeSensor = (currentType == "CPU" || isCpuNamedSensor || isCpuPath) && !isGpuNamedSensor && !isGpuPath
                val isDgpuLikeSensor = currentType == "dGPU" || ((isGpuNamedSensor || isGpuPath) && currentType != "iGPU")
                val isPerCorePower = textLower.contains("core #") ||
                    textLower.contains("core ") ||
                    textLower.contains("cores #") ||
                    sensorIdLower.contains("core #") ||
                    sensorIdLower.contains("/core/")
                val isPackageLikePower = textLower.contains("package") ||
                    textLower.contains("total package") ||
                    textLower.contains("cpu package") ||
                    textLower.contains("ppt") ||
                    sensorIdLower.contains("/package") ||
                    sensorIdLower.contains("/ppt")

                // LibreHardwareMonitor-oriented CPU selection with deterministic priority.
                if (isCpuLikeSensor) {
                    if ((textLower.contains("temperature") || textLower.contains("tdie") || textLower.contains("tctl") || textLower.contains("package") || textLower.contains("core max") || textLower.contains("ccd")) &&
                        (hasTempUnitNormalized || hasTempUnit) &&
                        !textLower.contains("distance")
                    ) {
                        val tempPriority = when {
                            textLower.contains("package") || textLower.contains("tdie") || textLower.contains("tctl") -> 4
                            textLower.contains("core max") || textLower.contains("average") -> 3
                            textLower.contains("ccd") -> 2
                            else -> 1
                        }
                        if (tempPriority >= cpuTempPriority) {
                            cpuTemp = value
                            cpuTempSensor = buildSensorLabel(node)
                            cpuTempPriority = tempPriority
                        }
                    }
                    if (
                        (hasPowerUnitNormalized || hasPowerUnit) &&
                        (
                            textLower.contains("power") ||
                            textLower.contains("ppt") ||
                            textLower.contains("cores") ||
                            textLower.contains("soc") ||
                            (textLower.contains("package") && (currentType == "CPU" || isCpuNamedSensor || isCpuPath)) ||
                            sensorIdLower.contains("/power")
                        )
                        && !isGpuNamedSensor
                        && !isGpuPath
                        && !isLikelyStaticPowerSensor(textLower, sensorIdLower)
                    ) {
                        val powerPriority = when {
                            isPackageLikePower -> 5
                            textLower.contains("soc") -> 3
                            isPerCorePower -> 1
                            sensorIdLower.contains("/power") -> 2
                            else -> 1
                        }
                        if (powerPriority >= cpuPowerPriority) {
                            cpuPower = value
                            cpuPowerSensor = buildSensorLabel(node)
                            cpuPowerPriority = powerPriority
                        }
                    }
                }
                if (
                    isDgpuLikeSensor &&
                    (hasPowerUnitNormalized || hasPowerUnit) &&
                    (
                        textLower.contains("power") ||
                        textLower.contains("tgp") ||
                        textLower.contains("board") ||
                        textLower.contains("package") ||
                        textLower.contains("total") ||
                        sensorIdLower.contains("/power")
                    ) &&
                    !isLikelyStaticPowerSensor(textLower, sensorIdLower)
                ) {
                    val powerPriority = when {
                        textLower.contains("tgp") || textLower.contains("board") -> 5
                        textLower.contains("total") || textLower.contains("package") -> 4
                        sensorIdLower.contains("/power") -> 3
                        textLower.contains("power") -> 2
                        else -> 1
                    }
                    if (powerPriority >= dgpuPowerPriority) {
                        dgpuPower = value
                        dgpuPowerSensor = buildSensorLabel(node)
                        dgpuPowerPriority = powerPriority
                    }
                }
                when (currentType) {
                    "CPU" -> {

                        if (value.contains("°C") || value.contains("C") || textLower.contains("temperature") || node.ImageURL?.contains("temperature") == true) {
                            if (!textLower.contains("distance")) {
                                if (cpuTemp == "N/A" || textLower.contains("package") || textLower.contains("tdie")) {
                                    cpuTemp = value
                                    cpuTempSensor = buildSensorLabel(node)
                                }
                            }
                        }
                        // Power: Check Unit "W" or Text "Power"
                        if ((hasPowerUnit || hasPowerUnitNormalized || textLower.contains("power") || sensorIdLower.contains("/power")) &&
                            !isLikelyStaticPowerSensor(textLower, sensorIdLower)
                        ) {
                            if (cpuPower == "N/A") {
                                cpuPower = value
                                cpuPowerSensor = buildSensorLabel(node)
                            }
                        }
                    }
                    "dGPU" -> {
                        if ((value.contains("°C") || node.ImageURL?.contains("temperature") == true) && (dgpuTemp == "N/A" || textLower.contains("core"))) dgpuTemp = value
                        if ((value.contains("MB") || value.contains("GB")) && textLower.contains("used")) {
                            val isShared = textLower.contains("shared")
                            val isDedicated = textLower.contains("dedicated")
                            if (!isShared) {
                                val shouldReplace = dgpuVram == "N/A" ||
                                    (isDedicated && !dgpuVramMetric.contains("dedicated"))
                                if (shouldReplace) {
                                    dgpuVram = value
                                    dgpuVramMetric = textLower
                                }
                                val usedMb = parseMemoryToMb(value)
                                if (usedMb > 0f) dgpuUsedMb = usedMb
                            }
                        }
                        if ((value.contains("MB") || value.contains("GB")) &&
                            (textLower.contains("available") || textLower.contains("free")) &&
                            !textLower.contains("shared")
                        ) {
                            val availMb = parseMemoryToMb(value)
                            if (availMb > 0f) dgpuAvailMb = availMb
                        }
                        if ((value.contains("MB") || value.contains("GB")) &&
                            (textLower.contains("total") || textLower.contains("capacity")) &&
                            !textLower.contains("shared")
                        ) {
                            dgpuVramTotal = value
                        }
                        if ((hasPowerUnit || hasPowerUnitNormalized || textLower.contains("power")) &&
                            !isLikelyStaticPowerSensor(textLower, sensorIdLower) &&
                            dgpuPower == "N/A"
                        ) {
                            dgpuPower = value
                            dgpuPowerSensor = buildSensorLabel(node)
                        }
                    }
                    "iGPU" -> {
                        if ((value.contains("MB") || value.contains("GB")) && textLower.contains("used")) {
                            igpuVram = value
                            val usedMb = parseMemoryToMb(value)
                            if (usedMb > 0f) igpuUsedMb = usedMb
                        }
                        if ((value.contains("MB") || value.contains("GB")) && (textLower.contains("available") || textLower.contains("free"))) {
                            val availMb = parseMemoryToMb(value)
                            if (availMb > 0f) igpuAvailMb = availMb
                        }
                        if ((value.contains("MB") || value.contains("GB")) && (textLower.contains("total") || textLower.contains("capacity"))) {
                            igpuVramTotal = value
                        }
                    }
                    "Battery" -> {
                        if (hasPowerUnit || hasPowerUnitNormalized || textLower.contains("rate")) batt = value
                    }
                    "RAM" -> {
                        if (textLower.contains("used") && (value.contains("GB") || value.contains("MB"))) {
                            ram = value
                            val usedMb = parseMemoryToMb(value)
                            if (usedMb > 0f) ramUsedMb = usedMb
                        }
                        if ((textLower.contains("available") || textLower.contains("free")) && (value.contains("GB") || value.contains("MB"))) {
                            val availMb = parseMemoryToMb(value)
                            if (availMb > 0f) ramAvailMb = availMb
                        }
                        if ((textLower.contains("total") || textLower.contains("capacity")) && (value.contains("GB") || value.contains("MB"))) {
                            ramTotal = value
                        }
                    }
                }
            }
            
            node.Children?.forEach { traverse(it, currentType, currentPath) }
        }

        nodes.forEach { traverse(it) }
        if (ramTotal == "N/A" && ramUsedMb != null && ramAvailMb != null) {
            ramTotal = formatMemoryFromMb(ramUsedMb!! + ramAvailMb!!)
        }
        if (dgpuVramTotal == "N/A" && dgpuUsedMb != null && dgpuAvailMb != null) {
            dgpuVramTotal = formatMemoryFromMb(dgpuUsedMb!! + dgpuAvailMb!!)
        }
        if (igpuVramTotal == "N/A" && igpuUsedMb != null && igpuAvailMb != null) {
            igpuVramTotal = formatMemoryFromMb(igpuUsedMb!! + igpuAvailMb!!)
        }
        return G14Stats(
            cpuTemp = cpuTemp,
            cpuPower = cpuPower,
            cpuTempSensor = cpuTempSensor,
            cpuPowerSensor = cpuPowerSensor,
            dgpuTemp = dgpuTemp,
            dgpuPower = dgpuPower,
            dgpuTempSensor = dgpuTempSensor,
            dgpuPowerSensor = dgpuPowerSensor,
            dgpuVram = dgpuVram,
            dgpuVramTotal = dgpuVramTotal,
            igpuVram = igpuVram,
            igpuVramTotal = igpuVramTotal,
            batteryWattage = batt,
            ramUsed = ram,
            ramTotal = ramTotal
        )
    }
}

class ForgeHardwareViewModel(
    private val repository: HardwareRepository,
    private val connectionModeFlow: StateFlow<ConnectionMode>? = null,
    private val onSustainedVramPressure: suspend (VramTelemetry) -> Boolean = { false },
    private val onSystemInstabilityChanged: suspend (Boolean, SystemStabilityTelemetry) -> Unit = { _, _ -> }
) : ViewModel() {
    private val _uiState = MutableStateFlow(G14Stats())
    val uiState: StateFlow<G14Stats> = _uiState.asStateFlow()
    
    private var pollingJob: kotlinx.coroutines.Job? = null
    private var isAppForeground = true
    private var vramPressureStartAt: Long? = null
    private var lastTriggeredPressureAt: Long? = null
    private var systemInstabilityStartAt: Long? = null
    private var isCloudRoutingActive = false

    private val foregroundPollIntervalMs = 3_000L
    private val backgroundPollIntervalMs = 60_000L
    private val sustainedVramPressureWindowMs = 5 * 60 * 1000L
    private val cautionInstabilityWindowMs = 3 * 60 * 1000L
    private val criticalInstabilityWindowMs = 90 * 1000L

    private suspend fun fetchSafely(useProxy: Boolean): G14Stats {
        return runCatching {
            repository.fetchG14Status(useProxy)
        }.getOrElse { error ->
            G14Stats(
                sourceLabel = if (useProxy) "Phone relay" else "Direct",
                updatedAtMillis = System.currentTimeMillis(),
                errorMessage = "Polling failed: ${error.javaClass.simpleName}: ${error.message ?: "Unknown Error"}"
            )
        }
    }

    private suspend fun publishStats(stats: G14Stats) {
        _uiState.value = stats
        runCatching {
            HardwareTelemetryState.update(stats)
            evaluateVramPressure(VramTelemetry.fromStats(stats))
            evaluateSystemStability(deriveSystemStabilityTelemetry(stats))
        }.onFailure { error ->
            _uiState.value = stats.copy(
                errorMessage = stats.errorMessage
                    ?: "Telemetry pipeline failed: ${error.javaClass.simpleName}: ${error.message ?: "Unknown Error"}"
            )
        }
    }

    fun setAppForeground(isForeground: Boolean) {
        isAppForeground = isForeground
    }

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val useProxy = connectionModeFlow?.value == ConnectionMode.Phone
                val stats = fetchSafely(useProxy)
                publishStats(stats)
                delay(if (isAppForeground) foregroundPollIntervalMs else backgroundPollIntervalMs)
            }
        }
    }
    
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val useProxy = connectionModeFlow?.value == ConnectionMode.Phone
            val stats = fetchSafely(useProxy)
            publishStats(stats)
        }
    }

    private suspend fun evaluateVramPressure(telemetry: VramTelemetry) {
        if (!telemetry.isUnderPressure()) {
            vramPressureStartAt = null
            lastTriggeredPressureAt = null
            return
        }

        val now = System.currentTimeMillis()
        val pressureStart = vramPressureStartAt ?: now.also { vramPressureStartAt = it }
        if (now - pressureStart < sustainedVramPressureWindowMs) return

        val alreadyTriggeredAt = lastTriggeredPressureAt
        if (alreadyTriggeredAt != null && alreadyTriggeredAt >= pressureStart) return

        val didHandle = runCatching { onSustainedVramPressure(telemetry) }.getOrDefault(false)
        if (didHandle) {
            lastTriggeredPressureAt = now
        }
    }

    private suspend fun evaluateSystemStability(telemetry: SystemStabilityTelemetry) {
        if (!telemetry.shouldRouteToCloud()) {
            systemInstabilityStartAt = null
            if (isCloudRoutingActive) {
                isCloudRoutingActive = false
                onSystemInstabilityChanged(false, telemetry)
            }
            return
        }

        val now = System.currentTimeMillis()
        val unstableSince = systemInstabilityStartAt ?: now.also { systemInstabilityStartAt = it }
        val requiredWindow = when (telemetry.status) {
            SystemStabilityStatus.CRITICAL -> criticalInstabilityWindowMs
            SystemStabilityStatus.CAUTION -> cautionInstabilityWindowMs
            else -> Long.MAX_VALUE
        }

        if (now - unstableSince < requiredWindow) return
        if (isCloudRoutingActive) return

        isCloudRoutingActive = true
        onSystemInstabilityChanged(true, telemetry)
    }
}

fun getTempColor(tempString: String): Color {
    val temp = tempString.replace("°C", "").trim().toFloatOrNull() ?: 0f
    return when {
        temp < 60f -> Color(0xFF4CAF50) // Green (Safe)
        temp < 80f -> Color(0xFFFF9800) // Orange (Warning)
        else -> Color(0xFFF44336)       // Red (Critical)
    }
}

fun parseValue(valueString: String): Float {
    return valueString.filter { it.isDigit() || it == '.' }.toFloatOrNull() ?: 0f
}

fun parseMemoryToMb(valueString: String): Float {
    val value = parseValue(valueString)
    return when {
        valueString.contains("GB", ignoreCase = true) -> value * 1024f
        valueString.contains("MB", ignoreCase = true) -> value
        else -> 0f
    }
}

fun deriveSystemStabilityTelemetry(stats: G14Stats): SystemStabilityTelemetry {
    val cpuTemp = parseValue(stats.cpuTemp)
    val gpuTemp = parseValue(stats.dgpuTemp)
    val cpuPower = parseValue(stats.cpuPower)
    val gpuPower = parseValue(stats.dgpuPower)
    val maxTemp = maxOf(cpuTemp, gpuTemp)
    val totalPower = cpuPower + gpuPower
    val hasThermalData = cpuTemp > 0f || gpuTemp > 0f
    val hasPowerData = cpuPower > 0f || gpuPower > 0f

    val status = when {
        !hasThermalData && !hasPowerData -> SystemStabilityStatus.UNKNOWN
        maxTemp >= 92f -> SystemStabilityStatus.CRITICAL
        maxTemp >= 85f -> SystemStabilityStatus.CAUTION
        totalPower >= 200f -> SystemStabilityStatus.CAUTION
        maxTemp >= 75f && totalPower >= 120f -> SystemStabilityStatus.CAUTION
        else -> SystemStabilityStatus.OPTIMAL
    }

    return SystemStabilityTelemetry(
        status = status,
        cpuTempC = cpuTemp,
        gpuTempC = gpuTemp,
        cpuPowerW = cpuPower,
        gpuPowerW = gpuPower,
        updatedAtMillis = System.currentTimeMillis()
    )
}

fun formatMemoryFromMb(valueMb: Float): String {
    return if (valueMb >= 1024f) String.format("%.1f GB", valueMb / 1024f) else String.format("%.0f MB", valueMb)
}

fun formatUsedTotal(used: String, total: String): String {
    return if (used == "N/A") "N/A" else if (total == "N/A") used else "$used / $total"
}

fun formatUsedTotalCompactMemory(used: String, total: String): String {
    if (used == "N/A") return "N/A"
    if (total == "N/A") return used
    val usedMb = parseMemoryToMb(used)
    val totalMb = parseMemoryToMb(total)
    if (usedMb <= 0f || totalMb <= 0f) return formatUsedTotal(used, total)

    // Keep watch labels compact and consistent.
    return if (totalMb >= 1024f) {
        String.format("%.1f/%.1fGB", usedMb / 1024f, totalMb / 1024f)
    } else {
        String.format("%.0f/%.0fMB", usedMb, totalMb)
    }
}

private fun buildPcActionUrl(host: String, port: String): String {
    val cleanHost = host.removePrefix("https://").removePrefix("http://").trim('/')
    val loweredHost = cleanHost.lowercase()
    val isTunnelHost =
        loweredHost.endsWith(".ts.net") ||
            loweredHost.contains("cloudflare") ||
            loweredHost.contains("ngrok") ||
            loweredHost.contains("loclx")

    return if (isTunnelHost) {
        "https://$cleanHost/action"
    } else {
        val hostWithoutPort = cleanHost.substringBefore(":")
        val resolvedPort = "1235"
        "http://$hostWithoutPort:${resolvedPort.ifBlank { "1235" }}/action"
    }
}

suspend fun sendPCCommand(host: String, port: String, command: String) {
    val client = OkHttpClient()
    val body = """{"command":"$command"}"""
        .toRequestBody("application/json".toMediaType())

    val request = Request.Builder()
        .url(buildPcActionUrl(host, port))
        .post(body)
        .addHeader("X-Token", "forge-secret-token")
        .build()

    withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("PC command failed: HTTP ${response.code}")
            }
        }
    }
}

private enum class PcPowerAction(
    val command: String,
    val title: String,
    val prompt: String,
    val confirmLabel: String,
    val confirmColor: Color
) {
    Sleep(
        command = "sleep",
        title = "Confirm sleep",
        prompt = "Put the PC to sleep now?",
        confirmLabel = "Sleep",
        confirmColor = Color(0xFF1E88E5)
    ),
    Shutdown(
        command = "shutdown",
        title = "Confirm shutdown",
        prompt = "Shutdown the PC now?",
        confirmLabel = "Shutdown",
        confirmColor = Color(0xFFD32F2F)
    )
}

private fun PcPowerAction.toPendingAction(): PendingPcAction {
    return PendingPcAction(
        command = command,
        title = title,
        prompt = prompt,
        confirmLabel = confirmLabel
    )
}

@Composable
private fun CircularActionButton(
    label: String,
    icon: ImageVector,
    backgroundColor: Color,
    iconTint: Color = Color.White,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.35f))
                .border(1.dp, iconTint.copy(alpha = if (enabled) 0.35f else 0.15f), CircleShape)
                .clickable(enabled = enabled) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) iconTint else iconTint.copy(alpha = 0.5f)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.caption3,
            color = if (enabled) Color.White else Color.LightGray
        )
    }
}

@Composable
private fun SwipeToConfirmRail(
    label: String,
    accentColor: Color,
    enabled: Boolean,
    onConfirmed: () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var confirmed by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val thumbSize = 28.dp
    val dragState = rememberDraggableState { delta ->
        if (enabled && !confirmed) {
            progress = (progress + delta / 220f).coerceIn(0f, 1f)
        }
    }

    LaunchedEffect(progress) {
        if (!confirmed && progress >= 0.85f) {
            confirmed = true
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onConfirmed()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (confirmed) "Confirmed" else "Swipe to confirm",
            style = MaterialTheme.typography.caption2,
            color = if (confirmed) accentColor else Color.LightGray,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(19.dp))
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = dragState,
                    enabled = enabled && !confirmed,
                    onDragStopped = {
                        if (!confirmed && progress < 0.85f) {
                            progress = 0f
                        }
                    }
                )
        ) {
            val thumbTravel = (maxWidth - thumbSize - 10.dp).coerceAtLeast(0.dp)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width((thumbSize + thumbTravel * progress).coerceAtLeast(thumbSize))
                    .background(accentColor.copy(alpha = 0.25f))
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.caption3,
                    color = Color.White.copy(alpha = if (confirmed) 0.5f else 1f)
                )
            }
            Box(
                modifier = Modifier
                    .offset(x = 8.dp + (thumbTravel * progress))
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(if (confirmed) accentColor else Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (confirmed) Icons.Default.CheckCircle else Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = if (confirmed) Color.White else accentColor
                )
            }
            if (!confirmed) {
                // Subtle trail hint for the swipe gesture.
                Text(
                    text = "Drag to the right",
                    style = MaterialTheme.typography.caption3,
                    color = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 42.dp))
                }
            }
        }
    }


@Composable
fun PcActionConfirmScreen(
    viewModel: GeminiViewModel,
    navController: NavController
) {
    val pendingAction by viewModel.pendingPcAction.collectAsStateWithLifecycle()
    val currentHost by viewModel.currentHostIp.collectAsStateWithLifecycle()
    val currentPort by viewModel.currentPort.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var isSendingCommand by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val action = remember(pendingAction) {
        pendingAction?.let { pending ->
            PcPowerAction.values().firstOrNull { it.command == pending.command }
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.clearPcActionRequest() }
    }

    LaunchedEffect(Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    AppScaffold {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x33D32F2F))
                        .border(1.dp, Color(0x88D32F2F), RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFFC107)
                        )
                        Column {
                            Text(
                                text = "Power action",
                                style = MaterialTheme.typography.caption2,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "This will affect the PC immediately after confirmation.",
                                style = MaterialTheme.typography.caption3,
                                color = Color(0xFFFFE0E0)
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "PC Action",
                    style = MaterialTheme.typography.caption2,
                    color = Color.Gray
                )
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background((action?.confirmColor ?: Color(0xFF1E88E5)).copy(alpha = 0.2f))
                            .border(1.dp, (action?.confirmColor ?: Color(0xFF1E88E5)).copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (action) {
                                PcPowerAction.Sleep -> Icons.Default.Brightness3
                                PcPowerAction.Shutdown -> Icons.Default.PowerSettingsNew
                                null -> Icons.Default.HelpOutline
                            },
                            contentDescription = action?.title ?: "PC action",
                            tint = action?.confirmColor ?: Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = action?.title ?: "Pending action",
                        style = MaterialTheme.typography.title3,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = action?.prompt ?: "No action selected.",
                        style = MaterialTheme.typography.caption2,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Host: $currentHost | Port: $currentPort",
                        style = MaterialTheme.typography.caption3,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    SwipeToConfirmRail(
                        label = action?.confirmLabel ?: "Confirm",
                        accentColor = action?.confirmColor ?: Color(0xFF1E88E5),
                        enabled = action != null && !isSendingCommand,
                        onConfirmed = {
                            val selectedAction = action ?: return@SwipeToConfirmRail
                            coroutineScope.launch {
                                isSendingCommand = true
                                statusMessage = null
                                try {
                                    sendPCCommand(currentHost, currentPort, selectedAction.command)
                                    viewModel.clearPcActionRequest()
                                    navController.popBackStack()
                                } catch (e: Exception) {
                                    statusMessage = e.message ?: "Unknown error"
                                } finally {
                                    isSendingCommand = false
                                }
                            }
                        }
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.clearPcActionRequest()
                        navController.popBackStack()
                    },
                    enabled = !isSendingCommand,
                    colors = ButtonDefaults.primaryButtonColors(backgroundColor = Color(0xFF4B4B4B)),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Cancel")
                }
            }

            if (action == null) {
                item {
                    Text(
                        text = "No pending action found.",
                        style = MaterialTheme.typography.caption3,
                        color = Color(0xFFFFCC80),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            if (!statusMessage.isNullOrBlank()) {
                item {
                    Text(
                        text = "Failed: $statusMessage",
                        style = MaterialTheme.typography.caption3,
                        color = Color(0xFFFF8A80),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun G14WatchMonitor(
    viewModel: ForgeHardwareViewModel,
    onRequestAction: (PendingPcAction) -> Unit
) {
    val stats by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.setAppForeground(true)
                Lifecycle.Event.ON_STOP -> viewModel.setAppForeground(false)
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        viewModel.startPolling()
        onDispose { viewModel.stopPolling() }
    }

    AppScaffold {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            item {
                Text(
                    text = "Hardware Monitor",
                    style = MaterialTheme.typography.caption2,
                    color = Color.Gray
                )
            }

            item {
                val endpointText = stats.endpointLabel.ifBlank { "Endpoint unavailable" }
                Text(
                    text = "${stats.sourceLabel} | $endpointText",
                    style = MaterialTheme.typography.caption3,
                    color = Color.LightGray,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            if (stats.cpuPowerSensor.isNotBlank() || stats.dgpuPowerSensor.isNotBlank()) {
                item {
                    val sensorSummary = buildList {
                        if (stats.cpuPowerSensor.isNotBlank()) add("CPU PWR: ${stats.cpuPowerSensor}")
                        if (stats.dgpuPowerSensor.isNotBlank()) add("GPU PWR: ${stats.dgpuPowerSensor}")
                    }.joinToString(separator = " | ")
                    Text(
                        text = sensorSummary,
                        style = MaterialTheme.typography.caption3,
                        color = Color(0xFFFFCC80),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
            
            if (stats.errorMessage != null) {
                item {
                    Text(
                        text = "Error: ${stats.errorMessage}",
                        style = MaterialTheme.typography.caption2,
                        color = Color.Red,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            } else {
                // System Health Card
                item {
                    SystemHealthCard(
                        cpuTempStr = stats.cpuTemp,
                        gpuTempStr = stats.dgpuTemp,
                        cpuPowerStr = stats.cpuPower,
                        gpuPowerStr = stats.dgpuPower
                    )
                }
                // CPU Row: Circular Temp + Linear Power
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CircularStatGauge(
                            label = "CPU",
                            value = stats.cpuTemp,
                            color = getTempColor(stats.cpuTemp),
                            max = 100f
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        LinearStatGauge(
                            label = "Power",
                            value = stats.cpuPower,
                            max = 120f, // Approx max CPU power
                            color = Color(0xFFFFC107) // Amber
                        )
                    }
                }

                // dGPU Row: Circular Temp + Linear VRAM
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CircularStatGauge(
                            label = "dGPU",
                            value = stats.dgpuTemp,
                            color = getTempColor(stats.dgpuTemp),
                            max = 100f
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LinearStatGauge(
                                label = "Power",
                                value = stats.dgpuPower,
                                max = 150f, // Approx max dGPU power (mobile)
                                color = Color(0xFFFFC107) // Amber
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearStatGauge(
                                label = "VRAM",
                                value = formatUsedTotalCompactMemory(stats.dgpuVram, stats.dgpuVramTotal),
                                max = parseMemoryToMb(stats.dgpuVramTotal).takeIf { it > 0f } ?: 8192f,
                                isMemory = true,
                                color = Color(0xFF03DAC5) // Teal
                            )
                        }
                    }
                }

                // System Row: RAM + Battery
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        LinearStatGauge(
                            label = "RAM Used",
                            value = formatUsedTotal(stats.ramUsed, stats.ramTotal),
                            max = parseMemoryToMb(stats.ramTotal).takeIf { it > 0f } ?: 32768f,
                            isMemory = true,
                            color = Color(0xFF2196F3) // Blue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearStatGauge(
                            label = "Battery Draw",
                            value = stats.batteryWattage,
                            max = 100f, // Discharge rate
                            color = Color(0xFFE91E63) // Pink
                        )
                    }
                }
                
                // iGPU VRAM (Compact)
                item {
                     LinearStatGauge(
                            label = "iGPU VRAM",
                            value = formatUsedTotal(stats.igpuVram, stats.igpuVramTotal),
                            max = parseMemoryToMb(stats.igpuVramTotal).takeIf { it > 0f } ?: 4096f,
                            isMemory = true,
                            color = Color(0xFF9C27B0) // Purple
                        )
                }
            }
        }
    }
}

@Composable
fun SystemHealthCard(cpuTempStr: String, gpuTempStr: String, cpuPowerStr: String, gpuPowerStr: String) {
    val cpuTemp = parseValue(cpuTempStr)
    val gpuTemp = parseValue(gpuTempStr)
    val cpuPower = parseValue(cpuPowerStr)
    val gpuPower = parseValue(gpuPowerStr)
    val maxTemp = maxOf(cpuTemp, gpuTemp)
    val totalPower = cpuPower + gpuPower

    val hasThermalData = cpuTemp > 0f || gpuTemp > 0f
    val hasPowerData = cpuPower > 0f || gpuPower > 0f

    var statusText = "UNKNOWN"
    var statusColor = Color.Gray
    var icon = Icons.Default.HelpOutline
    var reason = "Waiting for sensors"

    when {
        !hasThermalData && !hasPowerData -> {
            statusText = "UNKNOWN"
            reason = "No live thermal/power data"
        }
        maxTemp >= 92f -> {
            statusText = "CRITICAL"
            statusColor = Color(0xFFD32F2F)
            icon = Icons.Default.Warning
            reason = "Thermal limit reached"
        }
        maxTemp >= 85f -> {
            statusText = "CAUTION"
            statusColor = Color(0xFFF57C00)
            icon = Icons.Default.Warning
            reason = "High thermals"
        }
        totalPower >= 200f -> {
            statusText = "CAUTION"
            statusColor = Color(0xFFF57C00)
            icon = Icons.Default.Warning
            reason = "Very high system load"
        }
        maxTemp >= 75f && totalPower >= 120f -> {
            statusText = "CAUTION"
            statusColor = Color(0xFFF57C00)
            icon = Icons.Default.Warning
            reason = "Sustained load"
        }
        hasThermalData -> {
            statusText = "OPTIMAL"
            statusColor = Color(0xFF388E3C)
            icon = Icons.Default.CheckCircle
            reason = "Within expected limits"
        }
    }

    Card(
        onClick = {},
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = statusColor.copy(alpha = 0.2f),
            endBackgroundColor = MaterialTheme.colors.surface
        ),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = statusColor)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("System Stability", style = MaterialTheme.typography.caption2, color = Color.Gray)
                Text(statusText, style = MaterialTheme.typography.title3, fontWeight = FontWeight.Bold, color = statusColor)
                Text(reason, style = MaterialTheme.typography.caption3, color = Color.LightGray)
            }
        }
    }
}

@Composable
fun CircularStatGauge(label: String, value: String, color: Color, max: Float) {
    val floatValue = parseValue(value)
    val progress = (floatValue / max).coerceIn(0f, 1f)

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxSize(),
            startAngle = 0f,
            endAngle = 360f,
            strokeWidth = 4.dp,
            indicatorColor = color,
            trackColor = MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value.replace(" ", ""),
                style = MaterialTheme.typography.caption2,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.caption3,
                color = Color.Gray
            )
        }
    }
}

@Composable

fun LinearStatGauge(label: String, value: String, max: Float, color: Color, isMemory: Boolean = false) {

    val primaryValue = value.substringBefore("/").trim()
    val normalizedValue = if (isMemory) {
        val hasUnit = primaryValue.contains("GB", ignoreCase = true) || primaryValue.contains("MB", ignoreCase = true)
        val inferredPrimary = if (hasUnit) {
            primaryValue
        } else {
            when {
                value.contains("GB", ignoreCase = true) -> "$primaryValue GB"
                value.contains("MB", ignoreCase = true) -> "$primaryValue MB"
                else -> primaryValue
            }
        }
        parseMemoryToMb(inferredPrimary).takeIf { it > 0f } ?: parseValue(primaryValue)
    } else {
        parseValue(primaryValue)
    }

    val progress = (normalizedValue / max).coerceIn(0f, 1f)



    Column(modifier = Modifier.fillMaxWidth(0.9f)) {

        Row(

            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement = Arrangement.SpaceBetween

        ) {

            Text(text = label, style = MaterialTheme.typography.caption3, color = Color.LightGray)

            Text(text = value, style = MaterialTheme.typography.caption3, fontWeight = FontWeight.Bold, color = color)

        }

        // Custom Linear Progress Bar

        Box(

            modifier = Modifier

                .fillMaxWidth()

                .height(6.dp)

                .clip(RoundedCornerShape(3.dp))

                .background(MaterialTheme.colors.onSurface.copy(alpha = 0.1f))

        ) {

            Box(

                modifier = Modifier

                    .fillMaxWidth(progress)

                    .fillMaxHeight()

                    .clip(RoundedCornerShape(3.dp))

                    .background(color)

            )

        }

    }

}
