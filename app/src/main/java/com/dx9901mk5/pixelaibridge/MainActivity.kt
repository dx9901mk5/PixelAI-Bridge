package com.dx9901mk5.pixelaibridge

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dx9901mk5.pixelaibridge.core.ai.ApiKeyStore
import com.dx9901mk5.pixelaibridge.core.designsystem.PixelAiBridgeTheme
import com.dx9901mk5.pixelaibridge.core.designsystem.PixelFeatureRowCard
import com.dx9901mk5.pixelaibridge.core.designsystem.PixelSectionCard
import com.dx9901mk5.pixelaibridge.core.designsystem.StatusBadgePill
import com.dx9901mk5.pixelaibridge.core.shizuku.ShizukuStatus
import com.dx9901mk5.pixelaibridge.feature.callnotes.CallNotesScreen
import com.dx9901mk5.pixelaibridge.feature.magiccue.MagicCueScreen
import com.dx9901mk5.pixelaibridge.feature.nowplaying.NowPlayingScreen
import com.dx9901mk5.pixelaibridge.feature.screenshots.ScreenshotsScreen
import com.dx9901mk5.pixelaibridge.feature.studio.PixelStudioScreen
import com.dx9901mk5.pixelaibridge.permissions.PermissionCenter
import com.dx9901mk5.pixelaibridge.updater.GitHubUpdater
import com.dx9901mk5.pixelaibridge.updater.ReleaseInfo
import com.dx9901mk5.pixelaibridge.updater.UpdateCheckResult
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as PixelAiApplication
        val sharedImageUri = extractSharedImageUri(intent)

        setContent {
            PixelAiBridgeTheme {
                val vm: MainViewModel = viewModel(factory = MainViewModel.Factory(app.container))
                PixelAiBridgeApp(
                    vm = vm,
                    container = app.container,
                    sharedImageUri = sharedImageUri
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun extractSharedImageUri(intent: Intent?): Uri? {
        if (intent?.action != Intent.ACTION_SEND) return null
        if (intent.type?.startsWith("image/") != true) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PixelAiBridgeApp(
    vm: MainViewModel,
    container: AppContainer,
    sharedImageUri: Uri?
) {
    var selectedTab by remember { mutableIntStateOf(if (sharedImageUri != null) 3 else 0) }
    var selectedToolboxSection by remember { mutableIntStateOf(if (sharedImageUri != null) 1 else 0) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val hasApiKey by vm.hasApiKey.collectAsStateWithLifecycle()
    val selectedModel by vm.selectedModel.collectAsStateWithLifecycle()
    val shizukuStatus by vm.shizukuStatus.collectAsStateWithLifecycle()
    val modules by vm.modules.collectAsStateWithLifecycle()
    val testStatus by vm.testStatus.collectAsStateWithLifecycle()
    val isTesting by vm.isTesting.collectAsStateWithLifecycle()

    val showMessage: (String) -> Unit = { msg ->
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PixelAI Bridge",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pixel 專屬功能仿生 · 全模組整合版",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Rounded.Dashboard, contentDescription = null) },
                    label = { Text("總覽") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Rounded.ImageSearch, contentDescription = null) },
                    label = { Text("截圖") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null) },
                    label = { Text("識曲") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Rounded.Widgets, contentDescription = null) },
                    label = { Text("AI 工具") }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                    label = { Text("設定") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> DashboardTab(
                padding = innerPadding,
                hasApiKey = hasApiKey,
                selectedModel = selectedModel,
                shizukuStatus = shizukuStatus,
                modules = modules,
                onToggleModule = vm::toggleModule,
                onNavigateModule = { id ->
                    when (id) {
                        "screenshots" -> selectedTab = 1
                        "now_playing" -> selectedTab = 2
                        "magic_cue" -> {
                            selectedToolboxSection = 0
                            selectedTab = 3
                        }
                        "pixel_studio" -> {
                            selectedToolboxSection = 1
                            selectedTab = 3
                        }
                        "call_notes" -> {
                            selectedToolboxSection = 2
                            selectedTab = 3
                        }
                    }
                },
                onGoToSettings = { selectedTab = 4 },
                onRequestShizuku = vm::requestShizukuPermission
            )

            1 -> ScreenshotsScreen(
                padding = innerPadding,
                repository = container.screenshotRepository,
                hasApiKey = hasApiKey,
                onShowMessage = showMessage
            )

            2 -> NowPlayingScreen(
                padding = innerPadding,
                recognizer = container.nowPlayingRecognizer,
                hasApiKey = hasApiKey,
                onShowMessage = showMessage
            )

            3 -> ToolboxTab(
                padding = innerPadding,
                selectedSection = selectedToolboxSection,
                onSelectSection = { selectedToolboxSection = it },
                container = container,
                hasApiKey = hasApiKey,
                sharedImageUri = sharedImageUri,
                onShowMessage = showMessage
            )

            4 -> SettingsTab(
                padding = innerPadding,
                initialApiKey = vm.getCurrentApiKey(),
                selectedModel = selectedModel,
                shizukuStatus = shizukuStatus,
                testStatus = testStatus,
                isTesting = isTesting,
                onSaveApiKey = vm::saveApiKey,
                onSelectModel = vm::selectModel,
                onTestConnection = vm::testGeminiConnection,
                onRefreshShizuku = vm::refreshShizuku,
                onRequestShizuku = vm::requestShizukuPermission,
                onShowMessage = showMessage
            )
        }
    }
}

