package com.example.forgeint.presentation
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import retrofit2.http.GET
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.annotations.SerializedName
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val dgpuTemp: String = "N/A",
    val dgpuPower: String = "N/A",
    val dgpuVram: String = "N/A",
    val igpuVram: String = "N/A",
    val batteryWattage: String = "N/A",
    val ramUsed: String = "N/A",
    val errorMessage: String? = null
)

@Keep
interface HardwareApi {
    @GET("data.json")
    suspend fun getHardwareStats(): HardwareResponse

    companion object {
        fun create(baseUrl: String): HardwareApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
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
    private val phoneCommunicator: PhoneCommunicator? = null
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun fetchG14Status(usePhoneProxy: Boolean = false): G14Stats {
        return try {
            val url = buildHardwareUrl(baseUrl)
            val headers = buildTunnelHeaders(baseUrl)

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
                parseNodes(response.Children ?: emptyList())
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
                parseNodes(parsed.Children ?: emptyList())
            } else {
                val response = api.getHardwareStats()
                // If response.Children is null, treat as empty
                parseNodes(response.Children ?: emptyList())
            }
        } catch (e: Exception) {
            G14Stats(errorMessage = "${e.javaClass.simpleName}: ${e.message ?: "Unknown Error"}")
        }
    }

    private fun buildHardwareUrl(baseUrl: String): String {
        val cleanBase = baseUrl.trimEnd('/')
        return "$cleanBase/data.json"
    }

    private fun buildTunnelHeaders(baseUrl: String): Map<String, String> {
        val cleanHost = baseUrl
            .removePrefix("https://")
            .removePrefix("http://")
            .trim('/')

        val isTunnel = cleanHost.contains("cloudflare") ||
            cleanHost.contains("ngrok") ||
            cleanHost.contains("loclx")

        return if (isTunnel) {
            mapOf(
                "ngrok-skip-browser-warning" to "true",
                "cf-terminate-connection" to "true",
                "User-Agent" to "ForgeIntApp"
            )
        } else {
            emptyMap()
        }
    }

    private fun parseNodes(nodes: List<HardwareNode>): G14Stats {
        var cpuTemp = "N/A"
        var cpuPower = "N/A"
        var dgpuTemp = "N/A"
        var dgpuPower = "N/A"
        var dgpuVram = "N/A"
        var igpuVram = "N/A"
        var batt = "N/A"
        var ram = "N/A"

        fun traverse(node: HardwareNode, parentType: String = "") {
            var currentType = parentType
            val textLower = node.Text.lowercase()
            val imageLower = node.ImageURL?.lowercase() ?: ""
            
            // Detect Context
            if (imageLower.contains("nvidia") || textLower.contains("nvidia") || textLower.contains("geforce") || textLower.contains("rtx") || textLower.contains("gtx")) {
                currentType = "dGPU"
            } else if (textLower.contains("radeon") || textLower.contains("intel uhd") || textLower.contains("iris") || (textLower.contains("graphics") && parentType == "")) {
                currentType = "iGPU"
            } else if (imageLower.contains("cpu") || textLower.contains("ryzen") || textLower.contains("intel core") || textLower.contains("processor") || textLower.contains("cpu")) {
                currentType = "CPU"
            } else if (textLower.contains("memory") && parentType == "" && !textLower.contains("gpu") && !textLower.contains("graphics")) {
                currentType = "RAM"
            } else if (textLower.contains("battery")) {
                currentType = "Battery"
            }

            val value = node.Value ?: "N/A"
            val valueLower = value.lowercase()
            val hasTempUnit = value.contains("°C") || value.contains("Â°C") || (value.contains("°") && valueLower.contains("c"))
            val hasPowerUnit = valueLower.contains("w")
            if (value != "N/A" && value.any { it.isDigit() }) {
                when (currentType) {
                    "CPU" -> {

                        if (value.contains("°C") || value.contains("C") || textLower.contains("temperature") || node.ImageURL?.contains("temperature") == true) {
                            if (!textLower.contains("distance")) {
                                if (cpuTemp == "N/A" || textLower.contains("package") || textLower.contains("tdie")) {
                                    cpuTemp = value
                                }
                            }
                        }
                        // Power: Check Unit "W" or Text "Power"
                        if (value.contains("W") || textLower.contains("power")) {
                            if (cpuPower == "N/A" || textLower.contains("package") || textLower.contains("total")) {
                                cpuPower = value
                            }
                        }
                    }
                    "dGPU" -> {
                        if ((value.contains("°C") || node.ImageURL?.contains("temperature") == true) && (dgpuTemp == "N/A" || textLower.contains("core"))) dgpuTemp = value
                        if ((value.contains("MB") || value.contains("GB")) && textLower.contains("used")) dgpuVram = value
                        if ((value.contains("W") || textLower.contains("power")) && (dgpuPower == "N/A" || textLower.contains("tgp") || textLower.contains("total"))) dgpuPower = value
                    }
                    "iGPU" -> {
                        if ((value.contains("MB") || value.contains("GB")) && textLower.contains("used")) igpuVram = value
                    }
                    "Battery" -> {
                        if (value.contains("W") || textLower.contains("rate")) batt = value
                    }
                    "RAM" -> {
                        if (textLower.contains("used") && (value.contains("GB") || value.contains("MB"))) ram = value
                    }
                }
            }
            
            node.Children?.forEach { traverse(it, currentType) }
        }

        nodes.forEach { traverse(it) }
        return G14Stats(cpuTemp, cpuPower, dgpuTemp, dgpuPower, dgpuVram, igpuVram, batt, ram)
    }
}

