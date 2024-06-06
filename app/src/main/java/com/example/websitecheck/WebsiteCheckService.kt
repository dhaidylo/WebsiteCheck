package com.example.websitecheck

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.widget.Toast
import kotlinx.coroutines.*

class WebsiteCheckService: Service(){
    private lateinit var _wakeLock: PowerManager.WakeLock

    private val _websiteCheckers: List<WebsiteChecker> = listOf(
        ImmoweltChecker(),
        SagaChecker()
    )

    private var _isServiceStarted = false

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
        val restartServicePendingIntent: PendingIntent = PendingIntent.getService(
            this, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmService: AlarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent)
    }

    private fun startService() {
        if (_isServiceStarted) return
        log("Starting the foreground service task")
        _isServiceStarted = true
        setServiceState(this, ServiceState.STARTED)

        // we need this lock so our service gets not affected by Doze Mode
        _wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WebsiteCheckService::lock").apply {
                acquire()
            }
        }

        for (checker in _websiteCheckers) {
            checker.initialize(this)
        }

        GlobalScope.launch(Dispatchers.IO) {
            for (checker in _websiteCheckers) {
                launch {
                    while (_isServiceStarted) {
                        checker.run()
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
            if (_wakeLock.isHeld) {
                _wakeLock.release()
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } catch (e: Exception) {
            log("Service stopped without being started: ${e.message}")
        }
        _isServiceStarted = false
        setServiceState(this, ServiceState.STOPPED)
    }

    private fun createNotification(notificationManager: NotificationManager): Notification {
        val notificationChannelId = "website_check_service"
        val channel = NotificationChannel(
            notificationChannelId,
            "Website Check Service notifications channel",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Website Check Service channel"
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