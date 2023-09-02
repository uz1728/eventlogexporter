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
        val TAG = EventMonitoringService::class.java.simpleName
        const val CHANNEL_ID = "EventMonitoringServiceChannel"
    }

    private var broadcastReceiver: BroadcastReceiver? = null

    private val deviceEventReceiver = DeviceEventReceiver()

    inner class DeviceEventReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null && intent != null) {
                val applicationContext = context.applicationContext
                val fileWriter = FileWriter(applicationContext)
                val sharedPreferences =
                    applicationContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                val networkChangeListener = NetworkChangeListener.getInstance(applicationContext)
                networkChangeListener.startListening()

                when (intent.action) {
//                    Intent.ACTION_PACKAGE_REPLACED -> {
//                        sharedPreferences.edit().clear().apply()
//                        logEvent(
//                            fileWriter,
//                            "Event: Package replacement event received and sharedPreferences were cleared"
//                        )
//                    }

                    Intent.ACTION_SHUTDOWN -> logEvent(fileWriter, "Event: Device Shutting Down")
                    Intent.ACTION_SCREEN_ON -> logEvent(fileWriter, "Event: Screen Turned On")
                    Intent.ACTION_SCREEN_OFF -> logEvent(fileWriter, "Event: Screen Turned Off")
                }
                networkChangeListener.logState()
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
                //addAction(Intent.ACTION_PACKAGE_REPLACED)
                addAction(Intent.ACTION_SHUTDOWN)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }

            broadcastReceiver = deviceEventReceiver
            registerReceiver(broadcastReceiver, intentFilter)
            Log.d(TAG, "DeviceEventReceiver Initialized")

        } catch (exception: Exception) {
            Log.e(TAG, "Exception:", exception)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle the intent if needed
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

        val smallIcon = R.drawable.myicon // Replace with your small icon resource

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Event Log Exporter")
            .setContentText("Service is running.")
            .setSmallIcon(smallIcon)
            .setContentIntent(pendingIntent) // Use FLAG_UPDATE_CURRENT here
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

    private fun logEvent(fileWriter: FileWriter, message: String) {
        Log.i(TAG, message)
        fileWriter.writeToFile(message)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the broadcast receiver if the service is destroyed
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver)
        }
    }
}
