package com.dx9901mk5.pixelaibridge.feature.nowplaying

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.dx9901mk5.pixelaibridge.core.ai.GeminiAppHandoff
import com.dx9901mk5.pixelaibridge.core.ai.GeminiResult
import com.dx9901mk5.pixelaibridge.core.designsystem.PixelSectionCard
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NowPlayingScreen(
    padding: PaddingValues,
    recognizer: NowPlayingRecognizer,
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
        if (!granted) onShowMessage("需要麥克風權限才能聆聽周遭音樂")
    }

    var isListening by remember { mutableStateOf(false) }
    var latestTrack by remember { mutableStateOf<NowPlayingTrack?>(null) }
    var history by remember { mutableStateOf<List<NowPlayingTrack>>(emptyList()) }
    var hintInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        history = recognizer.getHistory()
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 12.dp,
            bottom = padding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            PixelSectionCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Now Playing 聽歌識曲仿生",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "取樣 6 秒環境音訊並透過 Gemini 音訊多模態辨識曲目，支援一鍵跳轉串流搜尋與歷史時間軸。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    enabled = !isListening,
                    onClick = {
                        if (!hasApiKey) {
                            if (!GeminiAppHandoff.launchGoogleSoundSearch(context)) {
                                onShowMessage("無法啟動 Google 聽歌識曲")
                            }
                            return@Button
                        }
                        if (!hasMicPermission) {
                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            return@Button
                        }
                        scope.launch {
                            isListening = true
                            when (val res = recognizer.listenAndIdentify(6)) {
                                is GeminiResult.Success -> {
                                    latestTrack = res.data
                                    history = recognizer.getHistory()
                                    onShowMessage("辨識完成：${res.data.title}")
                                }
                                is GeminiResult.Error -> onShowMessage("辨識失敗：${res.message}")
                            }
                            isListening = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isListening) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在聆聽環境音樂（6 秒）…")
                    } else {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (hasApiKey) "開始聽歌識曲" else "啟動 Google 聽歌／哼唱識曲")
                    }
                }
            }
        }

        latestTrack?.let { track ->
            item {
                PixelSectionCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text(
                        text = "最新辨識結果",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${track.title} — ${track.artist}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (track.confidenceNote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = track.confidenceNote, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            val q = Uri.encode("${track.title} ${track.artist}")
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/search?q=$q"))
                            )
                        }
                    ) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("在 YouTube Music 搜尋")
                    }
                }
            }
        }

        item {
            PixelSectionCard {
                Text(
                    text = "歌詞／線索猜歌",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hintInput,
                        onValueChange = { hintInput = it },
                        placeholder = { Text("輸入幾句歌詞或旋律描述…") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        enabled = !isListening && hintInput.isNotBlank(),
                        onClick = {
                            if (!hasApiKey) {
                                val prompt = "請幫我根據以下歌詞或旋律線索找出歌名與歌手：\n$hintInput"
                                if (!GeminiAppHandoff.sendText(context, prompt)) {
                                    onShowMessage("找不到可接收分享的 Gemini App")
                                }
                                return@IconButton
                            }
                            scope.launch {
                                isListening = true
                                when (val res = recognizer.identifyFromLyricsOrHint(hintInput)) {
                                    is GeminiResult.Success -> {
                                        latestTrack = res.data
                                        history = recognizer.getHistory()
                                    }
                                    is GeminiResult.Error -> onShowMessage("查詢失敗：${res.message}")
                                }
                                isListening = false
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = "猜歌")
                    }
                }
            }
        }

        if (history.isNotEmpty()) {
            item {
                Text(
                    text = "識曲歷史紀錄 (${history.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(history, key = { it.id }) { item ->
                ElevatedCard(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${item.artist} · ${formatTime(item.recognizedAtMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                val q = Uri.encode("${item.title} ${item.artist}")
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/search?q=$q"))
                                )
                            }
                        ) {
                            Icon(Icons.Rounded.OpenInNew, contentDescription = "開啟")
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    return SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(ms))
}
