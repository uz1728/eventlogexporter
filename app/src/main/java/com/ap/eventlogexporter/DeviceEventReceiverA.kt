package com.ap.eventlogexporter

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

private val TAG = DeviceEventReceiverA::class.java.simpleName

class DeviceEventReceiverA : ModifiedBroadcastReceiver() {

    private var networkChangeListener: NetworkChangeListener? = null

    override fun onReceive(context: Context?, intent: Intent?) {

        val sharedPreferences = context?.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val fileWriter = FileWriter(context!!)

        if (context != null && intent != null) {
            when (intent.action) {

                Intent.ACTION_PACKAGE_REPLACED -> {
                    // Clear SharedPreferences data
                    sharedPreferences?.edit()?.clear()?.apply()
                    Log.d("DeviceEventReceiver", "Package replaced event received")
                }

                Intent.ACTION_USER_PRESENT, Intent.ACTION_SHUTDOWN,
                Intent.ACTION_SCREEN_OFF, Intent.ACTION_SCREEN_ON, Intent.ACTION_BOOT_COMPLETED -> {
                    // Create a NetworkChangeListener instance if not already created
                    if (networkChangeListener == null) {
                        networkChangeListener = NetworkChangeListener(context)
                    }
                    when (intent.action) {

                        Intent.ACTION_BOOT_COMPLETED -> {
                            networkChangeListener?.startListening()
                            val message = "Device Booted"
                            Log.d("DeviceEventReceiver", message)
                            fileWriter.writeToFile(message)
                        }

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

        else {
            Log.e(javaClass.canonicalName, "Unable to detect device events")
        }
    }
    override fun getValidReceiverActions(): IntentFilter{
        return IntentFilter().apply {
            addAction(Intent.ACTION_BOOT_COMPLETED)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SHUTDOWN)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
    }
}
