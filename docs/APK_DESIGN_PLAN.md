# PixelAI-Bridge APK 規劃與架構設計書
# PixelAI-Bridge APK Design & Architecture Plan

## 1. 應用程式基本規格 | App Specifications

- **App Name**: PixelAI Bridge (Pixel 專屬功能仿生)
- **Application ID**: `com.dx9901mk5.pixelaibridge`
- **Min SDK**: API 26 (Android 8.0)
- **Target / Compile SDK**: API 35 (Android 15)
- **JDK**: Java 17
- **UI Framework**: Jetpack Compose + Material 3 (支援 Dynamic Color 莫奈取色，貼近 Pixel 原生視覺)

---

## 2. 專案目錄與模組結構 | Gradle Module Structure

採用清晰的模組化設計，方便各仿生功能獨立開發與測試：

```text
PixelAI-Bridge/
├── app/                          # 主程式進入點、導航、權限引導、設定頁
├── core/
│   ├── common/                   # 共用工具、Coroutine Dispatchers、Result 封裝
│   ├── designsystem/             # Material 3 主題、Pixel 風格元件、動態色彩
│   ├── data/                     # Room DB (截圖索引、識曲紀錄、摘要歷史)、EncryptedPrefs
│   ├── ai/                       # Gemini API 客戶端 (Text/Vision/Audio/Image) + ML Kit 封裝
│   └── shizuku/                  # Shizuku 狀態檢查、Binder 呼叫、免 Root 權限自動授予
└── feature/
    ├── nowplaying/               # 聽歌識曲仿生 (前景服務、歷史紀錄、鎖屏/狀態列顯示)
    ├── screenshots/              # AI 截圖記憶庫 (MediaStore 監聽、OCR、語意搜尋)
    ├── magiccue/                 # 語境智慧建議 (通知監聽、懸浮膠囊、快捷動作)
    ├── studio/                   # AI 圖像工作室 (桌布生成、修圖、貼圖去背)
    └── callnotes/                # 錄音轉錄與摘要 (音訊匯入/即時摘要)
```

---

## 3. 核心權限與隱私安全設計 | Permissions & Privacy Architecture

### 3.1 權限分級 (Permission Tiers)
1. **基礎權限 (Standard)**:
   - `INTERNET`：呼叫 Gemini API 與音樂辨識 API。
   - `READ_MEDIA_IMAGES` (API 33+) / `READ_EXTERNAL_STORAGE`：讀取新截圖以進行索引。
   - `POST_NOTIFICATIONS`：顯示 Now Playing 結果與語境建議。
   - `RECORD_AUDIO`：僅在使用者主動觸發或啟用環境識曲時使用。
2. **進階體驗權限 (Optional / User Opt-in)**:
   - `BIND_NOTIFICATION_LISTENER_SERVICE`：支援 Magic Cue 讀取訊息通知並產生智慧回覆/行程建議。
   - `SYSTEM_ALERT_WINDOW`：支援懸浮膠囊 / 智慧側邊欄快捷建議。
3. **Shizuku 增強權限 (Optional)**:
   - 透過 Shizuku 協助授予背景執行豁免、無障礙開關或系統設定寫入，改善陸版 ROM（OriginOS / ColorOS / HyperOS）殺後台問題。

### 3.2 隱私優先原則 (Privacy-First)
- **BYOK (Bring Your Own Key)**：使用者自行填入 Gemini API Key，並以 Android Keystore / `EncryptedSharedPreferences` 加密儲存。
- **本地預處理 (Local-First)**：截圖先於裝置端執行 ML Kit OCR，僅在需要深度語意摘要或使用者主動查詢時才送出文字/圖片至 Gemini。
- **敏感 App 黑名單**：銀行、密碼管理器、私密聊天 App 可加入排除清單，絕不擷取或分析其通知與畫面。

---

## 4. UI / UX 畫面規劃 | Screen Flow & UI Design

1. **Dashboard (首頁總覽)**：
   - Pixel 風格卡片式儀表板：顯示 Shizuku 連線狀態、Gemini API 連線狀態、今日已索引截圖數、最近辨識歌曲。
   - 各功能模組獨立開關與快速入口。
2. **Screenshots (截圖記憶庫)**：
   - 頂部自然語言搜尋列（例如：「找上週的機票截圖」、「找出 Wi-Fi 密碼」）。
   - 網格瀑布流展示截圖與 AI 自動標籤，點擊可展開 OCR 全文、AI 重點與一鍵複製。
3. **Now Playing (聽歌識曲歷史)**：
   - 時間軸顯示辨識到的曲目、歌手、時間點，支援一鍵跳轉 YouTube Music / Spotify。
4. **Magic Cue (語境橋接設定)**：
   - 設定觸發規則（行事曆提取、地址辨識、外語翻譯、智慧回覆）與 App 排除清單。
5. **Settings (設定與橋接中心)**：
   - Gemini 模型選擇（Flash / Pro）、API Key 管理、Shizuku 一鍵授權精靈、匯出/清除本地資料庫。

---

## 5. 分階段開發路線圖 | Phased Implementation Roadmap

### Phase 1: 基礎骨架與核心引擎 (Foundation & Core)
- [ ] 建立 Gradle Kotlin DSL (`libs.versions.toml`) 與多模組專案結構
- [ ] 實作 Material 3 Pixel 風格設計系統與首頁 Dashboard
- [ ] 實作 `core:ai`（Gemini API Key 設定、測試連線、基本文字/圖片請求）
- [ ] 實作 `core:shizuku`（Shizuku 綁定、狀態監測、權限檢查）

### Phase 2: MVP 首波主打功能 — AI 截圖記憶庫 (Screenshots Bionic)
- [ ] `MediaStore` + `WorkManager` 背景自動偵測新截圖
- [ ] 整合 ML Kit 裝置端 OCR 與 Room FTS 全文檢索
- [ ] 串接 Gemini Vision 自動分類、摘要與自然語言問答

### Phase 3: 聽歌識曲與語境橋接 (Now Playing & Magic Cue)
- [ ] 實作 Quick Settings Tile 與背景低耗電聽歌識曲服務
- [ ] 實作 `NotificationListenerService` + 懸浮建議氣泡 (Magic Cue)

### Phase 4: 圖像工作室與通話摘要 (Studio & Call Notes)
- [ ] 實作 Share Sheet 圖片接入與 AI 編輯/生成工作流
- [ ] 實作音訊檔案轉錄與結構化會議/通話摘要
- [ ] 針對 vivo OriginOS / OPPO ColorOS / 小米 HyperOS 進行相容性與保活調校
