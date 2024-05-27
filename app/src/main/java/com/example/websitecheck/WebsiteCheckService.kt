package com.example.websitecheck

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import okhttp3.OkHttpClient

class WebsiteCheckService: Service(){
    private val okHttpClient = OkHttpClient()
    private var notificationId = 2

    private val immoweltUrl = "https://www.immowelt.de/suche/hamburg/wohnungen/mieten?ama=55&ami=30&d=true&pma=600&r=10&sd=DESC&sf=TIMESTAMP&sp=1"
    private val immoweltSelector = ".SearchList-22b2e"
    private val immoweltChecker = WebsiteChecker(immoweltUrl, immoweltSelector, okHttpClient, "Immowelt")

    private val sagaUrl = "https://www.saga.hamburg/immobiliensuche?Kategorie=APARTMENT"
    private val sagaSelector = "#APARTMENT"
    private val sagaChecker = WebsiteChecker(sagaUrl, sagaSelector, okHttpClient, "Saga")

    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent != null) {
            val action = intent.action
            when (action) {
                Actions.START.name -> startService()
                Actions.STOP.name -> stopService()
                else -> log("This should never happen. No action in the received intent")
                }
        } else {
            log("with a null intent. It has been probably restarted by the system.")
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        log("The service has been created")
        val notification = createNotification()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        log("The service has been destroyed")
        Toast.makeText(this, "Service destroyed", Toast.LENGTH_SHORT).show()
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent = Intent(applicationContext, WebsiteCheckService::class.java).also {
            it.setPackage(packageName)
        }
        val restartServicePendingIntent: PendingIntent = PendingIntent.getService(this, 1, restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        applicationContext.getSystemService(Context.ALARM_SERVICE)
        val alarmService: AlarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent)
    }

    private fun startService() {
        if (isServiceStarted) return
        log("Starting the foreground service task")
        isServiceStarted = true
        setServiceState(this, ServiceState.STARTED)

        // we need this lock so our service gets not affected by Doze Mode
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WebsiteCheckService::lock").apply {
                    acquire()
                }
            }

        // we're starting a loop in a coroutine
        GlobalScope.launch(Dispatchers.IO) {
            immoweltChecker.initialize()
            sagaChecker.initialize()
            while (isServiceStarted) {
                checkWebsites()
                delay(1 * 10 * 1000)
            }
            log("End of the loop for the service")
        }
    }
    
    private fun stopService() {
        log("Stopping the foreground service")
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } catch (e: Exception) {
            log("Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
        setServiceState(this, ServiceState.STOPPED)
    }

    private suspend fun checkWebsites() = coroutineScope {
        launch { checkWebsite(sagaChecker) }
        launch { checkWebsite(immoweltChecker) }
    }

    private suspend fun checkWebsite(checker: WebsiteChecker) {
        if (checker.check()) {
            sendNotification("${checker.name} changed!")
        }
    }

    private fun sendNotification(message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "website_check_channel"
        val channel = NotificationChannel(
            channelId,
            "Website Check",
            NotificationManager.IMPORTANCE_DEFAULT
        ).let {
            it.description = "Website Check Service channel"
            it.enableLights(true)
            it.lightColor = Color.RED
            it.enableVibration(true)
            it.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            it.setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, Notification.AUDIO_ATTRIBUTES_DEFAULT)
            it
        }

        notificationManager.createNotificationChannel(channel)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Website Check")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .build()
        notificationManager.notify(notificationId, notification)
        notificationId++
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "website_check_service"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            notificationChannelId,
            "Website Check Service notifications channel",
            NotificationManager.IMPORTANCE_HIGH
        ).let {
            it.description = "Website Check Service channel"
            it.enableLights(true)
            it.lightColor = Color.RED
            it
        }
        notificationManager.createNotificationChannel(channel)

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        val builder: Notification.Builder = Notification.Builder(
            this,
            notificationChannelId
        )

        return builder
            .setContentTitle("Website Check Service")
            .setContentText("Website Check service working")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker("Ticker text")
            .build()
    }
}