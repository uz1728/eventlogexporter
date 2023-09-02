package com.ap.eventlogexporter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.ap.eventlogexporter.utils.Utils.isServiceRunning

class MainActivity : AppCompatActivity() {

    companion object {
        val TAG = MainActivity::class.java.simpleName
    }

    private val initializeOnStartupReceiver = InitializeOnStartupReceiver()

    inner class InitializeOnStartupReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null && intent != null && intent.action == Intent.ACTION_BOOT_COMPLETED) {
                try {
                    val sharedPreferences =
                        context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    val enrollmentCompleted =
                        sharedPreferences.getBoolean("enrollmentCompleted", false)

                    if (enrollmentCompleted) {
                        val fileWriter = FileWriter(context)
                        val networkChangeListener = NetworkChangeListener.getInstance(context)
                        networkChangeListener.startListening()

                        val message = "Device Boot Completed"
                        Log.i(TAG, message)
                        fileWriter.writeToFile(message)
                        networkChangeListener.logState()

                        if (!isServiceRunning(EventMonitoringService::class.java, context)) {
                            val serviceIntent = Intent(context, EventMonitoringService::class.java)

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val enrollmentCompleted = sharedPreferences.getBoolean("enrollmentCompleted", false)
        val receiverRegistered = sharedPreferences.getBoolean("receiverRegistered", false)

        if (!enrollmentCompleted) {
            navController.navigate(R.id.enrollmentFragment)
        } else {
            val intentFilter = IntentFilter().apply {
                addAction(Intent.ACTION_BOOT_COMPLETED)
                //addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED)
                addAction(Intent.ACTION_USER_PRESENT)
            }

            if (!receiverRegistered) {
                Log.i(TAG, "Trying to Register InitializeOnStartupReceiver")
                try {
                    registerReceiver(initializeOnStartupReceiver, intentFilter)
                    sharedPreferences.edit().putBoolean("receiverRegistered", true).apply()
                } catch (exception: Exception) {
                    Log.i(TAG, "Exception:", exception)
                }
            }

            if (!isServiceRunning(EventMonitoringService::class.java, this)) {
                val serviceIntent = Intent(this, EventMonitoringService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.i(TAG, "Trying to Start Event Monitoring Service as Foreground Service")
                    startForegroundService(serviceIntent)
                } else {
                    Log.i(TAG, "Trying to Start Event Monitoring Service as Regular Service")
                    startService(serviceIntent)
                }
            }

            val networkChangeListener = NetworkChangeListener.getInstance(applicationContext)
            networkChangeListener.startListening()
            navController.navigate(R.id.action_enrollmentFragment_to_uploadFragment)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(initializeOnStartupReceiver)
    }
}
