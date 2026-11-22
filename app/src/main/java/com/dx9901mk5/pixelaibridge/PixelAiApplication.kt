package com.dx9901mk5.pixelaibridge

import android.app.Application
import com.dx9901mk5.pixelaibridge.core.ai.ApiKeyStore
import com.dx9901mk5.pixelaibridge.core.ai.GeminiClient
import com.dx9901mk5.pixelaibridge.core.shizuku.ShizukuBridge

class AppContainer(application: Application) {
    val apiKeyStore = ApiKeyStore(application)
    val geminiClient = GeminiClient(apiKeyStore)
    val shizukuBridge = ShizukuBridge(application)
}

class PixelAiApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
