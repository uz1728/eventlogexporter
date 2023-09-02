package com.ap.eventlogexporter

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TAG = FileWriter::class.java.simpleName

class FileWriter(private val context: Context) {

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("MM/dd/yyyy HH:mm:ss:SSS z", Locale.getDefault())
        val date = Date(timestamp)
        return sdf.format(date)
    }

    fun writeToFile(message: String): String {
        val timestamp = System.currentTimeMillis()
        val formattedTimestamp = formatTimestamp(timestamp)

        val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        val enrolledId = sharedPreferences.getString("enrolledId", null)

        val file = File(context.filesDir, "${enrolledId}_event_log.txt")
        val fullMessage = "${enrolledId} | ${formattedTimestamp} | ${message}\n"
        file.appendText(fullMessage)

        // Update the number of lines in SharedPreferences
        val lineCount = file.readLines().size
        sharedPreferences.edit().putInt("numberOfLines", lineCount).apply()

        return fullMessage
    }
}
