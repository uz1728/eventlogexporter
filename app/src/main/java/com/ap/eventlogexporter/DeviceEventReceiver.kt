package com.ap.eventlogexporter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class DeviceEventReceiver : BroadcastReceiver() {

    private var networkChangeListener: NetworkChangeListener? = null

    override fun onReceive(context: Context?, intent: Intent?) {

        val fileWriter = FileWriter(context!!)

        when (intent?.action) {
            Intent.ACTION_PACKAGE_REPLACED -> {
                // Clear SharedPreferences data
                val sharedPreferences =
                    context?.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                sharedPreferences?.edit()?.clear()?.apply()
                Log.d("DeviceEventReceiver", "Package replaced event received")
            }
            Intent.ACTION_USER_PRESENT, Intent.ACTION_SHUTDOWN,
            Intent.ACTION_SCREEN_OFF, Intent.ACTION_SCREEN_ON -> {
                // Create a NetworkChangeListener instance if not already created
                if (networkChangeListener == null) {
                    networkChangeListener = NetworkChangeListener(context!!)
                }
                when (intent.action) {
                    Intent.ACTION_USER_PRESENT -> {
                        networkChangeListener?.startListening()
                        val message = "User Unlocked Device"
                        Log.d("DeviceEventReceiver", message)
                        fileWriter.writeToFile(message)
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        networkChangeListener?.startListening()
                        val message = "Screen Turned On"
                        Log.d("DeviceEventReceiver", message)
                        fileWriter.writeToFile(message)
                    }
                    Intent.ACTION_SHUTDOWN -> {
                        networkChangeListener?.stopListening()
                        val message = "Device Shutting Down"
                        Log.d("DeviceEventReceiver", message)
                        fileWriter.writeToFile(message)
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        networkChangeListener?.stopListening()
                        val message = "Screen Turned Off"
                        Log.d("DeviceEventReceiver", message)
                        fileWriter.writeToFile(message)
                    }
                }
            }
        }
    }
}
