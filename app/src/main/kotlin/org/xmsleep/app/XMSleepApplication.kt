package org.xmsleep.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class XMSleepApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        org.xmsleep.app.diary.ListeningDiaryManager.init(this)
    }
}
