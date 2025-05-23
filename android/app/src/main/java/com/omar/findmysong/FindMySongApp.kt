package com.omar.findmysong

import android.app.Application
import timber.log.Timber
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class FindMySongApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppNotificationManager.init(this.applicationContext)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

}