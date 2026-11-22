# PixelAI-Bridge 本地開發與交接紀錄 | Local Development Log

> 本文件用於記錄專案進度、環境設定注意事項、已驗證之建置結果與後續交接事項。

---

## 1. 專案基本資訊 | Project Info

- **專案名稱**：PixelAI-Bridge（Pixel 專屬功能仿生）
- **Application ID**：`com.dx9901mk5.pixelaibridge`
- **本地工作區**：`D:\Google雲端硬碟\Antigravity\Pixel專屬功能仿生`
- **GitHub 儲存庫**：https://github.com/dx9901mk5/PixelAI-Bridge (`main` 分支)
- **目前版本**：`0.2.3` (`versionCode = 5`)

---

## 2. 開發環境與建置備忘 | Environment & Build Notes

- **JDK**：須使用 **JDK 17**（本機路徑：`C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`）。
  - 注意：系統預設 `JAVA_HOME` 指向 Android Studio JBR（Java 25），直接執行 Gradle 8.11.1 會失敗，建置前須切換 `JAVA_HOME` 至 JDK 17。
- **Gradle / AGP**：Gradle `8.11.1` + AGP `8.7.3` + Kotlin `2.0.21`。
- **Android SDK**：`D:\Android\Sdk`（`compileSdk = 35`、`targetSdk = 35`、`minSdk = 26`）。
- **非 ASCII 路徑相容性**：因工作區路徑含中文（`Google雲端硬碟`、`Pixel專屬功能仿生`），已於 `gradle.properties` 設定 `android.overridePathCheck=true`，並於根目錄 `build.gradle.kts` 將各模組 `layout.buildDirectory` 重導向至 `%TEMP%\pixelai-bridge-build\`，避免 Google Drive 虛擬磁碟鎖定編譯中繼檔。
- **編譯指令（PowerShell）**：
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot"
  .\gradlew.bat assembleDebug --no-daemon
  # APK 產出路徑：%TEMP%\pixelai-bridge-build\app\outputs\apk\debug\app-debug.apk
  ```

---

## 3. 進度里程碑紀錄 | Milestone History

### 2026-11-22 — 專案初始化與 Phase 1 完成
- **Commit `88547fb`**：建立 GitHub 公開儲存庫 `PixelAI-Bridge`，新增 `.gitignore`（排除 `*.gdoc` 與建置產物）、中英文 `README.md` 與 `docs/APK_DESIGN_PLAN.md`。
- **Commit `b142c3e`**：完成 Phase 1 多模組專案骨架與基礎功能：
  - `:core:designsystem`：Material 3 Dynamic Color 主題與 Pixel 風格元件（`PixelSectionCard`、`StatusBadgePill`、`PixelFeatureRowCard`）。
  - `:core:ai`：`ApiKeyStore`（`EncryptedSharedPreferences` 加密儲存 BYOK 金鑰與模型選擇）及 `GeminiClient`（REST `generateContent` 連線測試與文字生成）。
  - `:core:shizuku`：`ShizukuBridge`（Binder 生命週期監聽、安裝/運行/授權狀態評估與一鍵請求權限）。
  - `:app`：三頁式導航（總覽 Dashboard、仿生模組列表、設定與橋接），支援 API Key 儲存、即時連線測試與 Shizuku 權限觸發。
- **APK 驗證**：`.\gradlew.bat assembleDebug --no-daemon` 建置成功（`BUILD SUCCESSFUL`），產出 `app/build/outputs/apk/debug/app-debug.apk`（18,246,815 bytes）。

### 2026-11-22 — Phase 2 完成：AI 截圖記憶庫 (Pixel Screenshots Bionic)
- **多模態升級 (`:core:ai`)**：`GeminiClient` 新增 `generateWithImage()` 支援圖片 + Prompt 分析，並共用 `executeGenerateContent()` 請求管線。
- **新模組 `:feature:screenshots`**：
  - `ScreenshotIndexStore`：本地 SQLite 索引儲存 OCR 文字、AI 分類與摘要，支援關鍵字檢索。
  - `ScreenshotOcrEngine`：整合 Google ML Kit 裝置端中文 + 英文 OCR（`text-recognition-chinese`）與 JPEG 縮放壓縮。
  - `ScreenshotRepository`：自動掃描 `MediaStore` 截圖、執行離線 OCR、選配呼叫 Gemini Vision 生成分類與摘要，並提供跨截圖「AI 問答」。
  - `ScreenshotsScreen`：整合至主程式第 2 分頁「截圖記憶」，支援媒體權限引導、即時搜尋、批次掃描進度、詳細檢視與一鍵複製 OCR 文字。
