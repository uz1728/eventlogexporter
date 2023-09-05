package com.uza.eventlogexporter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Display
import androidx.core.app.NotificationCompat
import android.hardware.display.DisplayManager
import android.view.WindowManager

class EventMonitoringService : Service() {
    companion object {
        val TAG: String = EventMonitoringService::class.java.simpleName
        const val CHANNEL_ID = "EventMonitoringServiceChannel"
        const val NOTIFICATION_ID = 1
    }

    private val deviceEventReceiver = DeviceEventReceiver()

    inner class DeviceEventReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context == null || intent == null) {
                Log.e(TAG, "Unable to receive events due to context and/or intent being null")
                return
            }

            val applicationContext = context.applicationContext
            val networkChangeListener = NetworkChangeListener.getInstance(applicationContext)
            networkChangeListener.startListening()

            val action = intent.action
            val displayState = getDisplayState(context)

            val logMessage = when (action) {
                Intent.ACTION_USER_PRESENT -> "User Unlocked Device"
                Intent.ACTION_SHUTDOWN -> "Device Shutting Down"
                Intent.ACTION_SCREEN_ON -> "Screen Interactive"
                Intent.ACTION_SCREEN_OFF -> "Screen Non-Interactive"
                else -> "Unknown Action: $action"
            }

            networkChangeListener.logState("${logMessage}: ${displayState}")
        }


        private fun getDisplayState(context: Context): String {
            val displays: Array<Display>?

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val displayManager =
                    context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                displays = displayManager.displays
            } else {
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                displays = arrayOf(windowManager.defaultDisplay)
            }

            if (!displays.isNullOrEmpty()) {
                val displayInfo = displays[0].state
                return when (displayInfo) {
                    Display.STATE_ON -> "Display On"
                    Display.STATE_OFF -> "Display Off"
                    Display.STATE_DOZE -> "Display Dozing"
                    Display.STATE_DOZE_SUSPEND -> "Display Dozing Suspended"
                    else -> "Unknown"
                }
            }
            return "Unknown"
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
            Log.e(TAG, "Server was unable to be properly started:", exception)
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // Service will be restarted if terminated by the system
    }

    private fun startForeground() {
        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
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
