package com.uza.eventlogexporter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.uza.eventlogexporter.utils.Utils
import com.uza.eventlogexporter.utils.Utils.startEventMonitoringService

class InitializeOnStartupReceiver : BroadcastReceiver() {

    companion object {
        val TAG: String = InitializeOnStartupReceiver::class.java.simpleName
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.e(TAG, "Unable to receive events due to context and/or intent being null")
            return
        }

        val applicationContext = context.applicationContext
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // Capture the boot time
                val serviceCreatedTime = SystemClock.elapsedRealtime()
                Log.d(TAG, "Time Since System Booted: $serviceCreatedTime")
                // Calculate the startup time
                try {
                    val sharedPreferences =
                        applicationContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

                    if (sharedPreferences?.getBoolean("enrollmentCompleted", false) == true) {
                        val networkChangeListener =
                            NetworkChangeListener.getInstance(applicationContext)
                        networkChangeListener.startListening()

                        val message = "Device Boot Completed"
                        networkChangeListener.logState((message))

                        if (!Utils.isServiceRunning(EventMonitoringService::class.java, context)) {
                            startEventMonitoringService(TAG, context)
                        } else {
                            Log.i(MainActivity.TAG, "EventMonitoringService Already Running")
                        }
                    }
                } catch (exception: Exception) {
                    Log.e(TAG, "Exception:", exception)
                }
            }
        }
    }
}