- **APK 驗證**：`.\gradlew.bat assembleDebug --no-daemon` 建置成功（`BUILD SUCCESSFUL`），產出 `%TEMP%\pixelai-bridge-build\app\outputs\apk\debug\app-debug.apk`（63,404,427 bytes，含 ML Kit 離線中文/英文 OCR 模型）。

### 2026-11-22 — v0.2.3 自動檢查與一鍵自動下載安裝
- 新增 `AutoUpdateWorker`（WorkManager 每 12 小時定時檢查 GitHub Releases，發現新版時背景自動下載 APK 並推播點擊即安裝通知）。
- 啟動 App 及點擊「檢查並自動更新」時，若偵測到新版本且已具備安裝未知應用程式權限，無需二次點擊即自動下載並喚起系統安裝程式。

### 2026-11-22 — v0.2.2 全模組免 API Key 橋接模式
- 新增 `GeminiAppHandoff`（`:core:ai`）：支援透過 Android `ACTION_SEND` 直接將文字、圖片、錄音檔導向手機本機已登入的 Gemini App（`com.google.android.apps.bard`），以及一鍵喚起 Google 聽歌／哼唱識曲。
- 五大模組全面解除 API Key 限制：截圖問答、聽歌識曲／猜歌、Magic Cue 語境建議、Pixel Studio 修圖分析與 Call Notes 即時錄音／音訊匯入皆可在無 API Key 狀態下直接運作。

### 2026-11-22 — v0.2.1 Gemini 模型端點更新
- 依最新官方 Gemini API 規格，將可用模型清單更新為 `gemini-3.8-flash`（預設）、`gemini-4-argon`、`gemini-3.5-flash-lite`，並於 `ApiKeyStore` 加入舊版 `2.5` 模型自動遷移邏輯。
- `GeminiClient.testConnection()` 的 `maxOutputTokens` 調整為 `256`，避免預設 Thinking token 佔滿導致測試失敗。
- `SettingsTab` 模型選擇列改為 `FlowRow` 自動換行排版。

### 2026-11-22 — Phase 3 & 4 全模組整合完成
- **音訊多模態擴充 (`:core:ai`)**：`GeminiClient` 新增 `generateWithAudio()`，支援 WAV／AAC 音訊串流與檔案分析。
- **新模組 `:feature:nowplaying`**：`AudioRecord` 6 秒環境音訊取樣識曲、歌詞／線索猜歌、歷史紀錄 SQLite 與一鍵跳轉 YouTube Music。
- **新模組 `:feature:magiccue`**：`MagicCueNotificationListener` 背景通知擷取、內建銀行／密碼 App 隱私黑名單、一鍵生成 3 則智慧回覆與關鍵資訊提取。
- **新模組 `:feature:studio`**：支援 `ACTION_SEND` 分享圖片接入、Gemini Vision 修圖構圖建議，以及本機莫奈幾何動態桌布渲染與相簿儲存。
- **新模組 `:feature:callnotes`**：`MediaRecorder` 即時錄音與通話錄音檔匯入，產出重點摘要、待辦事項與精簡逐字稿並支援歷史保存。
- **全功能整合 APK**：`.\gradlew.bat assembleDebug --no-daemon` 建置成功（`BUILD SUCCESSFUL`），並同步複製至工作區 `PixelAI-Bridge-debug.apk`（`.gitignore` 已排除不推上 GitHub）。

---

## 4. 待辦事項 | Next Up

- 實機安裝 `PixelAI-Bridge-debug.apk` 測試五大模組權限與 Gemini API 實測效果。
- 針對 vivo OriginOS / OPPO ColorOS / 小米 HyperOS 進行實機保活與 Shizuku 腳本擴充。
