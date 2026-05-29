package com.majiang.vision

import com.majiang.model.Tile

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
        const val CONFIDENCE_THRESHOLD = 0.5f

        fun unknown(label: String, confidence: Float): RecognitionResult =
            RecognitionResult(
                tile = null,
                confidence = confidence,
                boundingBox = null,
                label = label
            )
    }
}
