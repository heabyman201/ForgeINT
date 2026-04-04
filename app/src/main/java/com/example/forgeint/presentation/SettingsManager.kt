package com.example.weargemini.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("user_settings")

class SettingsManager(private val context: Context) {
    companion object {
        // Shared app token for local/Funnel LLM access. Change this to your own long random value.
        const val DEFAULT_LOCAL_AUTH_TOKEN = "sk-lm-fBt9rGIy:7Dk693RtQlDV5nZSR3or"
        const val DEFAULT_CLOUD_MODEL_ID = "google/gemma-3n-e4b-it:free"
    }

    // Keys
    private val LITE_MODE_KEY = booleanPreferencesKey("lite_mode_enabled")
    private val MODEL_ID_KEY = stringPreferencesKey("model_id")
    private val SELECTED_PERSONA_KEY = stringPreferencesKey("selected_persona_id")
    private val HOST_IP_KEY = stringPreferencesKey("host_ip")
    private val IS_LOCAL_ENABLED_KEY = booleanPreferencesKey("is_local_enabled")

    private val MEMORY_MONITOR = booleanPreferencesKey("memory_monitor")
    private val SYSTEM_TELEMETRY = booleanPreferencesKey("system_telemetry")

    private val MESSAGE_LENGTH_KEY = stringPreferencesKey("message_length")

    // New Key for Port
    private val SERVER_PORT_KEY = stringPreferencesKey("server_port")
    private val HARDWARE_HOST_IP_KEY = stringPreferencesKey("hardware_host_ip")
    private val HARDWARE_PORT_KEY = stringPreferencesKey("hardware_port")
    private val REMOTE_HOST_IP_KEY = stringPreferencesKey("remote_host_ip")
    private val REMOTE_PORT_KEY = stringPreferencesKey("remote_port")
    private val IS_FUNNEL_ENABLED_KEY = booleanPreferencesKey("is_funnel_enabled")
    private val LOCAL_AUTH_TOKEN_KEY = stringPreferencesKey("local_auth_token")

    private val API_KEY_KEY = stringPreferencesKey("api_key")
    private val IS_CUSTOM_API_KEY_ENABLED_KEY = booleanPreferencesKey("is_custom_api_key_enabled")
    private val APP_THEME_KEY = stringPreferencesKey("app_theme")
    private val VOICE_DOMINANT_MODE_KEY = booleanPreferencesKey("voice_dominant_mode")
    private val AUTO_POWER_SAVING_MODE_KEY = booleanPreferencesKey("auto_power_saving_mode")
    private val AUTO_POWER_SAVING_PREVIOUS_THEME_KEY =
        stringPreferencesKey("auto_power_saving_previous_theme")
    private val AUTO_POWER_SAVING_ACTIVE_KEY = booleanPreferencesKey("auto_power_saving_active")
    private val AUTO_POWER_SAVING_MODE_THRESHOLD_KEY =
        floatPreferencesKey("auto_power_saving_mode_threshold")
    // --- Flows ---

    val isVoiceDominantMode: Flow<Boolean> = context.dataStore.data
        .map { it[VOICE_DOMINANT_MODE_KEY] ?: false }

    val isAutoPowerSavingMode: Flow<Boolean> = context.dataStore.data
        .map { it[AUTO_POWER_SAVING_MODE_KEY] ?: false }

    val autoPowerSavingPreviousTheme: Flow<String> = context.dataStore.data
        .map { it[AUTO_POWER_SAVING_PREVIOUS_THEME_KEY] ?: "" }

    val isAutoPowerSavingModeThreshold: Flow<Float> = context.dataStore.data
        .map { it[AUTO_POWER_SAVING_MODE_THRESHOLD_KEY] ?: 0f }

    val isAutoPowerSavingActive: Flow<Boolean> = context.dataStore.data
        .map { it[AUTO_POWER_SAVING_ACTIVE_KEY] ?: false }

    val isLiteMode: Flow<Boolean> = context.dataStore.data
        .map { it[LITE_MODE_KEY] ?: true }

    val appTheme: Flow<String> = context.dataStore.data
        .map { it[APP_THEME_KEY] ?: "Default" }

    val selectedModel: Flow<String> = context.dataStore.data
        .map { it[MODEL_ID_KEY] ?: DEFAULT_CLOUD_MODEL_ID }

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

    val hardwareHostIp: Flow<String> = context.dataStore.data
        .map { it[HARDWARE_HOST_IP_KEY] ?: "192.168.1.1" }

