package com.majiang.vision

import android.content.Context
import android.graphics.Bitmap
import com.majiang.model.Tile
import com.majiang.model.TileCategory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TileRecognizer @Inject constructor(
    private val context: Context
) {
    private val detectorOptions = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
        .enableClassification()
        .build()

    private val objectDetector = ObjectDetection.getClient(detectorOptions)

    private var tfliteClassifier: TFLiteClassifier? = null

    var confidenceThreshold: Float = RecognitionResult.CONFIDENCE_THRESHOLD

    fun initializeClassifier(modelPath: String = "mahjong_model.tflite") {
        try {
            tfliteClassifier = TFLiteClassifier(context, modelPath)
            Timber.d("TFLite classifier initialized")
        } catch (e: Exception) {
            Timber.w(e, "TFLite classifier initialization failed, using ML Kit only")
        }
    }

    fun recognizeFromBitmap(bitmap: Bitmap, onResult: (List<RecognitionResult>) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)

        objectDetector.process(image)
            .addOnSuccessListener { detectedObjects ->
                val results = processDetectedObjects(detectedObjects, bitmap)
                onResult(results)
            }
            .addOnFailureListener { e ->
                Timber.e(e, "Object detection failed")
                onResult(emptyList())
            }
    }

    private fun processDetectedObjects(
        objects: List<DetectedObject>,
        bitmap: Bitmap
    ): List<RecognitionResult> {
        val results = mutableListOf<RecognitionResult>()

        for (obj in objects) {
            val boundingBox = obj.boundingBox?.let { box ->
                BoundingBox(
                    left = box.left.toFloat() / bitmap.width,
                    top = box.top.toFloat() / bitmap.height,
                    right = box.right.toFloat() / bitmap.width,
                    bottom = box.bottom.toFloat() / bitmap.height
                )
            }

            if (obj.labels.isNotEmpty()) {
                val label = obj.labels.first()
                val tile = mapLabelToTile(label.text)
                results.add(
                    RecognitionResult(
                        tile = tile,
                        confidence = label.confidence,
                        boundingBox = boundingBox,
                        label = label.text
                    )
                )
            } else if (boundingBox != null) {
                val croppedBitmap = cropBitmap(bitmap, obj.boundingBox)
                val tfResult = classifyWithTFLite(croppedBitmap)
                if (tfResult != null) {
                    results.add(tfResult.copy(boundingBox = boundingBox))
                }
            }
        }

        return results.filter { it.confidence >= confidenceThreshold }
    }

    private fun classifyWithTFLite(bitmap: Bitmap): RecognitionResult? {
        val classifier = tfliteClassifier ?: return null
        return classifier.classify(bitmap)
    }

    private fun cropBitmap(bitmap: Bitmap, rect: android.graphics.Rect?): Bitmap? {
        if (rect == null) return null
        val left = maxOf(0, rect.left)
        val top = maxOf(0, rect.top)
        val right = minOf(bitmap.width, rect.right)
        val bottom = minOf(bitmap.height, rect.bottom)
        if (right <= left || bottom <= top) return null
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    }

    private fun mapLabelToTile(label: String): Tile? {
        return try {
            Tile.valueOf(label)
        } catch (_: Exception) {
            null
        }
    }

    fun release() {
        objectDetector.close()
        tfliteClassifier?.close()
        tfliteClassifier = null
    }
}

private class TFLiteClassifier(
    context: Context,
    modelPath: String
) {
    private var interpreter: org.tensorflow.lite.Interpreter? = null

    init {
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val inputStream = assetFileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val mappedByteBuffer = fileChannel.map(
            java.nio.channels.FileChannel.MapMode.READ_ONLY,
            startOffset,
            declaredLength
        )
        interpreter = org.tensorflow.lite.Interpreter(mappedByteBuffer)
    }

    fun classify(bitmap: Bitmap): RecognitionResult? {
        val interp = interpreter ?: return null

        val resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val input = convertBitmapToBuffer(resized)

        val output = Array(1) { FloatArray(Tile.entries.size) }
        interp.run(input, output)

        val maxIndex = output[0].indices.maxByOrNull { output[0][it] } ?: return null
        val confidence = output[0][maxIndex]
        val tile = Tile.entries.getOrNull(maxIndex)

        return RecognitionResult(
            tile = tile,
            confidence = confidence,
            boundingBox = null,
            label = tile?.name ?: "unknown"
        )
    }

    private fun convertBitmapToBuffer(bitmap: Bitmap): java.nio.ByteBuffer {
        val byteBuffer = java.nio.ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(java.nio.ByteOrder.nativeOrder())

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixelValue in intValues) {
            byteBuffer.putFloat(((pixelValue shr 16) and 0xFF) / 255.0f)
            byteBuffer.putFloat(((pixelValue shr 8) and 0xFF) / 255.0f)
            byteBuffer.putFloat((pixelValue and 0xFF) / 255.0f)
        }

        return byteBuffer
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    companion object {
        private const val INPUT_SIZE = 224
    }
}
