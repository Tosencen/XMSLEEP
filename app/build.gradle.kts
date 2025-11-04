plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "org.streambox.app"
    compileSdk = 35
    
    defaultConfig {
        applicationId = "org.streambox.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
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
    
    // AndroidX
    implementation("androidx.core:core-ktx:1.15.0")
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
    
    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
}

