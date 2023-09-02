package com.ap.eventlogexporter

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileWriter(private val context: Context) {

    companion object {
        val TAG = FileWriter::class.java.simpleName
        const val DATE_FORMAT = "MM/dd/yyyy HH:mm:ss:SSS z"
    }

    private val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        val date = Date(timestamp)
        return sdf.format(date)
    }

    fun writeToFile(message: String): String {
        val timestamp = System.currentTimeMillis()
        val formattedTimestamp = formatTimestamp(timestamp)

        val enrolledId = sharedPreferences.getString("enrolledId", null)

        if (enrolledId.isNullOrEmpty()) {
            // Handle the case when enrolledId is not set
            return ""
        }

        val file = File(context.filesDir, "${enrolledId}_event_log.txt")
        val fullMessage = "$enrolledId,$formattedTimestamp,$message\n"
        file.appendText(fullMessage)

        // Update the number of lines in SharedPreferences
        val lineCount = file.readLines().size
        sharedPreferences.edit().putInt("numberOfLines", lineCount).apply()

        return fullMessage
    }
}
