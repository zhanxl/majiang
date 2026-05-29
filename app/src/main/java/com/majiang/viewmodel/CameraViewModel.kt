package com.majiang.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.majiang.model.Tile
import com.majiang.vision.RecognitionResult
import com.majiang.vision.TileRecognizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class CameraUiState(
    val isCameraActive: Boolean = false,
    val recognitionResults: List<RecognitionResult> = emptyList(),
    val recognizedTiles: List<Tile> = emptyList(),
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val tileRecognizer: TileRecognizer
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    init {
        tileRecognizer.initializeClassifier()
    }

    fun onCameraActive(isActive: Boolean) {
        _uiState.value = _uiState.value.copy(isCameraActive = isActive)
    }

    fun processFrame(bitmap: Bitmap) {
        if (_uiState.value.isProcessing) return

        _uiState.value = _uiState.value.copy(isProcessing = true)

        tileRecognizer.recognizeFromBitmap(bitmap) { results ->
            val tiles = results
                .filter { it.isHighConfidence && it.tile != null }
                .mapNotNull { it.tile }

            _uiState.value = _uiState.value.copy(
                recognitionResults = results,
                recognizedTiles = tiles,
                isProcessing = false
            )
        }
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

    override fun onCleared() {
        super.onCleared()
        tileRecognizer.release()
    }
}
