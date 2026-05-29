package com.majiang.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.majiang.model.Tile
import com.majiang.vision.RecognitionMode
import com.majiang.vision.RecognitionResult
import com.majiang.vision.TileRecognizer
import com.majiang.vision.strategy.CloudVisionProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CameraUiState(
    val isCameraActive: Boolean = false,
    val recognitionResults: List<RecognitionResult> = emptyList(),
    val recognizedTiles: List<Tile> = emptyList(),
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val recognitionMode: RecognitionMode = RecognitionMode.CLOUD_VISION,
    val isConfigured: Boolean = false,
    val lastCapturedBitmap: Bitmap? = null
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val tileRecognizer: TileRecognizer
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun onCameraActive(isActive: Boolean) {
        _uiState.value = _uiState.value.copy(isCameraActive = isActive)
    }

    fun recognizeFromBitmap(bitmap: Bitmap) {
        if (_uiState.value.isProcessing) return

        _uiState.value = _uiState.value.copy(
            isProcessing = true,
            lastCapturedBitmap = bitmap,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val results = tileRecognizer.recognize(bitmap)
                val tiles = results
                    .filter { it.tile != null }
                    .mapNotNull { it.tile }

                _uiState.value = _uiState.value.copy(
                    recognitionResults = results,
                    recognizedTiles = tiles,
                    isProcessing = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = "识别失败: ${e.message}"
                )
            }
        }
    }

    fun configureCloudVision(apiKey: String, provider: CloudVisionProvider) {
        tileRecognizer.configureCloudVision(apiKey, provider)
        _uiState.value = _uiState.value.copy(
            isConfigured = tileRecognizer.isConfigured,
            recognitionMode = RecognitionMode.CLOUD_VISION
        )
    }

    fun setRecognitionMode(mode: RecognitionMode) {
        tileRecognizer.setMode(mode)
        _uiState.value = _uiState.value.copy(
            recognitionMode = mode,
            isConfigured = tileRecognizer.isConfigured
        )
    }

    fun setConfidenceThreshold(threshold: Float) {
        tileRecognizer.confidenceThreshold = threshold
    }

    fun clearResults() {
        _uiState.value = _uiState.value.copy(
            recognitionResults = emptyList(),
            recognizedTiles = emptyList(),
            lastCapturedBitmap = null,
            errorMessage = null
        )
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        tileRecognizer.release()
    }
}
