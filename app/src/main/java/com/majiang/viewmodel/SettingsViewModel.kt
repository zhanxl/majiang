package com.majiang.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.majiang.repository.SettingsRepository
import com.majiang.vision.strategy.CloudVisionProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val selectedRule: String = "广东麻将",
    val confidenceThreshold: Float = 0.7f,
    val simulationCount: Int = 1000,
    val isDarkTheme: Boolean = false,
    val cloudProvider: CloudVisionProvider = CloudVisionProvider.QWEN,
    val cloudApiKey: String = "",
    val customEndpoint: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val apiKey = settingsRepository.getCloudApiKeyOnce()
            val provider = settingsRepository.getCloudProviderOnce()
            val endpoint = settingsRepository.getCustomEndpointOnce()
            _uiState.value = _uiState.value.copy(
                cloudApiKey = apiKey,
                cloudProvider = provider,
                customEndpoint = endpoint
            )
        }
    }

    fun setRule(ruleName: String) {
        _uiState.value = _uiState.value.copy(selectedRule = ruleName)
        viewModelScope.launch { settingsRepository.saveSelectedRule(ruleName) }
    }

    fun setConfidenceThreshold(threshold: Float) {
        _uiState.value = _uiState.value.copy(confidenceThreshold = threshold)
        viewModelScope.launch { settingsRepository.saveConfidenceThreshold(threshold) }
    }

    fun setSimulationCount(count: Int) {
        _uiState.value = _uiState.value.copy(simulationCount = count)
    }

    fun setDarkTheme(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isDarkTheme = enabled)
    }

    fun setCloudProvider(provider: CloudVisionProvider) {
        _uiState.value = _uiState.value.copy(cloudProvider = provider)
        viewModelScope.launch { settingsRepository.saveCloudProvider(provider) }
    }

    fun setCloudApiKey(key: String) {
        _uiState.value = _uiState.value.copy(cloudApiKey = key)
        viewModelScope.launch { settingsRepository.saveCloudApiKey(key) }
    }

    fun setCustomEndpoint(endpoint: String) {
        _uiState.value = _uiState.value.copy(customEndpoint = endpoint)
        viewModelScope.launch { settingsRepository.saveCustomEndpoint(endpoint) }
    }
}
