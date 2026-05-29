package com.majiang.vision.strategy

import android.content.Context
import android.graphics.Bitmap
import com.majiang.model.Tile
import com.majiang.vision.RecognitionResult
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TFLiteRecognitionStrategy(
    private val context: Context,
    private val modelPath: String = "mahjong_model.tflite"
) : RecognitionStrategy {

    override val name: String = "TFLite本地模型"
    override val isAvailable: Boolean = false
    override val requiresNetwork: Boolean = false

    private var interpreter: Any? = null
    private var isInitialized = false

    fun initialize() {
        try {
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

            val options = org.tensorflow.lite.Interpreter.Options()
            options.setNumThreads(4)
            interpreter = org.tensorflow.lite.Interpreter(mappedByteBuffer, options)
            isInitialized = true
        } catch (_: Exception) {
            isInitialized = false
        }
    }

    override suspend fun recognize(bitmap: Bitmap): List<RecognitionResult> {
        if (!isInitialized || interpreter == null) return emptyList()

        val interp = interpreter as? org.tensorflow.lite.Interpreter ?: return emptyList()

        val resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val input = convertBitmapToBuffer(resized)

        val output = Array(1) { FloatArray(Tile.entries.size) }
        try {
            interp.run(input, output)
        } catch (_: Exception) {
            return emptyList()
        }

        val results = mutableListOf<RecognitionResult>()
        val maxResults = 5
        val sortedIndices = output[0].indices.sortedByDescending { output[0][it] }

        for (i in 0 until minOf(maxResults, sortedIndices.size)) {
            val index = sortedIndices[i]
            val confidence = output[0][index]
            if (confidence < 0.3f) break

            val tile = Tile.entries.getOrNull(index)
            if (tile != null) {
                results.add(
                    RecognitionResult(
                        tile = tile,
                        confidence = confidence,
                        boundingBox = null,
                        label = tile.displayName
                    )
                )
            }
        }

        return results
    }

    private fun convertBitmapToBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixelValue in intValues) {
            byteBuffer.putFloat(((pixelValue shr 16) and 0xFF) / 255.0f)
            byteBuffer.putFloat(((pixelValue shr 8) and 0xFF) / 255.0f)
            byteBuffer.putFloat((pixelValue and 0xFF) / 255.0f)
        }

        return byteBuffer
    }

    override fun release() {
        (interpreter as? org.tensorflow.lite.Interpreter)?.close()
        interpreter = null
        isInitialized = false
    }

    companion object {
        private const val INPUT_SIZE = 224
    }
}
