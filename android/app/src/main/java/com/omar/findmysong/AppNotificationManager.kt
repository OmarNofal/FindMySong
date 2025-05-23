package com.omar.findmysong

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import coil3.Bitmap
import com.omar.findmysong.model.SongInfo

class AppNotificationManager private constructor(context: Context) {


    private val appContext = context.applicationContext

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun notifySongIdentifiedOffline(song: SongInfo, albumArt: Bitmap?) {
        createOfflineIdentificationChannel()
        val notificationBuilder = NotificationCompat.Builder(appContext, "song_offline_detection")
            .setSmallIcon(R.drawable.ic_radar_24)
            .setContentTitle(appContext.getString(R.string.song_identified))
            .setContentText("${song.title} by ${song.artist}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (albumArt != null)
            notificationBuilder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(albumArt)
                    .bigLargeIcon(null as Bitmap?)
            ).setLargeIcon(albumArt)

        notificationManager.notify(1, notificationBuilder.build())
    }

    fun notifySongNotIdentifiedOffline() {
        createOfflineIdentificationChannel()
        val notificationBuilder = NotificationCompat.Builder(appContext, "song_offline_detection")
            .setSmallIcon(R.drawable.ic_radar_24)
            .setContentTitle(appContext.getString(R.string.song_not_identified))
            .setContentText(appContext.getString(R.string.song_not_found_desc))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(2, notificationBuilder.build())
    }

    private fun createOfflineIdentificationChannel() =
        createNotificationChannel(
            "song_offline_detection",
            "Song Offline Detection",
            "Get notified when a song you previously recorded gets identified when you are back online"
        )

    private fun createNotificationChannel(id: String, title: String, desc: String) {
        val channel = NotificationChannel(
            id,
            title,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = desc
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        @Volatile
        private lateinit var instance: AppNotificationManager

        fun init(context: Context) {
            instance = AppNotificationManager(context.applicationContext)
        }

        fun get(): AppNotificationManager = instance
    }
}