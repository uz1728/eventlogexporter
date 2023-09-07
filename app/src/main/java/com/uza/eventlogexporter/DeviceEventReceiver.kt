package com.uza.eventlogexporter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.WindowManager

class DeviceEventReceiver private constructor() : BroadcastReceiver() {
    companion object {
        private var instance: DeviceEventReceiver? = null

        @JvmStatic
        fun getInstance(): DeviceEventReceiver {
            return instance ?: synchronized(this) {
                instance ?: DeviceEventReceiver().also {
                    instance = it
                }
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.e(EventMonitoringService.TAG, "Unable to receive events due to context and/or intent being null")
            return
        }

        val applicationContext = context.applicationContext
        val networkChangeListener = NetworkChangeListener.getInstance(applicationContext)
        networkChangeListener.startListening(applicationContext)

        val action = intent.action
        val displayState = getDisplayState(applicationContext)

        val logMessage = when (action) {
            Intent.ACTION_USER_PRESENT -> "User Unlocked Device"
            Intent.ACTION_SHUTDOWN -> "Device Shutting Down"
            Intent.ACTION_SCREEN_ON -> "Screen Interactive"
            Intent.ACTION_SCREEN_OFF -> "Screen Non-Interactive"
            else -> "Unknown Action: $action"
        }

        networkChangeListener.logState("$logMessage: $displayState")
    }

    private fun getDisplayState(context: Context): String {
        val displays: Array<Display>? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val displayManager =
                    context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                displayManager.displays
            } else {
                val windowManager =
                    context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                arrayOf(windowManager.defaultDisplay)
            }

        if (!displays.isNullOrEmpty()) {
            val displayInfo = displays[0].state
            return when (displayInfo) {
                Display.STATE_ON -> "Display On"
                Display.STATE_OFF -> "Display Off"
                Display.STATE_DOZE -> "Display Dozing"
                Display.STATE_DOZE_SUSPEND -> "Display Dozing Suspended"
                else -> "Unknown"
            }
        }
        return "Unknown"
    }
}