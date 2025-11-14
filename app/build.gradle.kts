plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "org.xmsleep.app"
    compileSdk = 35
    
    defaultConfig {
        applicationId = "org.xmsleep.app"
        minSdk = 26
        targetSdk = 35
		versionCode = 25
		versionName = "2.0.5"
        
        // 只保留 arm64-v8a 架构以减小 APK 体积（现代设备都支持）
        ndk {
            abiFilters += listOf("arm64-v8a")
            debugSymbolLevel = "NONE"  // 禁用debug符号，避免strip警告
        }
        
        // 从 gradle.properties 读取 GitHub Token（如果存在）
        val githubToken = project.findProperty("GITHUB_TOKEN") as String? ?: ""
        buildConfigField("String", "GITHUB_TOKEN", if (githubToken.isNotBlank()) "\"$githubToken\"" else "null")
    }
    
    signingConfigs {
        create("release") {
            val keystoreFile = project.findProperty("RELEASE_STORE_FILE") as String? ?: "release.keystore"
            val keystorePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String? ?: ""
            val keyAliasName = project.findProperty("RELEASE_KEY_ALIAS") as String? ?: "xmsleep"
            val keyAliasPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String? ?: ""
            
            storeFile = file("${project.rootDir}/$keystoreFile")
            storePassword = keystorePassword
            keyAlias = keyAliasName
            keyPassword = keyAliasPassword
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false  // 禁用代码混淆
            isShrinkResources = false  // 禁用资源压缩
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            // 禁用debug版本的strip操作，避免警告
            isDebuggable = true
            ndk {
                debugSymbolLevel = "NONE"
            }
        }
    }
    
        
    buildFeatures {
        compose = true
        buildConfig = true
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
    
    lint {
        disable.add("NullSafeMutableLiveData")
    }
    
    packaging {
        // 忽略无法strip的库文件警告
        jniLibs {
            keepDebugSymbols.add("**/*.so")
            useLegacyPackaging = true
        }
    }
    
    dependenciesInfo {
        // Disables dependency metadata when building APKs (for IzzyOnDroid/F-Droid)
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles (for Google Play)
        includeInBundle = false
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
    // Pull-to-refresh (Accompanist)
    implementation("com.google.accompanist:accompanist-swiperefresh:0.34.0")
    
    // AndroidX
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.9.5")
    
    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    
    // MaterialKolor - 动态主题色生成
    implementation("com.materialkolor:material-kolor:2.0.2")
    
    // Coil - 图片加载
    implementation("io.coil-kt:coil-compose:2.6.0")
    
    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    
    // Lottie - 用于显示JSON动画（声音模块需要）
    implementation("com.airbnb.android:lottie:6.3.0")
    
    // ExoPlayer/Media3 - 用于无缝循环播放音频（声音模块需要）
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-ui:1.8.0")
    implementation("androidx.media3:media3-common:1.8.0")
    
    // OkHttp - 用于网络请求和文件下载
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Gson - 用于JSON解析
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
}

