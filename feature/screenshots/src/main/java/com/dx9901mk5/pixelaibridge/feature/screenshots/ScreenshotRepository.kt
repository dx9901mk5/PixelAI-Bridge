package com.dx9901mk5.pixelaibridge.feature.screenshots

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.dx9901mk5.pixelaibridge.core.ai.ApiKeyStore
import com.dx9901mk5.pixelaibridge.core.ai.GeminiClient
import com.dx9901mk5.pixelaibridge.core.ai.GeminiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScanSummary(
    val scannedNew: Int,
    val totalIndexed: Int
)

/**
 * Coordinates MediaStore screenshot discovery, local ML Kit OCR, and optional Gemini Vision indexing.
 */
class ScreenshotRepository(
    private val context: Context,
    private val indexStore: ScreenshotIndexStore,
    private val ocrEngine: ScreenshotOcrEngine,
    private val geminiClient: GeminiClient,
    private val apiKeyStore: ApiKeyStore
) {
    suspend fun search(query: String): List<ScreenshotRecord> = indexStore.search(query)

    suspend fun count(): Int = indexStore.count()

    suspend fun scanNewScreenshots(
        maxBatch: Int = 15,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): ScanSummary = withContext(Dispatchers.IO) {
        val indexedIds = indexStore.getIndexedIds()
        val candidates = queryMediaStoreScreenshots(limit = 80)
            .filter { it.mediaId !in indexedIds }
            .take(maxBatch)

        val total = candidates.size
        candidates.forEachIndexed { index, item ->
            onProgress(index + 1, total)
            val uri = Uri.parse(item.contentUri)
            val ocrText = ocrEngine.recognizeText(uri)
            val (category, summary) = if (apiKeyStore.hasApiKey.value && ocrText.isNotBlank()) {
                summarizeWithGemini(uri, ocrText)
            } else {
                inferLocalCategory(ocrText) to buildLocalSnippet(ocrText)
            }

            indexStore.upsert(
                ScreenshotRecord(
                    mediaId = item.mediaId,
                    contentUri = item.contentUri,
                    displayName = item.displayName,
                    dateAddedSec = item.dateAddedSec,
                    ocrText = ocrText,
                    category = category,
                    aiSummary = summary,
                    indexedAtMs = System.currentTimeMillis()
                )
            )
        }

        ScanSummary(scannedNew = total, totalIndexed = indexStore.count())
    }

    suspend fun reanalyzeWithGemini(record: ScreenshotRecord): ScreenshotRecord? {
        if (!apiKeyStore.hasApiKey.value) return null
        val uri = Uri.parse(record.contentUri)
        val (category, summary) = summarizeWithGemini(uri, record.ocrText)
        val updated = record.copy(
            category = category,
            aiSummary = summary,
            indexedAtMs = System.currentTimeMillis()
        )
        indexStore.upsert(updated)
        return updated
    }

    suspend fun askAcrossScreenshots(question: String): GeminiResult<String> {
        val allRecords = indexStore.search("").take(25)
        if (allRecords.isEmpty()) {
            return GeminiResult.Error("目前尚無已索引的截圖，請先掃描截圖。")
        }
        val contextBlock = buildString {
            allRecords.forEachIndexed { idx, rec ->
                append("[#${idx + 1} ${rec.displayName} | ${rec.category}]\n")
                if (rec.aiSummary.isNotBlank()) {
                    append("摘要: ${rec.aiSummary}\n")
                }
                if (rec.ocrText.isNotBlank()) {
                    append("文字: ${rec.ocrText.take(400)}\n")
                }
                append("\n")
            }
        }
        val prompt = """
            你是 PixelAI Bridge 的截圖記憶助理。請根據以下使用者手機截圖的 OCR 與摘要紀錄，精準回答使用者的問題，並標註來源截圖檔名（例如 [#1 xxx.png]）。若紀錄中找不到答案，請誠實說明。

            使用者問題：$question

            截圖紀錄：
            $contextBlock
        """.trimIndent()
        return geminiClient.generateText(prompt = prompt, maxOutputTokens = 600)
    }

    private suspend fun summarizeWithGemini(uri: Uri, ocrText: String): Pair<String, String> {
        val jpeg = ocrEngine.loadScaledJpeg(uri)
        val prompt = """
            請分析這張手機截圖，用繁體中文回覆一行，格式嚴格為：
            分類｜重點摘要（35字以內）
            分類請從以下擇一：票券、帳單、對話、設定、筆記、購物、其他。
            OCR文字參考：${ocrText.take(500)}
        """.trimIndent()

        val result = if (jpeg != null) {
            geminiClient.generateWithImage(prompt, jpeg, maxOutputTokens = 120)
        } else {
            geminiClient.generateText(prompt, maxOutputTokens = 120)
        }

        return when (result) {
            is GeminiResult.Success -> {
                val parts = result.data.split("｜", "|", limit = 2)
                if (parts.size == 2) {
                    parts[0].trim().take(12) to parts[1].trim()
                } else {
                    "AI 摘要" to result.data.trim()
                }
            }
            is GeminiResult.Error -> inferLocalCategory(ocrText) to buildLocalSnippet(ocrText)
        }
    }

    private fun queryMediaStoreScreenshots(limit: Int): List<MediaStoreItem> {
        val result = ArrayList<MediaStoreItem>()
        val projection = buildList {
            add(MediaStore.Images.Media._ID)
            add(MediaStore.Images.Media.DISPLAY_NAME)
            add(MediaStore.Images.Media.DATE_ADDED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Images.Media.RELATIVE_PATH)
            }
        }.toTypedArray()

        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        } else {
            "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        }
        val args = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf("%Screenshot%", "%Screenshot%")
        } else {
            arrayOf("%Screenshot%")
        }

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            args,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (cursor.moveToNext() && result.size < limit) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                ).toString()
                result.add(
                    MediaStoreItem(
                        mediaId = id,
                        contentUri = contentUri,
                        displayName = cursor.getString(nameCol) ?: "screenshot_$id.png",
                        dateAddedSec = cursor.getLong(dateCol)
                    )
                )
            }
        }
        return result
    }

    private fun inferLocalCategory(ocrText: String): String {
        val lower = ocrText.lowercase()
        return when {
            lower.contains("wi-fi") || lower.contains("wifi") || lower.contains("設定") -> "設定"
            lower.contains("取貨") || lower.contains("訂單") || lower.contains("nt$") -> "購物"
            lower.contains("高鐵") || lower.contains("航班") || lower.contains("qr") -> "票券"
            ocrText.isBlank() -> "純圖片"
            else -> "OCR 筆記"
        }
    }

    private fun buildLocalSnippet(ocrText: String): String {
        val singleLine = ocrText.replace('\n', ' ').trim()
        return if (singleLine.isBlank()) "（未偵測到文字）" else singleLine.take(70)
    }

    private data class MediaStoreItem(
        val mediaId: Long,
        val contentUri: String,
        val displayName: String,
        val dateAddedSec: Long
    )
}
