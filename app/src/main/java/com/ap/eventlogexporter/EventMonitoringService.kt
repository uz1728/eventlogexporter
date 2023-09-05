package com.ap.eventlogexporter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class EventMonitoringService : Service() {
    companion object {
        val TAG: String = EventMonitoringService::class.java.simpleName
        const val CHANNEL_ID = "EventMonitoringServiceChannel"
    }

    private val deviceEventReceiver = DeviceEventReceiver()

    inner class DeviceEventReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null && intent != null) {
                val applicationContext = context.applicationContext
                val networkChangeListener = NetworkChangeListener.getInstance(applicationContext)
                networkChangeListener.startListening()

                when (intent.action) {
                    Intent.ACTION_USER_PRESENT -> networkChangeListener.logState("Event: User Unlocked Device")
                    Intent.ACTION_SHUTDOWN -> networkChangeListener.logState("Device Shutting Down")
                    Intent.ACTION_SCREEN_ON -> networkChangeListener.logState("Screen Turned On")
                    Intent.ACTION_SCREEN_OFF -> networkChangeListener.logState("Screen Turned Off")
                }
            } else {
                Log.e(TAG, "Unable to receive events due to context and/or intent being null")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForeground()
                Log.d(TAG, "EventMonitoringService Started as Foreground Service")
            } else {
                Log.d(TAG, "EventMonitoringService Started as Regular Service")
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
            Log.e(TAG, "Exception:", exception)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // Service will be restarted if terminated by the system
    }

    private fun startForeground() {
        val notification = createNotification()
        startForeground(1, notification)
    }

    private fun createNotification(): Notification {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val smallIcon = R.drawable.ic_notification_icon // Replace with your small icon resource

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Event Log Exporter")
            .setContentText("")
            .setSmallIcon(smallIcon                                                                                                                                                                                                                                         )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        return builder.build()
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

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the broadcast receiver if the service is destroyed
        unregisterReceiver(deviceEventReceiver)
    }
}
