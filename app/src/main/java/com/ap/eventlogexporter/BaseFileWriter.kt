package com.uza.eventlogexporter

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

abstract class BaseFileWriter(
    protected val context: Context,
    private val fileName: String,
    private var header: String
) {

    companion object {
        val TAG: String = BaseFileWriter::class.java.simpleName
        const val DATE_FORMAT = "MM-dd-yyyy_HH:mm:ss:SSS_z"
        fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            val date = Date(timestamp)
            return sdf.format(date)
        }
    }

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    private val filePathPref = "${fileName}FilePath"
    private val numberOfLinesPref = "${fileName}NumberOfLines"

    protected open fun writeToFileWithTimestamp(message: String): String? {
        val timestamp = System.currentTimeMillis()
        val formattedTimestamp = formatTimestamp(timestamp)

        val participantId = sharedPreferences.getString("participantId", null)

        if (participantId.isNullOrEmpty()) {
            // Handle the case when participantId is not set
            return null
        }

        var originalFile = sharedPreferences.getString(filePathPref, null)
            ?.let { File(it) }

        if (originalFile == null) {
            Log.e(
                TAG,
                "${fileName} was removed or deleted or never created, creating new one"
            )
            // Check if the file exists, and if not, create it
            val createdFile = File(context.filesDir, "${participantId}_${fileName}.csv")
            if (!createdFile.exists()) {
                createNewFile(createdFile)
            }
            sharedPreferences.edit().putString(filePathPref, createdFile.absolutePath).apply()
            originalFile = createdFile
        }

        try {
            val newFileName = "${participantId}_${fileName}_${formattedTimestamp}.csv"
            val newFile = File(context.filesDir, newFileName)

            return if (originalFile.renameTo(newFile)) {
                // Append the message to the newly renamed file
                val fullMessage = "$participantId,$formattedTimestamp,$message\n"
                newFile.appendText(fullMessage)

                // Update the number of lines in SharedPreferences
                val lineCount = newFile.readLines().size
                sharedPreferences.edit().putInt(numberOfLinesPref, lineCount).apply()
                Log.d(TAG, "${newFileName} Length: $lineCount")

                sharedPreferences.edit().putString(filePathPref, newFile.absolutePath).apply()

                fullMessage

            } else {
                // Failed to rename the file, handle this case as needed
                Log.e(TAG, "Failed to rename original file to ${newFileName}")
                return null
            }
        } catch (e: Exception) {
            // Capture the specific exception that caused the renaming to fail
            Log.e(TAG, "Error during renaming to new filename: ${e.message}")
            return null
        }
    }

    private fun createNewFile(file: File) {
        try {
            file.createNewFile()
            if (!header.endsWith("\n")) {
                header = "$header\n"
            }
            file.appendText(header)
        } catch (e: IOException) {
            Log.e(TAG, "Error creating ${file.absolutePath}: ${e.message}")
        }
    }
}
