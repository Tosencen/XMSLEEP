package org.xmsleep.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Hilt Application 类
 * 用于初始化依赖注入容器
 */
@HiltAndroidApp
class XMSleepApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
    }
}
