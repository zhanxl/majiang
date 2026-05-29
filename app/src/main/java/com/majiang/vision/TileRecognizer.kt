package com.majiang.vision

import android.content.Context
import android.graphics.Bitmap
import com.majiang.model.Tile
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TileRecognizer @Inject constructor(
    private val context: Context
) {
    private var detector: YOLOv8Detector? = null

    var confidenceThreshold: Float = 0.5f
        set(value) {
            field = value
            detector?.confThreshold = value
        }

    var iouThreshold: Float = 0.45f
        set(value) {
            field = value
            detector?.iouThreshold = value
        }

    val isInitialized: Boolean
        get() = detector?.isInitialized == true

    fun initialize(modelPath: String = "mahjong_yolov8.tflite") {
        try {
            detector = YOLOv8Detector(
                context = context,
                modelPath = modelPath,
                labelsPath = "mahjong_labels.txt"
            ).also {
                it.confThreshold = confidenceThreshold
                it.iouThreshold = iouThreshold
            }
            Timber.d("TileRecognizer initialized with YOLOv8 model")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize YOLOv8 detector")
        }
    }

    fun recognizeFromBitmap(bitmap: Bitmap): List<RecognitionResult> {
        val det = detector
        if (det == null || !det.isInitialized) {
            Timber.w("Detector not initialized, skipping recognition")
            return emptyList()
        }

        val tileDetections = det.detectTiles(bitmap)

        return tileDetections.map { detection ->
            RecognitionResult(
                tile = detection.tile,
                confidence = detection.confidence,
                boundingBox = BoundingBox(
                    left = detection.boundingBox.left / bitmap.width,
                    top = detection.boundingBox.top / bitmap.height,
                    right = detection.boundingBox.right / bitmap.width,
                    bottom = detection.boundingBox.bottom / bitmap.height
                ),
                label = detection.tile.displayName
            )
        }
    }

    fun recognizeTilesFromBitmap(bitmap: Bitmap): List<Tile> {
        return recognizeFromBitmap(bitmap)
            .filter { it.isHighConfidence && it.tile != null }
            .mapNotNull { it.tile }
    }

    fun release() {
        detector?.close()
        detector = null
        Timber.d("TileRecognizer released")
    }
}
