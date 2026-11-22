# PixelAI-Bridge 專案交接手冊 | Project Handover Guide

> **最後更新**：2026-11-22  
> **GitHub 儲存庫**：https://github.com/dx9901mk5/PixelAI-Bridge (`main` 分支)  
> **目前版本**：`0.2.2` (`versionCode = 4`，全模組免 API Key 橋接模式 + 最新 Gemini 模型)

---

## 1. 快速接手檢查清單 | Quick Onboarding Checklist

- **Application ID**：`com.dx9901mk5.pixelaibridge`
- **SDK 規格**：`minSdk = 26` / `compileSdk = 35` / `targetSdk = 35`
- **建置工具**：Gradle `8.11.1` + AGP `8.7.3` + Kotlin `2.0.21` + Compose BOM `2024.12.01`
- **JDK 要求**：**必須使用 JDK 17**（本機：`C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`）。
  - 本機預設 `JAVA_HOME` 指向 Android Studio JBR（Java 25），會導致 Gradle 8.11.1 失敗，執行前務必覆寫 `$env:JAVA_HOME`。
- **編譯與產出路徑**：
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot"
  .\gradlew.bat assembleDebug --no-daemon
  ```
  - 因工作區位於 Google Drive（含中文路徑且有檔案鎖定），`build.gradle.kts` 已將各模組 `layout.buildDirectory` 重導向至 `%TEMP%\pixelai-bridge-build\`，並於 `gradle.properties` 設定 `android.overridePathCheck=true`。
  - 已驗證可安裝 APK：`PixelAI-Bridge-debug.apk`（位於工作區根目錄，已列入 `.gitignore`）。

---

## 2. 模組架構與關鍵檔案索引 | Module & File Map

| 模組 | 職責 | 關鍵類別與檔案 |
| :--- | :--- | :--- |
| `:app` | 主程式入口、5 分頁導航、Share Sheet 接收 | `PixelAiApplication.kt`、`MainViewModel.kt`、`MainActivity.kt`、`AndroidManifest.xml` |
| `:core:designsystem` | Material 3 Dynamic Color 主題與元件 | `Theme.kt`、`PixelComponents.kt` |
| `:core:ai` | Gemini REST Client (Text/Image/Audio) 與加密金鑰儲存 | `ApiKeyStore.kt`、`GeminiClient.kt` |
| `:core:shizuku` | Shizuku Binder 狀態監測與權限請求 | `ShizukuBridge.kt` |
| `:feature:screenshots` | AI 截圖記憶庫（MediaStore + ML Kit OCR + Gemini 問答） | `ScreenshotIndexStore.kt`、`ScreenshotOcrEngine.kt`、`ScreenshotRepository.kt`、`ScreenshotsScreen.kt` |
| `:feature:nowplaying` | 聽歌識曲仿生（6 秒 WAV 取樣 + 歌詞猜歌 + 歷史） | `NowPlayingStore.kt`、`NowPlayingRecognizer.kt`、`NowPlayingScreen.kt` |
| `:feature:magiccue` | 智慧語境建議（通知監聽 + 隱私黑名單 + 智慧回覆） | `MagicCueBus.kt`、`MagicCueNotificationListener.kt`、`MagicCueScreen.kt` |
| `:feature:studio` | AI 圖像工作室（莫奈幾何桌布生成 + 修圖分析） | `PixelStudioScreen.kt` |
| `:feature:callnotes` | 通話與會議摘要（即時錄音 / 匯入音訊 / 文字整理） | `CallNotesStore.kt`、`CallNotesScreen.kt` |

---

## 3. 本機儲存與權限清單 | Local Storage & Permissions

- **SQLite 資料庫**：
  - `pixelai_screenshots.db`（截圖 OCR 與 AI 摘要索引）
  - `pixelai_nowplaying.db`（識曲歷史紀錄）
  - `pixelai_callnotes.db`（通話／會議摘要紀錄）
- **SharedPreferences**：
  - `pixelai_secure_prefs`（`EncryptedSharedPreferences`，儲存 Gemini API Key 與模型選擇；Keystore 異常裝置自動降級 `pixelai_prefs_fallback`）
  - `pixelai_magiccue_prefs`（Magic Cue 排除之 App 套件名稱黑名單）
- **AndroidManifest 權限與元件**：
  - `INTERNET`、`POST_NOTIFICATIONS`、`RECORD_AUDIO`、`READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE (<=32)`
  - `ShizukuProvider` (`rikka.shizuku.ShizukuProvider`)
  - `MagicCueNotificationListener` (`BIND_NOTIFICATION_LISTENER_SERVICE`)
  - `MainActivity` 支援 `ACTION_SEND` (`image/*`) 直接分享圖片進入 Pixel Studio

---

## 4. 已知限制與後續交接建議 | Known Items & Next Steps

1. **Kotlin 編譯警告**：`NowPlayingScreen.kt` 中 `Icons.Rounded.OpenInNew` 建議改為 `Icons.AutoMirrored.Rounded.OpenInNew`。
2. **進階系統整合（待實作）**：
   - 新增 `TileService` 讓使用者可從快速設定面板一鍵觸發 **Now Playing 識曲**。
   - 新增 **WorkManager** 背景自動監聽新截圖，無須手動點擊「掃描新截圖」。
   - 針對 **vivo OriginOS / OPPO ColorOS / 小米 HyperOS** 實作 Shizuku 輔助指令（如背景保活、通知監聽權限引導）。
   - 為 **Magic Cue** 加上選配的懸浮膠囊氣泡（`SYSTEM_ALERT_WINDOW`）。
3. **相關文件**：
   - [專案架構與設計規劃書](docs/APK_DESIGN_PLAN.md)
   - [開發里程碑紀錄](docs/DEV_LOG.md)
