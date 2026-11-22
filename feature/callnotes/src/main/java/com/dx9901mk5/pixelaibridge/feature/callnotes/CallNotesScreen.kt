package com.dx9901mk5.pixelaibridge.feature.callnotes

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.dx9901mk5.pixelaibridge.core.ai.GeminiAppHandoff
import com.dx9901mk5.pixelaibridge.core.ai.GeminiClient
import com.dx9901mk5.pixelaibridge.core.ai.GeminiResult
import com.dx9901mk5.pixelaibridge.core.designsystem.PixelSectionCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallNotesScreen(
    padding: PaddingValues,
    geminiClient: GeminiClient,
    store: CallNotesStore,
    hasApiKey: Boolean,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (!granted) onShowMessage("需要麥克風權限才能即時錄音")
    }

    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var isSummarizing by remember { mutableStateOf(false) }
    var transcriptInput by remember { mutableStateOf("") }
    var latestSummary by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf<List<CallNoteRecord>>(emptyList()) }

    LaunchedEffect(Unit) {
        notes = store.getRecent()
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                recorder?.release()
            } catch (_: Exception) {
            }
        }
    }

    suspend fun summarizeAudioBytes(title: String, bytes: ByteArray, mimeType: String) {
        if (bytes.size > 8 * 1024 * 1024) {
            onShowMessage("音訊檔過大（超過 8MB），建議剪輯後再匯入")
            return
        }
        isSummarizing = true
        val prompt = """
            你是 Google Pixel Call Notes 通話與會議紀錄助理。請聆聽這段音訊並以繁體中文產出結構化筆記：
            1. 【標題】（一句話概括主題）
            2. 【重點摘要】（條列 3~5 點）
            3. 【待辦事項 (Action Items)】（若無則寫無）
            4. 【精簡逐字稿】
        """.trimIndent()
        when (val res = geminiClient.generateWithAudio(prompt, bytes, mimeType, maxOutputTokens = 1024)) {
            is GeminiResult.Success -> {
                latestSummary = res.data
                store.insert(
                    CallNoteRecord(
                        title = title,
                        summary = res.data,
                        createdAtMs = System.currentTimeMillis()
                    )
                )
                notes = store.getRecent()
            }
            is GeminiResult.Error -> onShowMessage("摘要失敗：${res.message}")
        }
        isSummarizing = false
    }

    val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (!hasApiKey) {
            val prompt = "請聆聽這段通話或會議錄音，整理成繁體中文「重點摘要」與「待辦事項 (Action Items)」。"
            if (!GeminiAppHandoff.sendAudioWithPrompt(context, uri, prompt)) {
                onShowMessage("找不到可接收音訊的 Gemini App")
            }
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val mime = context.contentResolver.getType(uri) ?: "audio/mp4"
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            if (bytes == null) {
                onShowMessage("無法讀取音訊檔案")
                return@launch
            }
            summarizeAudioBytes("匯入錄音摘要", bytes, mime)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 12.dp,
            bottom = padding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            PixelSectionCard {
                Text(
                    text = "Call Notes 錄音轉錄與摘要",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "支援即時會議錄音，或直接匯入 vivo／OPPO／小米內建通話錄音檔，由 Gemini 自動產出重點與待辦事項。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        enabled = !isSummarizing,
                        onClick = {
                            if (!hasMicPermission) {
                                micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@Button
                            }
                            if (!isRecording) {
                                val recDir = File(context.cacheDir, "recordings").apply { mkdirs() }
                                val outFile = File(recDir, "callnote_${System.currentTimeMillis()}.m4a")
                                val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    MediaRecorder(context)
                                } else {
                                    @Suppress("DEPRECATION")
                                    MediaRecorder()
                                }
                                try {
                                    mr.setAudioSource(MediaRecorder.AudioSource.MIC)
                                    mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                    mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                    mr.setAudioSamplingRate(16000)
                                    mr.setAudioEncodingBitRate(32000)
                                    mr.setOutputFile(outFile.absolutePath)
                                    mr.prepare()
                                    mr.start()
                                    recorder = mr
                                    recordedFile = outFile
                                    isRecording = true
                                } catch (e: Exception) {
                                    mr.release()
                                    onShowMessage("無法啟動錄音：${e.localizedMessage}")
                                }
                            } else {
                                try {
                                    recorder?.stop()
                                } catch (_: Exception) {
                                }
                                recorder?.release()
                                recorder = null
                                isRecording = false
                                val file = recordedFile
                                if (file != null && file.exists()) {
                                    if (!hasApiKey) {
                                        val audioUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val prompt = "請聆聽這段錄音，整理成繁體中文「重點摘要」與「待辦事項 (Action Items)」。"
                                        if (!GeminiAppHandoff.sendAudioWithPrompt(context, audioUri, prompt)) {
                                            onShowMessage("找不到可接收音訊的 Gemini App")
                                        }
                                    } else {
                                        scope.launch {
                                            val bytes = withContext(Dispatchers.IO) { file.readBytes() }
                                            summarizeAudioBytes("現場錄音筆記", bytes, "audio/mp4")
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isRecording) {
                                if (hasApiKey) "停止並摘要" else "停止並送 Gemini"
                            } else {
                                "即時錄音"
                            }
                        )
                    }

                    OutlinedButton(
                        enabled = !isSummarizing && !isRecording,
                        onClick = { audioPicker.launch(arrayOf("audio/*")) }
                    ) {
                        Icon(Icons.Rounded.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("匯入音訊")
                    }
                }
                if (isSummarizing) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gemini 正在轉錄與整理重點…", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            PixelSectionCard {
                Text(
                    text = "文字逐字稿快速摘要",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = transcriptInput,
                    onValueChange = { transcriptInput = it },
                    placeholder = { Text("貼上會議紀錄或通話文字內容…") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = hasApiKey && !isSummarizing && transcriptInput.isNotBlank(),
                        onClick = {
                            scope.launch {
                                isSummarizing = true
                                val prompt = """
                                    請將以下通話或會議文字整理為繁體中文「重點摘要」與「待辦事項」：
                                    $transcriptInput
                                """.trimIndent()
                                when (val res = geminiClient.generateText(prompt, maxOutputTokens = 600)) {
                                    is GeminiResult.Success -> {
                                        latestSummary = res.data
                                        store.insert(
                                            CallNoteRecord(
                                                title = "文字筆記摘要",
                                                summary = res.data,
                                                createdAtMs = System.currentTimeMillis()
                                            )
                                        )
                                        notes = store.getRecent()
                                    }
                                    is GeminiResult.Error -> onShowMessage("摘要失敗：${res.message}")
                                }
                                isSummarizing = false
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("整理重點")
                    }

                    OutlinedButton(
                        enabled = transcriptInput.isNotBlank(),
                        onClick = {
                            val prompt = """
                                請幫我把以下通話或會議內容整理成「重點摘要」與「待辦事項 (Action Items)」：

                                $transcriptInput
                            """.trimIndent()
                            if (!GeminiAppHandoff.sendText(context, prompt)) {
                                onShowMessage("找不到可接收分享的 Gemini App")
                            }
                        }
                    ) {
                        Text("送給手機 Gemini")
                    }
                }
            }
        }

        latestSummary?.let { summary ->
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "最新 AI 摘要結果",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = summary, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Call Note", summary))
                                onShowMessage("已複製摘要")
                            }
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("複製摘要")
                        }
                    }
                }
            }
        }

        if (notes.isNotEmpty()) {
            item {
                Text(
                    text = "歷史通話／會議摘要 (${notes.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(notes, key = { it.id }) { note ->
                ElevatedCard(
                    onClick = { latestSummary = note.summary },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "${note.title} · ${formatNoteTime(note.createdAtMs)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = note.summary.take(120),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

private fun formatNoteTime(ms: Long): String {
    return SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(ms))
}
