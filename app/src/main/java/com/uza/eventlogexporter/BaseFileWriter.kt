package com.uza.eventlogexporter

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.IBinder
import android.util.Log
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

abstract class BaseFileWriter(
    private val fileName: String,
    private var header: String
) {

    companion object {
        val TAG: String = BaseFileWriter::class.java.simpleName
        private const val DATE_FORMAT = "MM-dd-yyyy_HH:mm:ss:SSS_z"
        private const val ID_NOT_SET = "IDNotSet"
        fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            val date = Date(timestamp)
            return sdf.format(date)
        }
    }
    private var eventMonitoringService: EventMonitoringService? = null
    private val context = eventMonitoringService!!.applicationContext
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    private val filePathPref = "${fileName}FilePath"
    private val numberOfLinesPref = "${fileName}NumberOfLines"

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is EventMonitoringService.LocalBinder) {
                eventMonitoringService = service.getService()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            eventMonitoringService = null
        }
    }

    protected open fun writeToFileWithTimestamp(message: String): String? {
        val timestamp = System.currentTimeMillis()
        val formattedTimestamp = formatTimestamp(timestamp)

        var participantId = sharedPreferences.getString("participantId", null)

        if (participantId.isNullOrEmpty()) {
            participantId = ID_NOT_SET
            sharedPreferences.edit().putString("participantId", participantId).commit()
            Log.e(TAG, "participantId is not set")
        }

        val newFileName = "${participantId}_${fileName}_${formattedTimestamp}.csv"
        val newFile = File(context.filesDir, newFileName)
        val fullMessage = "$participantId,$formattedTimestamp,$message\n"

        val (originalFile, found) = getOriginalFile()

        try {
            originalFile.renameTo(newFile)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during renaming to new filename: ${e.message}")
        }

        if (!found) {
            Log.e(TAG, "Latest file was not found, new file was created.")
        }

        newFile.appendText(fullMessage)

        // Update the number of lines in SharedPreferences
        val lineCount = newFile.readLines().size
        sharedPreferences.edit().putInt(numberOfLinesPref, lineCount).apply()
        Log.d(TAG, "$newFileName Length: $lineCount")

        sharedPreferences.edit().putString(filePathPref, newFile.absolutePath).commit()

        return fullMessage
    }

    private fun getOriginalFile(): Pair<File, Boolean> {
        val originalFilePath = sharedPreferences.getString(filePathPref, null)
        val found = originalFilePath != null && File(originalFilePath).exists()

        val originalFile = originalFilePath?.let { File(it) } ?: createANewFile()

        return Pair(originalFile, found)
    }

    private fun createANewFile(): File {
        var participantId = sharedPreferences.getString("participantId", null)

        if (participantId.isNullOrEmpty()) {
            participantId = ID_NOT_SET
            sharedPreferences.edit().putString("participantId", participantId).commit()
            Log.e(TAG, "participantId is not set")
        }

        val newFileName = "${participantId}_${fileName}.csv"
        val newFile = File(context.filesDir, newFileName)

        if (!newFile.exists()) {
            try {
                newFile.createNewFile() // Create the new file here
                sharedPreferences.edit().putString(filePathPref, newFile.absolutePath).commit()
                if (!header.endsWith("\n")) {
                    header = "$header\n"
                }
                newFile.appendText(header)
            } catch (e: IOException) {
                Log.e(TAG, "Error creating ${newFile.absolutePath}: ${e.message}")
            }
        }
        return newFile
    }
}
