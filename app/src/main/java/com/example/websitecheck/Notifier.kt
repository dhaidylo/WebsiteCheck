package com.example.websitecheck

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color

class Notifier(context: Context, private val baseIntent: PendingIntent) : INotifier {
    companion object {
        private const val APP_NAME = "Website Check"
        private const val CHANNEL_ID = "website_check"
        private lateinit var _manager: NotificationManager
        private var _notificationId = 1

        fun initialize(notificationManager: NotificationManager) {
            _manager = notificationManager
            createChannel();
        }

        private fun createChannel() {
            val channel = NotificationChannel(
                CHANNEL_ID,
                APP_NAME,
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
            _manager.createNotificationChannel(channel)
        }
    }

    private val builder: Notification.Builder = Notification.Builder(context, CHANNEL_ID)
        .setContentTitle(APP_NAME)
        .setContentIntent(baseIntent)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setShowWhen(true)

    override fun notify(message: String) {
        builder.setWhen(System.currentTimeMillis())
            .setContentIntent(baseIntent)
            .setContentText(message)
        _manager.notify(++_notificationId, builder.build())
    }

    override fun notify(message: String, intent: PendingIntent) {
        builder.setWhen(System.currentTimeMillis())
            .setContentIntent(intent)
            .setContentText(message)
        _manager.notify(++_notificationId, builder.build())
    }
}