package com.example.forgeint.presentation

import kotlinx.coroutines.flow.Flow

data class AiSettings(
    val modelId: String,
    val selectedPersonaId: String,
    val messageLength: String,
    val apiKey: String,
    val isCustomApiKeyEnabled: Boolean
)

data class MonitoringSettings(
    val isMemoryMonitorEnabled: Boolean,
    val isSystemTelemetryEnabled: Boolean
)

data class PowerSettings(
    val isAutoPowerSavingMode: Boolean,
    val isAutoPowerSavingActive: Boolean,
    val previousTheme: String,
    val thresholdPercent: Float
)

data class EndpointSettings(
    val host: String,
    val port: String,
    val authToken: String = "",
    val funnelEnabled: Boolean = false
) {
    val cleanHost: String
        get() = host.removePrefix("https://").removePrefix("http://").trim('/')

    val isTunnel: Boolean
        get() = funnelEnabled ||
            cleanHost.endsWith(".ts.net", ignoreCase = true) ||
            cleanHost.contains("cloudflare", ignoreCase = true) ||
            cleanHost.contains("ngrok", ignoreCase = true) ||
            cleanHost.contains("loclx", ignoreCase = true)

    fun baseUrl(defaultPort: String): String {
        val resolvedPort = port.trim().ifBlank { defaultPort }
        val hostWithoutPort = cleanHost.substringBefore(":")
        return if (isTunnel) {
            "https://$cleanHost/"
        } else {
            "http://$hostWithoutPort:$resolvedPort/"
        }
    }

    fun defaultHeaders(): Map<String, String> {
        return buildMap {
            if (authToken.isNotBlank()) {
                put("Authorization", "Bearer ${authToken.trim()}")
            }
            if (isTunnel) {
                put("User-Agent", "ForgeIntApp")
                put("cf-terminate-connection", "true")
                if (cleanHost.contains("ngrok", ignoreCase = true)) {
                    put("ngrok-skip-browser-warning", "true")
                }
            }
        }
    }
}

interface SettingsStore {
    val isLiteMode: Flow<Boolean>
    val selectedModel: Flow<String>
    val selectedPersonaId: Flow<String>
    val messageLength: Flow<String>
    val appTheme: Flow<String>
    val isVoiceDominantMode: Flow<Boolean>
    val apiKey: Flow<String>
    val isCustomApiKeyEnabled: Flow<Boolean>
    val isMemoryMonitorEnabled: Flow<Boolean>
    val isSystemTelemetryEnabled: Flow<Boolean>
    val isLocalEnabled: Flow<Boolean>
    val isFunnelEnabled: Flow<Boolean>
    val localAuthToken: Flow<String>
    val hostIp: Flow<String>
    val serverPort: Flow<String>
    val hardwareHostIp: Flow<String>
    val hardwarePort: Flow<String>
    val remoteHostIp: Flow<String>
    val remotePort: Flow<String>
    val isAutoPowerSavingModeThreshold: Flow<Float>
    val aiSettings: Flow<AiSettings>
    val monitoringSettings: Flow<MonitoringSettings>
    val localEndpointSettings: Flow<EndpointSettings>
    val hardwareEndpointSettings: Flow<EndpointSettings>
    val remoteEndpointSettings: Flow<EndpointSettings>
    val powerSettings: Flow<PowerSettings>

    suspend fun setLiteMode(enabled: Boolean)
    suspend fun setModel(modelId: String)
    suspend fun setAutoPowerSavingModeThreshold(threshold: Float)
    suspend fun setLocalEnabled(enabled: Boolean)
    suspend fun setMemoryMonitorEnabled(enabled: Boolean)
    suspend fun setSystemTelemetryEnabled(enabled: Boolean)
    suspend fun setHostIp(ip: String)
    suspend fun setServerPort(port: String)
    suspend fun setHardwareHostIp(ip: String)
    suspend fun setHardwarePort(port: String)
    suspend fun setRemoteHostIp(ip: String)
    suspend fun setRemotePort(port: String)
    suspend fun setFunnelEnabled(enabled: Boolean)
    suspend fun setLocalAuthToken(token: String)
    suspend fun setSelectedPersona(personaId: String)
    suspend fun setMessageLength(length: String)
    suspend fun setApiKey(apiKey: String)
    suspend fun setCustomApiKeyEnabled(enabled: Boolean)
    suspend fun setAppTheme(theme: String)
    suspend fun setVoiceDominantMode(enabled: Boolean)
}
