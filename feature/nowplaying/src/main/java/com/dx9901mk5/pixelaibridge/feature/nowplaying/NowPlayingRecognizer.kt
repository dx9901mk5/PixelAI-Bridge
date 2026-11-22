package com.dx9901mk5.pixelaibridge.feature.nowplaying

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.dx9901mk5.pixelaibridge.core.ai.GeminiClient
import com.dx9901mk5.pixelaibridge.core.ai.GeminiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Captures a short ambient audio clip and uses Gemini multimodal audio understanding to identify music.
 */
class NowPlayingRecognizer(
    private val geminiClient: GeminiClient,
    private val store: NowPlayingStore
) {
    suspend fun getHistory(): List<NowPlayingTrack> = store.getRecent()

    suspend fun toggleFavorite(track: NowPlayingTrack) {
        store.setFavorite(track.id, !track.isFavorite)
    }

    @SuppressLint("MissingPermission")
    suspend fun listenAndIdentify(durationSeconds: Int = 6): GeminiResult<NowPlayingTrack> {
        val wavBytes = recordWavSample(durationSeconds)
            ?: return GeminiResult.Error("無法啟動麥克風錄音，請確認麥克風權限與裝置狀態。")

        val prompt = """
            請聆聽這段環境音訊並辨識其中的音樂（歌曲名稱、歌手/演奏者）。
            請嚴格使用以下單行格式回覆，不要加上額外解釋：
            歌名｜歌手｜簡短說明（例如語言、曲風或辨識線索）
            如果完全聽不到音樂或無法辨識，請回覆：
            未辨識到歌曲｜未知｜環境聲音過小或非音樂內容
        """.trimIndent()

        return when (val result = geminiClient.generateWithAudio(prompt, wavBytes, "audio/wav", maxOutputTokens = 160)) {
            is GeminiResult.Success -> {
                val track = parseAndSaveTrack(result.data)
                GeminiResult.Success(track)
            }
            is GeminiResult.Error -> result
        }
    }

    suspend fun identifyFromLyricsOrHint(hint: String): GeminiResult<NowPlayingTrack> {
        val prompt = """
            使用者記得以下歌詞、旋律線索或描述：「$hint」。
            請猜測最符合的歌曲，嚴格使用以下單行格式回覆：
            歌名｜歌手｜符合線索說明
        """.trimIndent()
        return when (val result = geminiClient.generateText(prompt, maxOutputTokens = 160)) {
            is GeminiResult.Success -> GeminiResult.Success(parseAndSaveTrack(result.data))
            is GeminiResult.Error -> result
        }
    }

    private suspend fun parseAndSaveTrack(raw: String): NowPlayingTrack {
        val parts = raw.lineSequence().firstOrNull()?.split("｜", "|") ?: emptyList()
        val title = parts.getOrNull(0)?.trim()?.ifBlank { "未知曲目" } ?: raw.trim().take(40)
        val artist = parts.getOrNull(1)?.trim()?.ifBlank { "未知歌手" } ?: "AI 推測"
        val note = parts.getOrNull(2)?.trim() ?: ""
        val track = NowPlayingTrack(
            title = title,
            artist = artist,
            confidenceNote = note,
            recognizedAtMs = System.currentTimeMillis()
        )
        return if (!title.startsWith("未辨識到")) {
            store.insert(track)
        } else {
            track
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun recordWavSample(durationSeconds: Int): ByteArray? = withContext(Dispatchers.IO) {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, encoding)
        if (minBuf <= 0) return@withContext null

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            encoding,
            minBuf * 2
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return@withContext null
        }

        val targetBytes = sampleRate * 2 * durationSeconds
        val pcmOut = ByteArrayOutputStream(targetBytes)
        val buffer = ByteArray(minBuf)

        try {
            recorder.startRecording()
            while (pcmOut.size() < targetBytes) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    pcmOut.write(buffer, 0, read)
                } else {
                    break
                }
            }
        } catch (_: Exception) {
            return@withContext null
        } finally {
            try {
                recorder.stop()
            } catch (_: Exception) {
            }
            recorder.release()
        }

        val pcmData = pcmOut.toByteArray()
        if (pcmData.isEmpty()) return@withContext null
        wrapPcmAsWav(pcmData, sampleRate, channels = 1, bitsPerSample = 16)
    }

    private fun wrapPcmAsWav(
        pcm: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ): ByteArray {
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        val byteRate = sampleRate * channels * bitsPerSample / 8
        header.put("RIFF".toByteArray())
        header.putInt(36 + pcm.size)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)
        header.putShort(1.toShort())
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort((channels * bitsPerSample / 8).toShort())
        header.putShort(bitsPerSample.toShort())
        header.put("data".toByteArray())
        header.putInt(pcm.size)

        return header.array() + pcm
    }
}
