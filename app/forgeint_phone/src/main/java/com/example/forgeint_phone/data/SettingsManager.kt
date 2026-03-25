package com.example.forgeint.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("user_settings")

class SettingsManager(private val context: Context) {
    // Keys
    private val LITE_MODE_KEY = booleanPreferencesKey("lite_mode_enabled")
    private val MODEL_ID_KEY = stringPreferencesKey("model_id")
    private val SELECTED_PERSONA_KEY = stringPreferencesKey("selected_persona_id")
    private val HOST_IP_KEY = stringPreferencesKey("host_ip")
    private val IS_LOCAL_ENABLED_KEY = booleanPreferencesKey("is_local_enabled")

    private val MEMORY_MONITOR = booleanPreferencesKey("memory_monitor")
    private val SYSTEM_TELEMETRY = booleanPreferencesKey("system_telemetry")
    private val APP_THEME_KEY = stringPreferencesKey("app_theme")
    private val VOICE_DOMINANT_MODE_KEY = booleanPreferencesKey("voice_dominant_mode")
    private val API_KEY = stringPreferencesKey("api_key")
    private val CUSTOM_API_KEY_ENABLED = booleanPreferencesKey("custom_api_key_enabled")
    private val FUNNEL_ENABLED = booleanPreferencesKey("funnel_enabled")
    private val LOCAL_AUTH_TOKEN = stringPreferencesKey("local_auth_token")

    private val MESSAGE_LENGTH_KEY = stringPreferencesKey("message_length")

    // New Key for Port
    private val SERVER_PORT_KEY = stringPreferencesKey("server_port")

    // --- Flows ---

    val isLiteMode: Flow<Boolean> = context.dataStore.data
        .map { it[LITE_MODE_KEY] ?: true }

    val selectedModel: Flow<String> = context.dataStore.data
        .map { it[MODEL_ID_KEY] ?: "google/gemma-3n-e4b-it:free" }

    val isMemoryMonitorEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[MEMORY_MONITOR] ?: false }

val isSystemTelemetryEnabled: Flow<Boolean> = context.dataStore.data
    .map {
        it[SYSTEM_TELEMETRY] ?: false
    }

    val isLocalEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[IS_LOCAL_ENABLED_KEY] ?: false }

    val hostIp: Flow<String> = context.dataStore.data
        .map { it[HOST_IP_KEY] ?: "192.168.1.1" }

    // New Flow: Defaults to "1234" (LM Studio default) if not set
    val serverPort: Flow<String> = context.dataStore.data
        .map { it[SERVER_PORT_KEY] ?: "1234" }

    val selectedPersonaId: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[SELECTED_PERSONA_KEY] ?: "default" }

    val messageLength: Flow<String> = context.dataStore.data
        .map { it[MESSAGE_LENGTH_KEY] ?: "Normal" }

    val appTheme: Flow<String> = context.dataStore.data
        .map { it[APP_THEME_KEY] ?: "Default" }

    val isVoiceDominantMode: Flow<Boolean> = context.dataStore.data
        .map { it[VOICE_DOMINANT_MODE_KEY] ?: false }

    val apiKey: Flow<String> = context.dataStore.data
        .map { it[API_KEY] ?: "" }

    val isCustomApiKeyEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[CUSTOM_API_KEY_ENABLED] ?: false }

    val isFunnelEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[FUNNEL_ENABLED] ?: false }

    val localAuthToken: Flow<String> = context.dataStore.data
        .map { it[LOCAL_AUTH_TOKEN] ?: DEFAULT_LOCAL_AUTH_TOKEN }

    // --- Setters ---

    suspend fun setLiteMode(enabled: Boolean) {
        context.dataStore.edit { it[LITE_MODE_KEY] = enabled }
    }

    suspend fun setModel(modelId: String) {
        context.dataStore.edit { it[MODEL_ID_KEY] = modelId }
    }

    suspend fun setLocalEnabled(enabled: Boolean) {
        context.dataStore.edit { it[IS_LOCAL_ENABLED_KEY] = enabled }
    }
suspend fun setMemoryMonitorEnabled(enabled: Boolean) {
    context.dataStore.edit {
        it[MEMORY_MONITOR] = enabled
    }
}
    suspend fun setSystemTelemetryEnabled(enabled: Boolean) {
        context.dataStore.edit { it[SYSTEM_TELEMETRY] = enabled }
    }
    suspend fun setHostIp(ip: String) {
        context.dataStore.edit { it[HOST_IP_KEY] = ip }
    }

    // New Setter for Port
    suspend fun setServerPort(port: String) {
        context.dataStore.edit { it[SERVER_PORT_KEY] = port }
    }

    suspend fun setSelectedPersona(personaId: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_PERSONA_KEY] = personaId
        }
    }

    suspend fun setMessageLength(length: String) {
        context.dataStore.edit { it[MESSAGE_LENGTH_KEY] = length }
    }

    suspend fun setAppTheme(theme: String) {
        context.dataStore.edit { it[APP_THEME_KEY] = theme }
    }

    suspend fun setVoiceDominantMode(enabled: Boolean) {
        context.dataStore.edit { it[VOICE_DOMINANT_MODE_KEY] = enabled }
    }

    suspend fun setApiKey(key: String) {
        context.dataStore.edit { it[API_KEY] = key }
    }

    suspend fun setCustomApiKeyEnabled(enabled: Boolean) {
        context.dataStore.edit { it[CUSTOM_API_KEY_ENABLED] = enabled }
    }

    suspend fun setFunnelEnabled(enabled: Boolean) {
        context.dataStore.edit { it[FUNNEL_ENABLED] = enabled }
    }

    suspend fun setLocalAuthToken(token: String) {
        val normalized = token.trim().ifBlank { DEFAULT_LOCAL_AUTH_TOKEN }
        context.dataStore.edit { it[LOCAL_AUTH_TOKEN] = normalized }
    }

    companion object {
        const val DEFAULT_LOCAL_AUTH_TOKEN = "sk-lm-fBt9rGIy:7Dk693RtQlDV5nZSR3or"
    }
}
