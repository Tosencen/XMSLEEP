plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "org.streambox.app"
    compileSdk = 35
    
    defaultConfig {
        applicationId = "org.streambox.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
    }
    
    signingConfigs {
        create("release") {
            storeFile = file("${project.rootDir}/release.keystore")
            storePassword = "xmsleep2025"
            keyAlias = "xmsleep"
            keyPassword = "xmsleep2025"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    buildFeatures {
        compose = true
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }
}

dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    
    // AndroidX
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    
    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    
    // MaterialKolor - 动态主题色生成（和 Animeko 一样）
    implementation("com.materialkolor:material-kolor:2.0.2")
    
    // XMBOX 视频源识别模块
    implementation(project(":datasource:xmbox"))
    
           // Coil - 图片加载
           implementation("io.coil-kt:coil-compose:2.5.0")
           
           // Kotlin Serialization
           implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    
    // Lottie - 用于显示JSON动画（声音模块需要）
    implementation("com.airbnb.android:lottie:6.1.0")
    
    // ExoPlayer/Media3 - 用于无缝循环播放音频（声音模块需要）
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-common:1.2.1")
    
    // OkHttp - 用于网络请求和文件下载
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
}