class ForgeHardwareViewModel(
    private val repository: HardwareRepository,
    private val connectionModeFlow: StateFlow<ConnectionMode>? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(G14Stats())
    val uiState: StateFlow<G14Stats> = _uiState.asStateFlow()
    
    private var pollingJob: kotlinx.coroutines.Job? = null

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (true) {
                val useProxy = connectionModeFlow?.value == ConnectionMode.Phone
                _uiState.value = repository.fetchG14Status(useProxy)
                delay(3000) // Poll every 3 seconds
            }
        }
    }
    
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun refresh() {
        viewModelScope.launch {
            val useProxy = connectionModeFlow?.value == ConnectionMode.Phone
            _uiState.value = repository.fetchG14Status(useProxy)
        }
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

@Composable
fun G14WatchMonitor(viewModel: ForgeHardwareViewModel) {
    val stats by viewModel.uiState.collectAsState()

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
                    text = "Local Server",
                    style = MaterialTheme.typography.caption2,
                    color = Color.Gray
                )
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
                    SystemHealthCard(cpuTempStr = stats.cpuTemp, gpuTempStr = stats.dgpuTemp)
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
                                value = stats.dgpuVram,
                                max = 8192f, // Approx 8GB VRAM
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
                            value = stats.ramUsed,
                            max = 32768f, // Approx 32GB RAM
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
                            value = stats.igpuVram,
                            max = 4096f,
                            isMemory = true,
                            color = Color(0xFF9C27B0) // Purple
                        )
                }
            }
        }
    }
}

@Composable
fun SystemHealthCard(cpuTempStr: String, gpuTempStr: String) {
    val cpuTemp = parseValue(cpuTempStr)
    val gpuTemp = parseValue(gpuTempStr)
    val maxTemp = maxOf(cpuTemp, gpuTemp)

    val (statusText, statusColor, icon) = when {
        maxTemp > 90 -> Triple("CRITICAL", Color(0xFFD32F2F), Icons.Default.Warning) // Red
        maxTemp > 75 -> Triple("CAUTION", Color(0xFFF57C00), Icons.Default.Warning) // Orange
        maxTemp > 0 -> Triple("OPTIMAL", Color(0xFF388E3C), Icons.Default.CheckCircle) // Green
        else -> Triple("UNKNOWN", Color.Gray, Icons.Default.HelpOutline)
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

    val floatValue = parseValue(value)

    val normalizedValue = if (isMemory && floatValue < 128f && value.contains("GB")) floatValue * 1024f else floatValue

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


