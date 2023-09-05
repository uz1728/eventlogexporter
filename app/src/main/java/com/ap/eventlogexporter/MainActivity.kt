package com.ap.eventlogexporter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.ap.eventlogexporter.utils.Utils.isServiceRunning

class MainActivity : AppCompatActivity() {

    companion object {
        val TAG: String = MainActivity::class.java.simpleName
    }

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var eventLogWriter: EventLogWriter  // Declare a property for FileWriter

    private val enrollmentCompleted by lazy {
        sharedPreferences.getBoolean("enrollmentCompleted", false)
    }

    private val receiverRegistered by lazy {
        sharedPreferences.getBoolean("receiverRegistered", false)
    }

    private val networkChangeListener by lazy {
        NetworkChangeListener.getInstance(applicationContext)
    }

    private val initializeOnStartupReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null && intent != null && intent.action == Intent.ACTION_BOOT_COMPLETED) {
                try {
                    if (enrollmentCompleted) {
                        networkChangeListener.startListening()
                        networkChangeListener.logState("Device Boot Completed")

                        if (!isServiceRunning(EventMonitoringService::class.java, context)) {
                            startMonitoringService(context)
                        } else {
                            Log.i(TAG, "EventMonitoringService Already Running")
                        }
                    }
                } catch (exception: Exception) {
                    Log.e(TAG, "Exception:", exception)
                }
            }
        }
    }

    private fun startMonitoringService(context: Context) {
        val serviceIntent = Intent(context, EventMonitoringService::class.java)
        Log.i(TAG, "Trying to Start EventMonitoringService as Regular Service")
        context.startService(serviceIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.i(TAG, "Trying to Upgrade EventMonitoringService to Foreground Service")
            context.startForegroundService(serviceIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity Instance Created")
        setContentView(R.layout.main_layout)

        eventLogWriter = EventLogWriter.getInstance(applicationContext)  // Initialize the FileWriter instance here

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        if (!enrollmentCompleted) {
            navController.navigate(R.id.enrollmentFragment)
        } else {
            val intentFilter = IntentFilter().apply {
                addAction(Intent.ACTION_BOOT_COMPLETED)
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

            // Check if the current destination is the exportFragment
            val currentDestinationId = navController.currentDestination?.id
            // Navigate to exportFragment only if not already there
            if (!isServiceRunning(EventMonitoringService::class.java, this)) {
                startMonitoringService(this)
            }
            networkChangeListener.startListening()

            if (currentDestinationId != R.id.exportFragment) {
                navController.navigate(R.id.action_enrollmentFragment_to_exportFragment)
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity Instance Destroyed")
    }
}
