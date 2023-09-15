package com.uza.eventlogexporter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import android.os.Binder

class EventMonitoringService : Service() {
    companion object {
        val TAG: String = EventMonitoringService::class.java.simpleName
        const val CHANNEL_ID = "EventMonitoringServiceChannel"
        const val NOTIFICATION_ID = 1
    }

    private val deviceEventReceiver = DeviceEventReceiver.getInstance()

    inner class LocalBinder : Binder() {
        fun getService(): EventMonitoringService {
            // Return this instance of EventMonitoringService so clients can call public methods
            return this@EventMonitoringService
        }
    }

    override fun onCreate() {
        super.onCreate()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForeground()
                Log.d(TAG, "EventMonitoringService Started as a Foreground Service")

            } else {
                // Log the startup time
                Log.d(TAG, "EventMonitoringService Started as a Regular Service")
            }

            val intentFilter = IntentFilter().apply {
                addAction(Intent.ACTION_SHUTDOWN)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }

            registerReceiver(deviceEventReceiver, intentFilter)
            Log.d(TAG, "DeviceEventReceiver Initialized")

        } catch (exception: Exception) {
            Log.e(TAG, "Service was unable to be properly started:", exception)
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "Service was Bound")
        return LocalBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // Service will be restarted if terminated by the system
    }

    private fun startForeground() {
        val notification = createNotification("Service is Running")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        val networkChangeListener =
            NetworkChangeListener.getInstance(applicationContext)
        networkChangeListener.startListening(applicationContext)
    }

    private fun createNotification(text: String): Notification {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val smallIcon = R.drawable.ic_notification_icon // Replace with your small icon resource

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Event Log Exporter")
                .setContentText(text)
                .setOnlyAlertOnce(true)
                .setSmallIcon(smallIcon)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

        } else {
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Event Log Exporter")
                .setContentText(text)
                .setOnlyAlertOnce(true)
                .setSmallIcon(smallIcon)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "EventMonitoringService Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Call this function to update the notification content
    fun updateNotificationContent(newContentText: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Check if the notification channel exists (for Android O and later)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(CHANNEL_ID)

            // If the channel doesn't exist, recreate it
            if (channel == null) {
                createNotificationChannel()
            }
        }

        // Create the updated notification within the same channel
        val updatedNotification = createNotification(newContentText)

        notificationManager.notify(NOTIFICATION_ID, updatedNotification)
        //Log.i(TAG, "Notification updated with: $newContentText")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the broadcast receiver if the service is destroyed
        unregisterReceiver(deviceEventReceiver)
    }
}
