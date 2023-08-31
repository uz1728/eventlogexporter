package com.ap.eventlogexporter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.navigation.fragment.NavHostFragment

class MainActivity : AppCompatActivity() {

    private val deviceEventReceiver = DeviceEventReceiver()
    private var isReceiverRegistered = false // Add this flag

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)

        // Initialize NavController using NavHostFragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val enrollmentCompleted = sharedPreferences.getBoolean("enrollmentCompleted", false)

        if (!enrollmentCompleted) {
            navController.navigate(R.id.enrollmentFragment)
        } else {
            val intentFilter = IntentFilter().apply {
                addAction(Intent.ACTION_BOOT_COMPLETED)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SHUTDOWN)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            registerReceiver(deviceEventReceiver, intentFilter)
            isReceiverRegistered = true // Set the flag to true
            navController.navigate(R.id.action_enrollmentFragment_to_uploadFragment)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister DeviceEventReceiver if it's registered to avoid memory leaks
        if (isReceiverRegistered) {
            unregisterReceiver(deviceEventReceiver)
            isReceiverRegistered = false // Reset the flag
        }
    }
}
