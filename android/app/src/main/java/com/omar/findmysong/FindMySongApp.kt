package com.omar.findmysong

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber


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