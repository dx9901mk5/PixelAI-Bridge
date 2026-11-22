package com.dx9901mk5.pixelaibridge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dx9901mk5.pixelaibridge.core.ai.ApiKeyStore
import com.dx9901mk5.pixelaibridge.core.ai.GeminiClient
import com.dx9901mk5.pixelaibridge.core.ai.GeminiResult
import com.dx9901mk5.pixelaibridge.core.shizuku.ShizukuBridge
import com.dx9901mk5.pixelaibridge.core.shizuku.ShizukuStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BionicModuleState(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val phaseLabel: String,
    val enabled: Boolean = false
)

class MainViewModel(
    private val apiKeyStore: ApiKeyStore,
    private val geminiClient: GeminiClient,
    private val shizukuBridge: ShizukuBridge
) : ViewModel() {

    val hasApiKey: StateFlow<Boolean> = apiKeyStore.hasApiKey
    val selectedModel: StateFlow<String> = apiKeyStore.selectedModel
    val shizukuStatus: StateFlow<ShizukuStatus> = shizukuBridge.status
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), shizukuBridge.status.value)

    private val _testStatus = MutableStateFlow<String?>(null)
    val testStatus: StateFlow<String?> = _testStatus.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    private val _modules = MutableStateFlow(
        listOf(
            BionicModuleState(
                id = "screenshots",
                title = "AI 截圖記憶庫",
                subtitle = "Pixel Screenshots 仿生 · 自動 OCR 與語意檢索",
                badge = "Vision",
                phaseLabel = "已整合",
                enabled = true
            ),
            BionicModuleState(
                id = "now_playing",
                title = "聽歌識曲仿生",
                subtitle = "Now Playing Bionic · 音訊取樣識曲與歌詞猜歌",
                badge = "Audio",
                phaseLabel = "已整合",
                enabled = true
            ),
            BionicModuleState(
                id = "magic_cue",
                title = "智慧語境建議",
                subtitle = "Magic Cue · 通知監聽與一鍵智慧回覆",
                badge = "Context",
                phaseLabel = "已整合",
                enabled = true
            ),
            BionicModuleState(
                id = "pixel_studio",
                title = "AI 圖像工作室",
                subtitle = "Pixel Studio · 莫奈桌布生成與修圖分析",
                badge = "Studio",
                phaseLabel = "已整合",
                enabled = true
            ),
            BionicModuleState(
                id = "call_notes",
                title = "通話與會議摘要",
                subtitle = "Call Notes · 錄音轉錄與結構化重點整理",
                badge = "Summary",
                phaseLabel = "已整合",
                enabled = true
            )
        )
    )
    val modules: StateFlow<List<BionicModuleState>> = _modules.asStateFlow()

    fun toggleModule(id: String, enabled: Boolean) {
        _modules.update { list ->
            list.map { if (it.id == id) it.copy(enabled = enabled) else it }
        }
    }

    fun getCurrentApiKey(): String = apiKeyStore.getApiKey()

    fun saveApiKey(key: String) {
        apiKeyStore.saveApiKey(key)
        _testStatus.value = if (key.isNotBlank()) "API Key 已加密儲存" else "已清除 API Key"
    }

    fun selectModel(model: String) {
        apiKeyStore.setModel(model)
    }

    fun testGeminiConnection() {
        if (_isTesting.value) return
        viewModelScope.launch {
            _isTesting.value = true
            _testStatus.value = "正在測試 Gemini 連線…"
            when (val result = geminiClient.testConnection()) {
                is GeminiResult.Success -> _testStatus.value = "連線成功：${result.data}"
                is GeminiResult.Error -> _testStatus.value = "連線失敗：${result.message}"
            }
            _isTesting.value = false
        }
    }

    fun refreshShizuku() {
        shizukuBridge.refreshStatus()
    }

    fun requestShizukuPermission() {
        shizukuBridge.requestPermission()
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(
                apiKeyStore = container.apiKeyStore,
                geminiClient = container.geminiClient,
                shizukuBridge = container.shizukuBridge
            ) as T
        }
    }
}
