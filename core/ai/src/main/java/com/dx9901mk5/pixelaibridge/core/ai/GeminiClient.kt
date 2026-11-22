package com.dx9901mk5.pixelaibridge.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed interface GeminiResult<out T> {
    data class Success<T>(val data: T) : GeminiResult<T>
    data class Error(val message: String, val code: Int? = null) : GeminiResult<Nothing>
}

/**
 * Lightweight Gemini REST client using the user's BYOK key.
 */
class GeminiClient internal constructor(
    private val keyStore: ApiKeyStore,
    private val httpClient: OkHttpClient
) {
    constructor(keyStore: ApiKeyStore) : this(
        keyStore = keyStore,
        httpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()
    )
    suspend fun testConnection(): GeminiResult<String> {
        return generateText(
            prompt = "Reply with only: PixelAI Bridge OK",
            maxOutputTokens = 256
        )
    }

    suspend fun generateText(
        prompt: String,
        model: String = keyStore.selectedModel.value,
        maxOutputTokens: Int = 512
    ): GeminiResult<String> {
        val payload = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
            put(
                "generationConfig",
                JSONObject().put("maxOutputTokens", maxOutputTokens)
            )
        }
        return executeGenerateContent(payload, model)
    }

    suspend fun generateWithImage(
        prompt: String,
        jpegBytes: ByteArray,
        model: String = keyStore.selectedModel.value,
        maxOutputTokens: Int = 512
    ): GeminiResult<String> {
        return generateWithInlineMedia(
            prompt = prompt,
            mediaBytes = jpegBytes,
            mimeType = "image/jpeg",
            model = model,
            maxOutputTokens = maxOutputTokens
        )
    }

    suspend fun generateWithAudio(
        prompt: String,
        audioBytes: ByteArray,
        mimeType: String = "audio/wav",
        model: String = keyStore.selectedModel.value,
        maxOutputTokens: Int = 1024
    ): GeminiResult<String> {
        return generateWithInlineMedia(
            prompt = prompt,
            mediaBytes = audioBytes,
            mimeType = mimeType,
            model = model,
            maxOutputTokens = maxOutputTokens
        )
    }

    private suspend fun generateWithInlineMedia(
        prompt: String,
        mediaBytes: ByteArray,
        mimeType: String,
        model: String,
        maxOutputTokens: Int
    ): GeminiResult<String> {
        val base64 = android.util.Base64.encodeToString(mediaBytes, android.util.Base64.NO_WRAP)
        val parts = JSONArray()
            .put(JSONObject().put("text", prompt))
            .put(
                JSONObject().put(
                    "inline_data",
                    JSONObject()
                        .put("mime_type", mimeType)
                        .put("data", base64)
                )
            )
        val payload = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().put("parts", parts)))
            put("generationConfig", JSONObject().put("maxOutputTokens", maxOutputTokens))
        }
        return executeGenerateContent(payload, model)
    }

    private suspend fun executeGenerateContent(
        payload: JSONObject,
        model: String
    ): GeminiResult<String> = withContext(Dispatchers.IO) {
        val apiKey = keyStore.getApiKey()
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Error("尚未設定 Gemini API Key")
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val msg = parseErrorMessage(rawBody) ?: "HTTP ${response.code}"
                    return@withContext GeminiResult.Error(msg, response.code)
                }
                val text = parseCandidateText(rawBody)
                    ?: return@withContext GeminiResult.Error("Gemini 回傳空白內容")
                GeminiResult.Success(text.trim())
            }
        } catch (e: Exception) {
            GeminiResult.Error(e.localizedMessage ?: "網路連線失敗")
        }
    }

    private fun parseCandidateText(rawJson: String): String? {
        return try {
            val root = JSONObject(rawJson)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val parts = candidates.getJSONObject(0)
                .optJSONObject("content")
                ?.optJSONArray("parts") ?: return null
            buildString {
                for (i in 0 until parts.length()) {
                    append(parts.getJSONObject(i).optString("text"))
                }
            }.ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseErrorMessage(rawJson: String): String? {
        return try {
            JSONObject(rawJson).optJSONObject("error")?.optString("message")?.ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
