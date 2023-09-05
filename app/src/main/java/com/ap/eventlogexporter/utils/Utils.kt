package com.uza.eventlogexporter.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.uza.eventlogexporter.EventMonitoringService

object Utils {
    fun isServiceRunning(serviceClass: Class<*>, context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
    fun startEventMonitoringService(tag: String, context: Context) {
        val serviceIntent = Intent(context, EventMonitoringService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Start as a foreground service
            Log.d(tag, "Trying to Start EventMonitoringService as a Foreground Service")
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            // Start as a regular service (for older Android versions)
            Log.d(tag, "Trying to Start EventMonitoringService as a Regular Service")
            context.startService(serviceIntent)
        }
    }
}