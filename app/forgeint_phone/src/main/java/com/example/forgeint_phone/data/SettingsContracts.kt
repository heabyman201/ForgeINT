package com.example.forgeint.data

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
}

data class LocalConnectivitySettings(
    val isLocalEnabled: Boolean,
    val endpoint: EndpointSettings
)

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
    val aiSettings: Flow<AiSettings>
    val monitoringSettings: Flow<MonitoringSettings>
    val localEndpointSettings: Flow<EndpointSettings>
    val localConnectivitySettings: Flow<LocalConnectivitySettings>

    suspend fun setLiteMode(enabled: Boolean)
    suspend fun setModel(modelId: String)
    suspend fun setLocalEnabled(enabled: Boolean)
    suspend fun setMemoryMonitorEnabled(enabled: Boolean)
    suspend fun setSystemTelemetryEnabled(enabled: Boolean)
    suspend fun setHostIp(ip: String)
    suspend fun setServerPort(port: String)
    suspend fun setSelectedPersona(personaId: String)
    suspend fun setMessageLength(length: String)
    suspend fun setAppTheme(theme: String)
    suspend fun setVoiceDominantMode(enabled: Boolean)
    suspend fun setApiKey(key: String)
    suspend fun setCustomApiKeyEnabled(enabled: Boolean)
    suspend fun setFunnelEnabled(enabled: Boolean)
    suspend fun setLocalAuthToken(token: String)
}
