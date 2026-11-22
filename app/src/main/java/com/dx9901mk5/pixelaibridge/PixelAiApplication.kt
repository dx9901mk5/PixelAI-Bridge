package com.dx9901mk5.pixelaibridge

import android.app.Application
import com.dx9901mk5.pixelaibridge.core.ai.ApiKeyStore
import com.dx9901mk5.pixelaibridge.core.ai.GeminiClient
import com.dx9901mk5.pixelaibridge.core.shizuku.ShizukuBridge
import com.dx9901mk5.pixelaibridge.feature.screenshots.ScreenshotIndexStore
import com.dx9901mk5.pixelaibridge.feature.screenshots.ScreenshotOcrEngine
import com.dx9901mk5.pixelaibridge.feature.screenshots.ScreenshotRepository

class AppContainer(application: Application) {
    val apiKeyStore = ApiKeyStore(application)
    val geminiClient = GeminiClient(apiKeyStore)
    val shizukuBridge = ShizukuBridge(application)
    val screenshotRepository = ScreenshotRepository(
        context = application,
        indexStore = ScreenshotIndexStore(application),
        ocrEngine = ScreenshotOcrEngine(application),
        geminiClient = geminiClient,
        apiKeyStore = apiKeyStore
    )
}

class PixelAiApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