    val hardwarePort: Flow<String> = context.dataStore.data
        .map { it[HARDWARE_PORT_KEY] ?: "8080" }

    val remoteHostIp: Flow<String> = context.dataStore.data
        .map { it[REMOTE_HOST_IP_KEY] ?: "100.96.101.62" }

    val remotePort: Flow<String> = context.dataStore.data
        .map { it[REMOTE_PORT_KEY] ?: "1235" }

    val isFunnelEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[IS_FUNNEL_ENABLED_KEY] ?: false }

    val localAuthToken: Flow<String> = context.dataStore.data
        .map { it[LOCAL_AUTH_TOKEN_KEY] ?: DEFAULT_LOCAL_AUTH_TOKEN }

    val selectedPersonaId: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[SELECTED_PERSONA_KEY] ?: "default" }

    val messageLength: Flow<String> = context.dataStore.data
        .map { it[MESSAGE_LENGTH_KEY] ?: "Normal" }

    val apiKey: Flow<String> = context.dataStore.data
        .map { it[API_KEY_KEY] ?: "" }

    val isCustomApiKeyEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[IS_CUSTOM_API_KEY_ENABLED_KEY] ?: false }

    // --- Setters ---

    suspend fun setLiteMode(enabled: Boolean) {
        context.dataStore.edit { it[LITE_MODE_KEY] = enabled }
    }

    suspend fun setModel(modelId: String) {
        context.dataStore.edit { it[MODEL_ID_KEY] = modelId }
    }

    suspend fun setAutoPowerSavingModeThreshold(threshold: Float) {
        context.dataStore.edit { it[AUTO_POWER_SAVING_MODE_THRESHOLD_KEY] = threshold }
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

        suspend fun setHardwareHostIp(ip: String) {
            context.dataStore.edit { it[HARDWARE_HOST_IP_KEY] = ip }
        }

        suspend fun setHardwarePort(port: String) {
            context.dataStore.edit { it[HARDWARE_PORT_KEY] = port }
        }

        suspend fun setRemoteHostIp(ip: String) {
            context.dataStore.edit { it[REMOTE_HOST_IP_KEY] = ip }
        }

        suspend fun setRemotePort(port: String) {
            context.dataStore.edit { it[REMOTE_PORT_KEY] = port }
        }

        suspend fun setFunnelEnabled(enabled: Boolean) {
            context.dataStore.edit { it[IS_FUNNEL_ENABLED_KEY] = enabled }
        }

        suspend fun setLocalAuthToken(token: String) {
            val normalized = token.trim().ifBlank { DEFAULT_LOCAL_AUTH_TOKEN }
            context.dataStore.edit { it[LOCAL_AUTH_TOKEN_KEY] = normalized }
        }

        suspend fun setSelectedPersona(personaId: String) {
            context.dataStore.edit { preferences ->
                preferences[SELECTED_PERSONA_KEY] = personaId
            }
        }

        suspend fun setMessageLength(length: String) {
            context.dataStore.edit { it[MESSAGE_LENGTH_KEY] = length }
        }

        suspend fun setApiKey(apiKey: String) {
            context.dataStore.edit { it[API_KEY_KEY] = apiKey }
        }

        suspend fun setCustomApiKeyEnabled(enabled: Boolean) {
            context.dataStore.edit { it[IS_CUSTOM_API_KEY_ENABLED_KEY] = enabled }
        }

        suspend fun setAppTheme(theme: String) {

            context.dataStore.edit { it[APP_THEME_KEY] = theme }

        }


        suspend fun setVoiceDominantMode(enabled: Boolean) {

            context.dataStore.edit { it[VOICE_DOMINANT_MODE_KEY] = enabled }

        }

        suspend fun setAutoPowerSavingMode(enabled: Boolean) {
            context.dataStore.edit { it[AUTO_POWER_SAVING_MODE_KEY] = enabled }
        }

        suspend fun setAutoPowerSavingPreviousTheme(theme: String) {
            context.dataStore.edit { it[AUTO_POWER_SAVING_PREVIOUS_THEME_KEY] = theme }
        }

        suspend fun clearAutoPowerSavingPreviousTheme() {
            context.dataStore.edit { it.remove(AUTO_POWER_SAVING_PREVIOUS_THEME_KEY) }
        }

        suspend fun setAutoPowerSavingActive(enabled: Boolean) {
            context.dataStore.edit { it[AUTO_POWER_SAVING_ACTIVE_KEY] = enabled }
        }



    }


    