@Composable
private fun DashboardTab(
    padding: PaddingValues,
    hasApiKey: Boolean,
    selectedModel: String,
    shizukuStatus: ShizukuStatus,
    modules: List<BionicModuleState>,
    onToggleModule: (String, Boolean) -> Unit,
    onNavigateModule: (String) -> Unit,
    onGoToSettings: () -> Unit,
    onRequestShizuku: () -> Unit
) {
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
            PixelSectionCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "PixelAI Bridge 全功能已就緒",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "已整合截圖記憶庫、Now Playing 識曲、Magic Cue、Pixel Studio 桌布／修圖與 Call Notes 摘要。點擊下方任一卡片即可直接進入。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (!hasApiKey) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = onGoToSettings) {
                        Text("前往設定 Gemini API Key")
                    }
                }
            }
        }

        item {
            PixelSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gemini ($selectedModel) · Shizuku",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = shizukuDescription(shizukuStatus),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusBadgePill(
                        label = if (hasApiKey) "AI 已就緒" else "未設金鑰",
                        isPositive = hasApiKey
                    )
                }
                if (shizukuStatus == ShizukuStatus.PERMISSION_REQUIRED) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = onRequestShizuku) {
                        Text("立即授權 Shizuku")
                    }
                }
            }
        }

        items(modules, key = { it.id }) { module ->
            PixelFeatureRowCard(
                title = module.title,
                subtitle = module.subtitle,
                badge = "${module.badge} · ${module.phaseLabel}",
                icon = iconForModule(module.id),
                enabled = module.enabled,
                onToggle = { onToggleModule(module.id, it) },
                onClick = { onNavigateModule(module.id) }
            )
        }
    }
}

