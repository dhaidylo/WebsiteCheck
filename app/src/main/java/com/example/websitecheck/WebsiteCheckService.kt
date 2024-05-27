package com.example.websitecheck

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.widget.Toast
import kotlinx.coroutines.*

class WebsiteCheckService: Service(){
    private lateinit var notificationManager: NotificationManager
    private lateinit var wakeLock: PowerManager.WakeLock

    private var notificationId = 1

    private val websiteCheckers: List<WebsiteChecker> = listOf(
        WebsiteChecker(
            "https://www.immowelt.de/suche/hamburg/wohnungen/mieten?ama=55&ami=30&d=true&pma=600&r=10&sd=DESC&sf=TIMESTAMP&sp=1",
            ".SearchList-22b2e",
            "Immowelt"
        ),
        WebsiteChecker(
            "https://www.saga.hamburg/immobiliensuche?Kategorie=APARTMENT",
            "#APARTMENT",
            "Saga"
        )
    )

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
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        log("The service has been created")
        val notification = createNotification()
        startForeground(notificationId++, notification)
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
            websiteCheckers.forEach { checker ->
                launch {
                    checker.initialize()
                    while (isServiceStarted) {
                        checkWebsite(checker)
                        delay(1 * 10 * 1000)
                    }
                }
            }
            log("End of the loop for the service")
        }
    }

    private fun stopService() {
        log("Stopping the foreground service")
        try {
            wakeLock.let {
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

    private fun checkWebsite(checker: WebsiteChecker) {
        if (checker.check()) {
            val openUrlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(checker.url))
            val pendingIntent = PendingIntent.getActivity(this, 0, openUrlIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            sendNotification("${checker.name} changed!", pendingIntent)
        }
    }

    private fun createNotificationChannel(){
        val channel = NotificationChannel(
            "website_check",
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
        notificationManager.createNotificationChannel(channel)
    }

    private fun sendNotification(message: String, pendingIntent: PendingIntent) {
        val notification = Notification.Builder(this, "website_check")
            .setContentTitle("Website Check")
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .build()
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "website_check_service"
        val channel = NotificationChannel(
            notificationChannelId,
            "Website Check Service notifications channel",
            NotificationManager.IMPORTANCE_HIGH
        ).let {
            it.description = "Website Check Service channel"
            it
        }
        notificationManager.createNotificationChannel(channel)

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        return Notification.Builder(this, notificationChannelId)
            .setContentTitle("Website Check Service")
            .setContentText("Website Check service working")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }
}