package com.majiang.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class SettingsUiState(
    val selectedRule: String = "广东麻将",
    val confidenceThreshold: Float = 0.7f,
    val simulationCount: Int = 1000,
    val isDarkTheme: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setRule(ruleName: String) {
        _uiState.value = _uiState.value.copy(selectedRule = ruleName)
    }

    fun setConfidenceThreshold(threshold: Float) {
        _uiState.value = _uiState.value.copy(confidenceThreshold = threshold)
    }

    fun setSimulationCount(count: Int) {
        _uiState.value = _uiState.value.copy(simulationCount = count)
    }

    fun setDarkTheme(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isDarkTheme = enabled)
    }
}
