package com.majiang.vision

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

    fun area(): Float = maxOf(0f, width) * maxOf(0f, height)

    fun intersection(other: BoundingBox): BoundingBox {
        val newLeft = maxOf(left, other.left)
        val newTop = maxOf(top, other.top)
        val newRight = minOf(right, other.right)
        val newBottom = minOf(bottom, other.bottom)
        return BoundingBox(newLeft, newTop, newRight, newBottom)
    }

    fun iou(other: BoundingBox): Float {
        val inter = intersection().area()
        if (inter <= 0f) return 0f
        val union = area() + other.area() - inter
        return if (union > 0f) inter / union else 0f
    }

    fun scale(scaleX: Float, scaleY: Float): BoundingBox = BoundingBox(
        left = left * scaleX,
        top = top * scaleY,
        right = right * scaleX,
        bottom = bottom * scaleY
    )

    fun clamp(maxWidth: Float, maxHeight: Float): BoundingBox = BoundingBox(
        left = left.coerceIn(0f, maxWidth),
        top = top.coerceIn(0f, maxHeight),
        right = right.coerceIn(0f, maxWidth),
        bottom = bottom.coerceIn(0f, maxHeight)
    )

    companion object {
        fun fromXYWH(x: Float, y: Float, w: Float, h: Float): BoundingBox =
            BoundingBox(x - w / 2, y - h / 2, x + w / 2, y + h / 2)
    }
}

data class YOLODetection(
    val boundingBox: BoundingBox,
    val classId: Int,
    val className: String,
    val confidence: Float
)

object NMSUtil {

    fun applyNMS(
        detections: List<YOLODetection>,
        iouThreshold: Float = 0.45f,
        scoreThreshold: Float = 0.25f
    ): List<YOLODetection> {
        val filtered = detections.filter { it.confidence >= scoreThreshold }
        if (filtered.isEmpty()) return emptyList()

        val grouped = filtered.groupBy { it.classId }

        val results = mutableListOf<YOLODetection>()
        for ((_, groupDetections) in grouped) {
            val sorted = groupDetections.sortedByDescending { it.confidence }
            val selected = mutableListOf<YOLODetection>()

            for (detection in sorted) {
                var shouldKeep = true
                for (selectedDet in selected) {
                    if (detection.boundingBox.iou(selectedDet.boundingBox) > iouThreshold) {
                        shouldKeep = false
                        break
                    }
                }
                if (shouldKeep) {
                    selected.add(detection)
                }
            }
            results.addAll(selected)
        }

        return results.sortedByDescending { it.confidence }
    }
}
