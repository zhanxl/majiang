package com.majiang.vision

import android.content.Context
import android.graphics.Bitmap
import com.majiang.model.Tile
import org.tensorflow.lite.Interpreter
import timber.log.Timber
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class YOLOv8Detector(
    context: Context,
    modelPath: String = "mahjong_yolov8.tflite",
    labelsPath: String = "mahjong_labels.txt"
) {
    private var interpreter: Interpreter? = null
    private val labels: List<String>
    private val tileSizeMap: Map<String, Tile>

    var inputSize: Int = 640
        private set
    var confThreshold: Float = 0.25f
    var iouThreshold: Float = 0.45f

    init {
        labels = loadLabels(context, labelsPath)
        tileSizeMap = buildTileMap()
        try {
            val modelBuffer = loadModelFile(context, modelPath)
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(modelBuffer, options)

            val inputShape = interpreter?.getInputTensor(0)?.shape()
            if (inputShape != null && inputShape.size == 4) {
                inputSize = inputShape[1]
            }

            Timber.d("YOLOv8 detector initialized: inputSize=$inputSize, labels=${labels.size}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load YOLOv8 model from $modelPath")
        }
    }

    fun detect(bitmap: Bitmap): List<YOLODetection> {
        val interp = interpreter ?: return emptyList()

        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val inputBuffer = preprocessImage(resized)

        val outputShape = interp.getOutputTensor(0).shape()
        val output = Array(1) { Array(outputShape[1]) { FloatArray(outputShape[2]) } }

        try {
            interp.run(inputBuffer, output)
        } catch (e: Exception) {
            Timber.e(e, "YOLOv8 inference failed")
            return emptyList()
        }

        val rawDetections = parseYOLOv8Output(output, outputShape)
        val nmsResults = NMSUtil.applyNMS(rawDetections, iouThreshold, confThreshold)

        val scaleX = bitmap.width.toFloat() / inputSize
        val scaleY = bitmap.height.toFloat() / inputSize

        return nmsResults.map { det ->
            det.copy(
                boundingBox = det.boundingBox.scale(scaleX, scaleY)
                    .clamp(bitmap.width.toFloat(), bitmap.height.toFloat())
            )
        }
    }

    fun detectTiles(bitmap: Bitmap): List<TileDetection> {
        return detect(bitmap).mapNotNull { detection ->
            val tile = tileSizeMap[detection.className]
            if (tile != null) {
                TileDetection(
                    tile = tile,
                    confidence = detection.confidence,
                    boundingBox = detection.boundingBox
                )
            } else {
                Timber.w("Unknown tile class: ${detection.className}")
                null
            }
        }
    }

    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixelValue in intValues) {
            byteBuffer.putFloat(((pixelValue shr 16) and 0xFF) / 255.0f)
            byteBuffer.putFloat(((pixelValue shr 8) and 0xFF) / 255.0f)
            byteBuffer.putFloat((pixelValue and 0xFF) / 255.0f)
        }

        return byteBuffer
    }

    private fun parseYOLOv8Output(
        output: Array<Array<FloatArray>>,
        outputShape: IntArray
    ): List<YOLODetection> {
        val detections = mutableListOf<YOLODetection>()
        val numDetections = outputShape[1]
        val numValues = outputShape[2]

        val numClasses = numValues - 4

        for (i in 0 until numDetections) {
            val cx = output[0][i][0]
            val cy = output[0][i][1]
            val w = output[0][i][2]
            val h = output[0][i][3]

            var maxClassScore = 0f
            var maxClassId = 0

            for (c in 0 until numClasses) {
                val score = output[0][i][4 + c]
                if (score > maxClassScore) {
                    maxClassScore = score
                    maxClassId = c
                }
            }

            if (maxClassScore < confThreshold) continue

            val box = BoundingBox.fromXYWH(cx, cy, w, h)
            val className = if (maxClassId < labels.size) labels[maxClassId] else "unknown_$maxClassId"

            detections.add(
                YOLODetection(
                    boundingBox = box,
                    classId = maxClassId,
                    className = className,
                    confidence = maxClassScore
                )
            )
        }

        return detections
    }

    private fun loadLabels(context: Context, path: String): List<String> {
        return try {
            context.assets.open(path).bufferedReader().readLines().filter { it.isNotBlank() }
        } catch (e: Exception) {
            Timber.w(e, "Failed to load labels from $path")
            emptyList()
        }
    }

    private fun buildTileMap(): Map<String, Tile> {
        val map = mutableMapOf<String, Tile>()
        for (tile in Tile.entries) {
            map[tile.name] = tile
        }
        return map
    }

    private fun loadModelFile(context: Context, path: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(path)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    val isInitialized: Boolean
        get() = interpreter != null
}

data class TileDetection(
    val tile: Tile,
    val confidence: Float,
    val boundingBox: BoundingBox
)
