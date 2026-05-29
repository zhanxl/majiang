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
    val recognitionMode: RecognitionMode = RecognitionMode.MANUAL,
    val recognitionResults: List<RecognitionResult> = emptyList(),
    val recognizedTiles: List<Tile> = emptyList(),
    val manuallySelectedTiles: List<Tile> = emptyList(),
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val availableModes: List<RecognitionMode> = listOf(RecognitionMode.MANUAL),
    val cloudApiKey: String = "",
    val cloudProvider: CloudVisionProvider = CloudVisionProvider.QWEN
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val tileRecognizer: TileRecognizer
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    init {
        updateAvailableModes()
    }

    fun onCameraActive(isActive: Boolean) {
        _uiState.value = _uiState.value.copy(isCameraActive = isActive)
    }

    fun setRecognitionMode(mode: RecognitionMode) {
        tileRecognizer.setMode(mode)
        _uiState.value = _uiState.value.copy(
            recognitionMode = mode,
            recognitionResults = emptyList(),
            recognizedTiles = emptyList()
        )
    }

    fun processFrame(bitmap: Bitmap) {
        if (_uiState.value.isProcessing) return
        if (_uiState.value.recognitionMode == RecognitionMode.MANUAL) return

        _uiState.value = _uiState.value.copy(isProcessing = true)

        viewModelScope.launch {
            try {
                val results = tileRecognizer.recognize(bitmap)
                val tiles = results
                    .filter { it.isHighConfidence && it.tile != null }
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

    fun addManualTile(tile: Tile) {
        tileRecognizer.manualStrategy.addTile(tile)
        val tiles = tileRecognizer.manualStrategy.selectedTiles
        _uiState.value = _uiState.value.copy(
            manuallySelectedTiles = tiles,
            recognizedTiles = tiles
        )
    }

    fun removeManualTile(index: Int) {
        val current = tileRecognizer.manualStrategy.selectedTiles
        if (index in current.indices) {
            tileRecognizer.manualStrategy.removeTile(current[index])
        }
        val tiles = tileRecognizer.manualStrategy.selectedTiles
        _uiState.value = _uiState.value.copy(
            manuallySelectedTiles = tiles,
            recognizedTiles = tiles
        )
    }

    fun removeLastManualTile() {
        tileRecognizer.manualStrategy.removeLastTile()
        val tiles = tileRecognizer.manualStrategy.selectedTiles
        _uiState.value = _uiState.value.copy(
            manuallySelectedTiles = tiles,
            recognizedTiles = tiles
        )
    }

    fun clearManualTiles() {
        tileRecognizer.manualStrategy.clearTiles()
        _uiState.value = _uiState.value.copy(
            manuallySelectedTiles = emptyList(),
            recognizedTiles = emptyList()
        )
    }

    fun configureCloudVision(apiKey: String, provider: CloudVisionProvider) {
        tileRecognizer.configureCloudVision(apiKey, provider)
        _uiState.value = _uiState.value.copy(
            cloudApiKey = apiKey,
            cloudProvider = provider
        )
        updateAvailableModes()
    }

    fun configureTFLite() {
        tileRecognizer.configureTFLite()
        updateAvailableModes()
    }

    fun clearResults() {
        _uiState.value = _uiState.value.copy(
            recognitionResults = emptyList(),
            recognizedTiles = emptyList()
        )
    }

    fun setConfidenceThreshold(threshold: Float) {
        tileRecognizer.confidenceThreshold = threshold
    }

    private fun updateAvailableModes() {
        val modes = tileRecognizer.getAvailableModes()
        _uiState.value = _uiState.value.copy(availableModes = modes)
    }

    override fun onCleared() {
        super.onCleared()
        tileRecognizer.release()
    }
}
