package com.ap.eventlogexporter

import android.content.Context
import android.util.Log

class AppLogger private constructor(context: Context) : BaseFileWriter(context, "com_ap_eventlogexporter    ", "Timestamp,Tag,Message") {

    companion object {
        private var instance: AppLogger? = null

        fun getInstance(context: Context): AppLogger {
            return instance ?: synchronized(this) {
                instance ?: AppLogger(context.applicationContext).also { instance = it }
            }
        }
    }

    fun log(logLevel: Char, tag: String, message: String) {
        val logMessage = "$tag,$message\n"
        when (logLevel.lowercaseChar()) {
            'i' -> {
                Log.i(tag, logMessage) // Info level log to Logcat
            }
            'd' -> {
                Log.d(tag, logMessage) // Debug level log to Logcat
            }
            'e' -> {
                Log.e(tag, logMessage) // Error level log to Logcat
            }
            else -> {
                Log.d(tag, logMessage) // Default to debug level if an invalid log level is provided
            }
        }
        writeToFileWithTimestamp(logMessage)
    }
}
