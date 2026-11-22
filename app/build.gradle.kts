plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.dx9901mk5.pixelaibridge"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dx9901mk5.pixelaibridge"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "0.3.0"
    }

    signingConfigs {
        val fixedKeystore = rootProject.file("keystore/pixelai-bridge.jks")
        if (fixedKeystore.exists()) {
            create("pixelFixed") {
                storeFile = fixedKeystore
                storePassword = System.getenv("PIXELAI_KEYSTORE_PASS") ?: "PixelAIBridge2026!"
                keyAlias = "pixelai"
                keyPassword = System.getenv("PIXELAI_KEY_PASS") ?: "PixelAIBridge2026!"
            }
        }
    }

    buildTypes {
        val fixedSigning = signingConfigs.findByName("pixelFixed")
        debug {
            if (fixedSigning != null) {
                signingConfig = fixedSigning
            }
        }
        release {
            isMinifyEnabled = false
            if (fixedSigning != null) {
                signingConfig = fixedSigning
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:ai"))
    implementation(project(":core:shizuku"))
    implementation(project(":feature:screenshots"))
    implementation(project(":feature:nowplaying"))
    implementation(project(":feature:magiccue"))
    implementation(project(":feature:studio"))
    implementation(project(":feature:callnotes"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.okhttp)

    debugImplementation(libs.androidx.ui.tooling)
}
