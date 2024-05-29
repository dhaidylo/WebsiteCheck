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
import okhttp3.OkHttpClient

class WebsiteCheckService: Service(){
    private lateinit var wakeLock: PowerManager.WakeLock
    private val okHttpClient = OkHttpClient()
    private var notificationId = 1

    private val websiteCheckers: List<WebsiteChecker> = listOf(
        WebsiteChecker(
            "https://www.immowelt.de/suche/hamburg/wohnungen/mieten?ama=55&ami=30&d=true&pma=600&r=10&sd=DESC&sf=TIMESTAMP&sp=1",
            ".SearchList-22b2e",
            "Immowelt",
            okHttpClient
        ),
        SagaChecker(okHttpClient)
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
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        Notifier.initialize(notificationManager)
        log("The service has been created")
        val notification = createNotification(notificationManager)
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

        websiteCheckers.forEach {
            it.initialize(this)
        }
        // we're starting a loop in a coroutine
        GlobalScope.launch(Dispatchers.IO) {
            websiteCheckers.forEach { checker ->
                launch {
                    while (isServiceStarted) {
                        checker.check()
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

    private fun createNotification(notificationManager: NotificationManager): Notification {
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