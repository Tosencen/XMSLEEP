package org.xmsleep.app

import android.app.Application

class XMSleepApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        org.xmsleep.app.analytics.AnalyticsLogger.initialize(this)
        org.xmsleep.app.weather.WeatherSoundMapper.initialize(this)
        org.xmsleep.app.diary.ListeningDiaryManager.init(this)
        org.xmsleep.app.preferences.PreferencesManager.initialize(this)
    }
}
