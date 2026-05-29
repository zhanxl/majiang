package com.majiang.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.majiang.vision.strategy.CloudVisionProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "majiang_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_CLOUD_API_KEY = stringPreferencesKey("cloud_api_key")
        val KEY_CLOUD_PROVIDER = stringPreferencesKey("cloud_provider")
        val KEY_CUSTOM_ENDPOINT = stringPreferencesKey("custom_endpoint")
        val KEY_SELECTED_RULE = stringPreferencesKey("selected_rule")
        val KEY_CONFIDENCE_THRESHOLD = stringPreferencesKey("confidence_threshold")
        val KEY_SIMULATION_COUNT = stringPreferencesKey("simulation_count")
    }

    val cloudApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CLOUD_API_KEY] ?: ""
    }

    val cloudProvider: Flow<CloudVisionProvider> = context.dataStore.data.map { prefs ->
        try {
            CloudVisionProvider.valueOf(prefs[KEY_CLOUD_PROVIDER] ?: "QWEN")
        } catch (_: Exception) {
            CloudVisionProvider.QWEN
        }
    }

    val customEndpoint: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_ENDPOINT] ?: ""
    }

    val selectedRule: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_RULE] ?: "广东麻将"
    }

    suspend fun saveCloudApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CLOUD_API_KEY] = key
        }
    }

    suspend fun saveCloudProvider(provider: CloudVisionProvider) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CLOUD_PROVIDER] = provider.name
        }
    }

    suspend fun saveCustomEndpoint(endpoint: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CUSTOM_ENDPOINT] = endpoint
        }
    }

    suspend fun saveSelectedRule(rule: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_RULE] = rule
        }
    }

    suspend fun saveConfidenceThreshold(threshold: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CONFIDENCE_THRESHOLD] = threshold.toString()
        }
    }

    suspend fun getCloudApiKeyOnce(): String {
        return cloudApiKey.first()
    }

    suspend fun getCloudProviderOnce(): CloudVisionProvider {
        return cloudProvider.first()
    }

    suspend fun getCustomEndpointOnce(): String {
        return customEndpoint.first()
    }
}
