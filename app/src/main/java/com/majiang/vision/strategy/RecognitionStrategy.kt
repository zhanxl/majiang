package com.majiang.vision.strategy

import android.graphics.Bitmap
import com.majiang.model.Tile
import com.majiang.vision.RecognitionResult

interface RecognitionStrategy {

    val name: String

    val isAvailable: Boolean

    val requiresNetwork: Boolean

    suspend fun recognize(bitmap: Bitmap): List<RecognitionResult>

    fun release() {}
}
