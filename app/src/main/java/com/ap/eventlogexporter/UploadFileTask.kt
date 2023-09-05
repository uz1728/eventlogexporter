package com.ap.eventlogexporter

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger

class UploadFileTask() {
    companion object {
        val TAG: String = UploadFileTask::class.java.simpleName
        private val LOGGER = Logger.getLogger(UploadFileTask::class.java.name)
    }

    suspend fun uploadFile(fileUri: File?): String {
        if (fileUri == null) {
            val msg = "Error: File URI was null"
            Log.e(TAG, msg)
            return msg
        }

        // Set OkHttpClient logger level to FINE
        LOGGER.level = Level.FINE

        return withContext(Dispatchers.IO) {
            try {

                val client = OkHttpClient()

                val response = client.newCall(buildRequest(fileUri)).execute()

                response.use {
                    val responseCode = response.code
                    val responseBody = response.body?.string()
                    val success = response.isSuccessful

                    val msg = if (success) "File upload successful." else "File upload failed."
                    val serverResponse = "$msg Server Response: $responseCode-$responseBody"

                    Log.i(TAG, serverResponse)

                    serverResponse
                }
            }

            catch (e: IOException) {

                val msg = "Error: ${e.message}"
                Log.e(TAG, msg)

                msg

            }
        }
    }

    private fun buildRequest(fileUri: File): Request {

        val serverUrl = "https://metalangue.link/upload" // Get the server URL
            val uploadToken = "d9pzu2swbktazaxh0cu!@#%^*_+=-utfib8gtai5q1f9m9gywkg3917qb2crh97woqemmk7q!@#%^*_+=-"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                fileUri.name,
                fileUri.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            )
            .addFormDataPart("token", uploadToken) // Add the verification token
            .build()
        val secureServerUrl = if (!serverUrl.startsWith("https://")) {
            "https://$serverUrl"
        } else {
            serverUrl
        }
        return Request.Builder()
            .url(secureServerUrl)
            .post(requestBody)
            .build()
    }

}
