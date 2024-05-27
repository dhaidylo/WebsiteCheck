package com.example.websitecheck

import android.app.Notification
import android.app.Notification.Builder
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.graphics.Color

class Notifier (private val notificationBuilder: Builder){
    companion object {
        const val CHANNEL_ID = "website_check"
        private lateinit var manager: NotificationManager
        private var notificationId = 1

        fun initialize(notificationManager: NotificationManager) {
            manager = notificationManager
            createChannel();
        }

        private fun createChannel() {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Website Check",
                NotificationManager.IMPORTANCE_HIGH
            ).let {
                it.description = "Website Check Channel"
                it.enableLights(true)
                it.lightColor = Color.RED
                it.enableVibration(true)
                it.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                it.setSound(
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    Notification.AUDIO_ATTRIBUTES_DEFAULT
                )
                it
            }
            manager.createNotificationChannel(channel)
        }
    }



    fun notify(intent: PendingIntent? = null) {
        if(intent != null)
            notificationBuilder.setContentIntent(intent)
        manager.notify(++notificationId, notificationBuilder.build())
    }
}