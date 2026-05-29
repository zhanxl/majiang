package com.majiang.vision.strategy

import android.graphics.Bitmap
import android.util.Base64
import com.majiang.model.Tile
import com.majiang.model.TileCategory
import com.majiang.vision.RecognitionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class CloudVisionRecognitionStrategy(
    private val apiKey: String = "",
    private val apiProvider: CloudVisionProvider = CloudVisionProvider.OPENAI,
    private val customEndpoint: String = ""
) : RecognitionStrategy {

    override val name: String = "云端视觉识别"
    override val isAvailable: Boolean = apiKey.isNotBlank()
    override val requiresNetwork: Boolean = true

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
        val (url, requestBody) = when (apiProvider) {
            CloudVisionProvider.OPENAI -> buildOpenAIRequest(base64Image)
            CloudVisionProvider.QWEN -> buildQwenRequest(base64Image)
            CloudVisionProvider.CUSTOM -> buildCustomRequest(base64Image)
        }

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "Bearer $apiKey")
        connection.doOutput = true
        connection.connectTimeout = 30000
        connection.readTimeout = 60000

        connection.outputStream.use { os ->
            os.write(requestBody.toByteArray(Charsets.UTF_8))
        }

        return if (connection.responseCode == 200) {
            connection.inputStream.bufferedReader().readText()
        } else {
            ""
        }
    }

    private fun buildOpenAIRequest(base64Image: String): Pair<String, String> {
        val url = "https://api.openai.com/v1/chat/completions"
        val body = JSONObject().apply {
            put("model", "gpt-4o")
            put("max_tokens", 1000)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", VISION_PROMPT)
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", "data:image/jpeg;base64,$base64Image")
                                put("detail", "high")
                            })
                        })
                    })
                })
            })
        }
        return url to body.toString()
    }

    private fun buildQwenRequest(base64Image: String): Pair<String, String> {
        val url = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
        val body = JSONObject().apply {
            put("model", "qwen-vl-max")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", VISION_PROMPT)
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", "data:image/jpeg;base64,$base64Image")
                            })
                        })
                    })
                })
            })
        }
        return url to body.toString()
    }

    private fun buildCustomRequest(base64Image: String): Pair<String, String> {
        val url = customEndpoint
        val body = JSONObject().apply {
            put("image", base64Image)
            put("prompt", VISION_PROMPT)
        }
        return url to body.toString()
    }

    private fun parseVisionResponse(response: String): List<RecognitionResult> {
        if (response.isBlank()) return emptyList()

        val results = mutableListOf<RecognitionResult>()

        try {
            val json = JSONObject(response)
            val content = json
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content", "")
                ?: ""

            val tileNames = extractTileNames(content)
            for (name in tileNames) {
                val tile = findTileByDisplayName(name)
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

    private fun findTileByDisplayName(name: String): Tile? {
        return Tile.entries.find { it.displayName == name }
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
