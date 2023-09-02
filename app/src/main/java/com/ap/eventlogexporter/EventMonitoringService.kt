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
import java.lang.Exception

private val TAG = EventMonitoringService::class.java.simpleName

class EventMonitoringService : Service() {

    private val channelId = "EventMonitoringServiceChannel"
    private var broadcastReceiver: BroadcastReceiver? = null
    inner class DeviceEventReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val TAG = EventMonitoringService::class.java.simpleName

            if (context != null && intent != null) {

                val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                val fileWriter = FileWriter(context)
                val networkChangeListener = NetworkChangeListener.getInstance(context)

                when (intent.action) {

                    Intent.ACTION_PACKAGE_REPLACED -> {
                        // Clear SharedPreferences data
                        sharedPreferences?.edit()?.clear()?.apply()
                        Log.d(TAG, "Package replacement event received and sharedPreferences were cleared")
                    }

                    Intent.ACTION_USER_PRESENT, Intent.ACTION_SHUTDOWN,
                    Intent.ACTION_SCREEN_OFF, Intent.ACTION_SCREEN_ON -> {
                        // Create a NetworkChangeListener instance if not already created
                        when (intent.action) {

                            Intent.ACTION_USER_PRESENT -> {
                                networkChangeListener.startListening()
                                val message = "User Unlocked Device"
                                Log.i(TAG, message)
                                fileWriter.writeToFile(message)
                            }

                            Intent.ACTION_SCREEN_ON -> {
                                networkChangeListener.startListening()
                                val message = "Screen Turned On"
                                Log.i(TAG, message)
                                fileWriter.writeToFile(message)
                            }

                            Intent.ACTION_SHUTDOWN -> {
                                networkChangeListener.stopListening()
                                val message = "Device Shutting Down"
                                Log.i(TAG, message)
                                fileWriter.writeToFile(message)
                            }

                            Intent.ACTION_SCREEN_OFF -> {
                                networkChangeListener.stopListening()
                                val message = "Screen Turned Off"
                                Log.i(TAG, message)
                                fileWriter.writeToFile(message)
                            }
                        }
                    }
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
            val intentFilter = IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SHUTDOWN)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            //create and register receiver
            broadcastReceiver = DeviceEventReceiver()
            registerReceiver(broadcastReceiver, intentFilter)
            Log.d(javaClass.name, "DeviceEventReceiver Initialized")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForeground()
                Log.d(javaClass.name, "Event Monitoring Service Started as Foreground Service")
            }
            else {
                Log.d(javaClass.name, "Event Monitoring Service Started as Regular Service")
            }
        } catch (exception: Exception) {
            Log.e(javaClass.name, "Exception:", exception)
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle the intent if needed
        return START_STICKY // Service will be restarted if it gets terminated by the system
    }

    private fun startForeground() {
        val notification = createNotification()

        startForeground(1, notification)
    }
// ...
    private fun createNotification(): Notification {
        createNotificationChannel()

        // Create an intent to open the app's main activity
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // Set your small icon here
        val smallIcon = R.drawable.myicon // Replace with your small icon resource

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("My Service")
            .setContentText("Service is running.")
            .setSmallIcon(smallIcon) // Set the small icon here
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "My Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
