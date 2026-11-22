plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Redirect build outputs outside Google Drive virtual filesystem to avoid file-lock collisions.
subprojects {
    val buildRoot = java.io.File(System.getProperty("java.io.tmpdir"), "pixelai-bridge-build")
    val safeName = path.trimStart(':').replace(':', '_')
    layout.buildDirectory.set(java.io.File(buildRoot, safeName))
}
