package com.majiang.vision

import android.content.Context
import android.graphics.Bitmap
import com.majiang.vision.strategy.CloudVisionProvider
import com.majiang.vision.strategy.CloudVisionRecognitionStrategy
import com.majiang.vision.strategy.RecognitionStrategy
import com.majiang.vision.strategy.TFLiteRecognitionStrategy
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

enum class RecognitionMode {
    CLOUD_VISION,
    TFLITE
}

@Singleton
class TileRecognizer @Inject constructor(
    private val context: Context
) {
    private val strategies = mutableMapOf<RecognitionMode, RecognitionStrategy>()

    private var currentMode: RecognitionMode = RecognitionMode.CLOUD_VISION

    var confidenceThreshold: Float = RecognitionResult.CONFIDENCE_THRESHOLD

    val currentStrategy: RecognitionStrategy
        get() = getStrategy(currentMode)

    val currentModeName: String
        get() = currentStrategy.name

    val isConfigured: Boolean
        get() = currentStrategy.isAvailable

    fun setMode(mode: RecognitionMode) {
        currentMode = mode
        Timber.d("Recognition mode set to: ${currentStrategy.name}")
    }

    fun getMode(): RecognitionMode = currentMode

    fun configureCloudVision(
        apiKey: String,
        provider: CloudVisionProvider = CloudVisionProvider.QWEN,
        customEndpoint: String = ""
    ) {
        strategies[RecognitionMode.CLOUD_VISION] = CloudVisionRecognitionStrategy(
            apiKey = apiKey,
            apiProvider = provider,
            customEndpoint = customEndpoint
        )
    }

    fun configureTFLite(modelPath: String = "mahjong_model.tflite") {
        val strategy = TFLiteRecognitionStrategy(context, modelPath)
        strategy.initialize()
        strategies[RecognitionMode.TFLITE] = strategy
    }

    fun getAvailableModes(): List<RecognitionMode> {
        return RecognitionMode.entries.filter { mode ->
            getStrategy(mode).isAvailable
        }
    }

    suspend fun recognize(bitmap: Bitmap): List<RecognitionResult> {
        val strategy = currentStrategy
        if (!strategy.isAvailable) {
            Timber.w("Strategy ${strategy.name} is not available")
            return emptyList()
        }

        return try {
            val results = strategy.recognize(bitmap)
            results.filter { it.confidence >= confidenceThreshold }
        } catch (e: Exception) {
            Timber.e(e, "Recognition failed")
            emptyList()
        }
    }

    private fun getStrategy(mode: RecognitionMode): RecognitionStrategy {
        return strategies[mode] ?: object : RecognitionStrategy {
            override val name = "未配置"
            override val isAvailable = false
            override val requiresNetwork = false
            override suspend fun recognize(bitmap: Bitmap) = emptyList<RecognitionResult>()
        }
    }

    fun release() {
        strategies.values.forEach { it.release() }
        strategies.clear()
    }
}
