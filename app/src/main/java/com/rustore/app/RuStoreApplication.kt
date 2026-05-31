package com.rustore.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class RuStoreApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(applicationContext)
        val channel = NotificationChannel(
            "download_channel",
            "Загрузка приложений",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
