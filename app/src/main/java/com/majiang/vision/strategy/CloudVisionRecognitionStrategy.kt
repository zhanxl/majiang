package com.majiang.vision.strategy

import android.graphics.Bitmap
import android.util.Base64
import com.majiang.model.Tile
import com.majiang.vision.RecognitionResult
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val role: String,
    val content: List<ContentPart>
)

data class ContentPart(
    val type: String,
    val text: String? = null,
    @SerializedName("image_url")
    val imageUrl: ImageUrl? = null
)

data class ImageUrl(
    val url: String,
    val detail: String = "high"
)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerializedName("max_tokens")
    val maxTokens: Int = 1000
)

data class ChatResponse(
    val choices: List<Choice>?
)

data class Choice(
    val message: ChoiceMessage?
)

data class ChoiceMessage(
    val content: String?
)

class CloudVisionRecognitionStrategy(
    private val apiKey: String = "",
    private val apiProvider: CloudVisionProvider = CloudVisionProvider.QWEN,
    private val customEndpoint: String = ""
) : RecognitionStrategy {

    override val name: String = "云端视觉识别"
    override val isAvailable: Boolean = apiKey.isNotBlank()
    override val requiresNetwork: Boolean = true

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    override suspend fun recognize(bitmap: Bitmap): List<RecognitionResult> {
        if (!isAvailable) return emptyList()

        return withContext(Dispatchers.IO) {
            try {
                val base64Image = bitmapToBase64(bitmap)
                val response = callVisionApi(base64Image)
                parseVisionResponse(response)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun callVisionApi(base64Image: String): String {
        val (url, model) = when (apiProvider) {
            CloudVisionProvider.OPENAI ->
                "https://api.openai.com/v1/chat/completions" to "gpt-4o"
            CloudVisionProvider.QWEN ->
                "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions" to "qwen-vl-max"
            CloudVisionProvider.CUSTOM ->
                customEndpoint to "custom"
        }

        val chatRequest = ChatRequest(
            model = model,
            messages = listOf(
                ChatMessage(
                    role = "user",
                    content = listOf(
                        ContentPart(type = "text", text = VISION_PROMPT),
                        ContentPart(
                            type = "image_url",
                            imageUrl = ImageUrl(
                                url = "data:image/jpeg;base64,$base64Image",
                                detail = "high"
                            )
                        )
                    )
                )
            )
        )

        val jsonBody = gson.toJson(chatRequest)
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            response.body?.string() ?: ""
        } else {
            ""
        }
    }

    private fun parseVisionResponse(response: String): List<RecognitionResult> {
        if (response.isBlank()) return emptyList()

        val results = mutableListOf<RecognitionResult>()

        try {
            val chatResponse = gson.fromJson(response, ChatResponse::class.java)
            val content = chatResponse.choices?.firstOrNull()
                ?.message?.content ?: ""

            val tileNames = extractTileNames(content)
            for (name in tileNames) {
                val tile = Tile.entries.find { it.displayName == name }
                if (tile != null) {
                    results.add(
                        RecognitionResult(
                            tile = tile,
                            confidence = 0.85f,
                            boundingBox = null,
                            label = tile.displayName
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }

        return results
    }

    private fun extractTileNames(content: String): List<String> {
        val names = mutableListOf<String>()
        val allDisplayNames = Tile.entries.map { it.displayName }.sortedByDescending { it.length }

        var remaining = content
        while (remaining.isNotEmpty()) {
            var found = false
            for (name in allDisplayNames) {
                if (remaining.startsWith(name)) {
                    names.add(name)
                    remaining = remaining.substring(name.length).trim()
                        .removePrefix(",").removePrefix("，").removePrefix(" ").trim()
                    found = true
                    break
                }
            }
            if (!found) {
                remaining = remaining.substring(1)
            }
        }

        return names
    }

    companion object {
        private const val VISION_PROMPT = """请识别图片中的所有麻将牌，只输出牌名，用逗号分隔。
万子：一万、二万、三万、四万、五万、六万、七万、八万、九万
条子：一条、二条、三条、四条、五条、六条、七条、八条、九条
筒子：一筒、二筒、三筒、四筒、五筒、六筒、七筒、八筒、九筒
风牌：东、南、西、北
箭牌：中、发、白
示例输出：一万,二万,三万,东,中"""
    }
}

enum class CloudVisionProvider {
    OPENAI,
    QWEN,
    CUSTOM
}
