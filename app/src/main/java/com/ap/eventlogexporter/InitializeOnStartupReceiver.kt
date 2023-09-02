package com.ap.eventlogexporter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.ap.eventlogexporter.utils.Utils
import java.lang.Exception

private val TAG = InitializeOnStartupReceiver::class.java.simpleName

class InitializeOnStartupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        if (context != null && intent != null) {
            if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                try {
                    val sharedPreferences =
                        context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    val fileWriter = FileWriter(context)

                    val enrollmentCompleted =
                        sharedPreferences?.getBoolean("enrollmentCompleted", false)

                    if (enrollmentCompleted == true) {
                        val networkChangeListener = NetworkChangeListener.getInstance(context)
                        networkChangeListener.startListening()
                        // startEventMonitoringWork(context)
                        // Log.i(TAG, "Started event monitoring worker on startup")
                        val message = "Device Boot Completed"
                        Log.i(TAG, message)
                        fileWriter.writeToFile(message)

                        val isServiceRunning =
                            Utils.isServiceRunning(EventMonitoringService::class.java, context)
                        // Start the service if it's not already running
                        if (!isServiceRunning) {
                            val serviceIntent = Intent(context, EventMonitoringService::class.java)

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                        }
                        else {
                            Log.i(TAG, "Event Monitoring Service Already Running")
                        }
                    }
                } catch (exception: Exception) {
                    Log.e(TAG, "Exception:", exception)
                }
            } else {
                Log.e(TAG,"Unable to receive events due to context and/or intent being null"
                )
            }
        }
    }
}
