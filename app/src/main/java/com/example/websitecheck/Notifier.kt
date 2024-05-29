package com.example.websitecheck

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri

class Notifier(private val context: Context, name: String, url: String) {
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

    private val builder: Notification.Builder

    init {
        val openUrlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val intent: PendingIntent = PendingIntent.getActivity(context, ++notificationId, openUrlIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        builder = Notification.Builder(context, CHANNEL_ID)
            .setContentTitle("Website Check")
            .setContentText(name)
            .setContentIntent(intent)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setShowWhen(true)
    }

    fun notify(url: String? = null) {
        notificationId++
        builder.setWhen(System.currentTimeMillis())
        if(url != null) {
            val openUrlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            val intent: PendingIntent = PendingIntent.getActivity(context, notificationId, openUrlIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            builder.setContentIntent(intent)
        }
        manager.notify(notificationId, builder.build())
    }
}