@Composable
private fun ToolboxTab(
    padding: PaddingValues,
    selectedSection: Int,
    onSelectSection: (Int) -> Unit,
    container: AppContainer,
    hasApiKey: Boolean,
    sharedImageUri: Uri?,
    onShowMessage: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedSection == 0,
                onClick = { onSelectSection(0) },
                label = { Text("Magic Cue") }
            )
            FilterChip(
                selected = selectedSection == 1,
                onClick = { onSelectSection(1) },
                label = { Text("Pixel Studio") }
            )
            FilterChip(
                selected = selectedSection == 2,
                onClick = { onSelectSection(2) },
                label = { Text("Call Notes") }
            )
        }

        val childPadding = PaddingValues(
            top = 4.dp,
            bottom = padding.calculateBottomPadding()
        )
        when (selectedSection) {
            0 -> MagicCueScreen(
                padding = childPadding,
                geminiClient = container.geminiClient,
                hasApiKey = hasApiKey,
                onShowMessage = onShowMessage
            )
            1 -> PixelStudioScreen(
                padding = childPadding,
                geminiClient = container.geminiClient,
                hasApiKey = hasApiKey,
                initialSharedImageUri = sharedImageUri,
                onShowMessage = onShowMessage
            )
            else -> CallNotesScreen(
                padding = childPadding,
                geminiClient = container.geminiClient,
                store = container.callNotesStore,
                hasApiKey = hasApiKey,
                onShowMessage = onShowMessage
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsTab(
    padding: PaddingValues,
    initialApiKey: String,
    selectedModel: String,
    shizukuStatus: ShizukuStatus,
    testStatus: String?,
    isTesting: Boolean,
    onSaveApiKey: (String) -> Unit,
    onSelectModel: (String) -> Unit,
    onTestConnection: () -> Unit,
    onRefreshShizuku: () -> Unit,
    onRequestShizuku: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updater = remember { GitHubUpdater() }

    var keyInput by remember(initialApiKey) { mutableStateOf(initialApiKey) }
    var permissions by remember { mutableStateOf(PermissionCenter.collectStatuses(context)) }
    var pendingPermId by remember { mutableStateOf<String?>(null) }

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var releaseInfo by remember { mutableStateOf<ReleaseInfo?>(null) }
    var updateMessage by remember { mutableStateOf<String?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissions = PermissionCenter.collectStatuses(context)
    }

    val runtimePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissions = PermissionCenter.collectStatuses(context)
        if (!granted) {
            pendingPermId?.let { PermissionCenter.openPermissionSettings(context, it) }
        }
        pendingPermId = null
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
            PixelSectionCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Key, contentDescription = null)
                    Text(
                        text = "Gemini API (BYOK)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("AIza...") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ApiKeyStore.SUPPORTED_MODELS.forEach { model ->
                        FilterChip(
                            selected = selectedModel == model,
                            onClick = { onSelectModel(model) },
                            label = { Text(model) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { onSaveApiKey(keyInput) }) {
                        Text("儲存金鑰")
                    }
                    OutlinedButton(
                        onClick = onTestConnection,
                        enabled = !isTesting
                    ) {
                        Text(if (isTesting) "測試中…" else "測試連線")
                    }
                }
                if (testStatus != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = testStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            PixelSectionCard {
                Text(
                    text = "Shizuku 系統橋接",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = shizukuDescription(shizukuStatus),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onRefreshShizuku) {
                        Text("重新偵測")
                    }
                    if (shizukuStatus == ShizukuStatus.PERMISSION_REQUIRED) {
                        Button(onClick = onRequestShizuku) {
                            Text("請求授權")
                        }
                    }
                }
            }
        }

        item {
            PixelSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Security, contentDescription = null)
                        Text(
                            text = "權限檢查與導向開啟",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "${permissions.count { it.isGranted }}/${permissions.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "點擊任一權限項目即可直接請求授權或跳轉至系統設定頁面。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    permissions.forEach { perm ->
                        Surface(
                            onClick = {
                                if (!perm.isGranted && perm.runtimePermission != null) {
                                    pendingPermId = perm.id
                                    runtimePermLauncher.launch(perm.runtimePermission)
                                } else {
                                    PermissionCenter.openPermissionSettings(context, perm.id)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = perm.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = perm.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusBadgePill(
                                    label = if (perm.isGranted) "已開啟" else "前往設定",
                                    isPositive = perm.isGranted
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            PixelSectionCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.SystemUpdate, contentDescription = null)
                    Text(
                        text = "版本與自動檢查更新",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "目前版本：v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        enabled = !isCheckingUpdate && !isDownloadingUpdate,
                        onClick = {
                            scope.launch {
                                isCheckingUpdate = true
                                updateMessage = "正在檢查 GitHub 最新版本…"
                                when (val res = updater.checkLatestRelease()) {
                                    is UpdateCheckResult.Success -> {
                                        releaseInfo = res.info
                                        updateMessage = if (res.info.hasUpdate) {
                                            "發現新版本：${res.info.tagName}（${res.info.title}）"
                                        } else {
                                            "目前已是最新版本 (${res.info.tagName})"
                                        }
                                    }
                                    is UpdateCheckResult.Error -> {
                                        updateMessage = res.message
                                    }
                                }
                                isCheckingUpdate = false
                            }
                        }
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("檢查中…")
                        } else {
                            Text("檢查更新")
                        }
                    }

                    val apkUrl = releaseInfo?.apkDownloadUrl
                    if (apkUrl != null && releaseInfo?.hasUpdate == true) {
                        OutlinedButton(
                            enabled = !isDownloadingUpdate,
                            onClick = {
                                if (!updater.canInstallPackages(context)) {
                                    onShowMessage("請先允許安裝未知來源應用程式")
                                    PermissionCenter.openPermissionSettings(context, "install_unknown")
                                    return@OutlinedButton
                                }
                                scope.launch {
                                    isDownloadingUpdate = true
                                    downloadProgress = 0f
                                    updateMessage = "正在下載 APK 更新檔…"
                                    val result = updater.downloadAndInstall(context, apkUrl) { progress ->
                                        downloadProgress = progress
                                    }
                                    isDownloadingUpdate = false
                                    result.onSuccess {
                                        updateMessage = "下載完成，已啟動系統安裝程式"
                                    }.onFailure { err ->
                                        updateMessage = "下載失敗：${err.localizedMessage}"
                                    }
                                }
                            }
                        ) {
                            Text(if (isDownloadingUpdate) "下載中…" else "下載並安裝")
                        }
                    }
                }

                if (isDownloadingUpdate) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (updateMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = updateMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            ElevatedCard(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(GitHubUpdater.REPO_URL))
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GitHub 開源專案頁面",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = GitHubUpdater.REPO_URL,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = "開啟 GitHub"
                    )
                }
            }
        }
    }
}

private fun iconForModule(id: String): ImageVector = when (id) {
    "screenshots" -> Icons.Rounded.ImageSearch
    "now_playing" -> Icons.Rounded.GraphicEq
    "magic_cue" -> Icons.Rounded.TipsAndUpdates
    "pixel_studio" -> Icons.Rounded.Palette
    "call_notes" -> Icons.Rounded.Mic
    else -> Icons.Rounded.AutoAwesome
}

private fun shizukuDescription(status: ShizukuStatus): String = when (status) {
    ShizukuStatus.NOT_INSTALLED -> "裝置未安裝 Shizuku（標準功能可正常使用）"
    ShizukuStatus.NOT_RUNNING -> "已安裝 Shizuku，但服務尚未啟動"
    ShizukuStatus.PERMISSION_REQUIRED -> "Shizuku 運行中，點擊授權即可啟用特權橋接"
    ShizukuStatus.READY -> "Shizuku 已授權並正常運作中"
}
