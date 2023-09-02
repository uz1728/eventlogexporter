package com.ap.eventlogexporter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.ap.eventlogexporter.utils.Utils

class InitializeOnStartupReceiver : BroadcastReceiver() {

    companion object {
        val TAG = InitializeOnStartupReceiver::class.java.simpleName
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.e(TAG, "Unable to receive events due to context and/or intent being null")
            return
        }

        val applicationContext = context.applicationContext
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                try {
                    val sharedPreferences =
                        applicationContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

                    if (sharedPreferences?.getBoolean("enrollmentCompleted", false) == true) {
                        val fileWriter = FileWriter(applicationContext)
                        val networkChangeListener =
                            NetworkChangeListener.getInstance(applicationContext)
                        networkChangeListener.startListening()

                        val message = "Device Boot Completed"
                        Log.i(TAG, message)
                        fileWriter.writeToFile(message)

                        if (!Utils.isServiceRunning(
                                EventMonitoringService::class.java,
                                applicationContext
                            )
                        ) {
                            val serviceIntent =
                                Intent(applicationContext, EventMonitoringService::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                applicationContext.startForegroundService(serviceIntent)
                            } else {
                                applicationContext.startService(serviceIntent)
                            }
                        } else {
                            Log.i(TAG, "Event Monitoring Service Already Running")
                        }
                    }
                } catch (exception: Exception) {
                    Log.e(TAG, "Exception:", exception)
                }
            }
        }
    }
}
