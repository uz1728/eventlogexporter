package com.ap.eventlogexporter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.navigation.fragment.NavHostFragment
import com.ap.eventlogexporter.utils.Utils.isServiceRunning
import java.lang.Exception

private val TAG = MainActivity::class.java.simpleName

class MainActivity : AppCompatActivity() {

    private var isServiceStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)

        // Initialize NavController using NavHostFragment
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val initializeOnStartupReceiver = InitializeOnStartupReceiver()

        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val enrollmentCompleted = sharedPreferences.getBoolean("enrollmentCompleted", false)
        val receiverRegistered = sharedPreferences.getBoolean("receiverRegistered", false)

        /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = packageName
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        } */

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
                }
                catch (exception: Exception) {
                    Log.i(TAG, "Exception:", exception)
                }
            }

            val isServiceRunning = isServiceRunning(EventMonitoringService::class.java, this)
            // Start the service if it's not already running
            if (!isServiceRunning) {
                val serviceIntent = Intent(this, EventMonitoringService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.i(TAG, "Trying to Start Event Monitoring Service as Foreground Service")
                    startForegroundService(serviceIntent)
                }
                else {
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
        // Unregister DeviceEventReceiver if it's registered to avoid memory leaks
        //if (areReceiversRegistered) {
        // unregisterReceiver(deviceEventReceiver)
        //areReceiversRegistered = false // Reset the flag
    }
}
