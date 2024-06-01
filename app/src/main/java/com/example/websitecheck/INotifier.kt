package com.example.websitecheck

import android.app.PendingIntent

interface INotifier {
    fun notify(message: String)
    fun notify(message: String, intent: PendingIntent)
}