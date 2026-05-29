package com.majiang.vision

import com.majiang.model.Tile
import com.majiang.model.TileCategory

data class RecognitionResult(
    val tile: Tile?,
    val confidence: Float,
    val boundingBox: BoundingBox?,
    val label: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isHighConfidence: Boolean
        get() = confidence >= CONFIDENCE_THRESHOLD

    companion object {
        const val CONFIDENCE_THRESHOLD = 0.7f

        fun unknown(label: String, confidence: Float): RecognitionResult =
            RecognitionResult(
                tile = null,
                confidence = confidence,
                boundingBox = null,
                label = label
            )
    }
}

data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2
    val centerY: Float get() = (top + bottom) / 2
}
