package com.uza.eventlogexporter

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

class UploadFileTask {
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
                // Create an OkHttpClient with a custom timeout
                val client = OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS) // Set connection timeout to 10 seconds
                    .readTimeout(10, TimeUnit.SECONDS)    // Set read timeout to 10 seconds
                    .build()

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
            } catch (e: SocketTimeoutException) {
                // Handle socket timeout exception here
                val msg = "Socket Timeout Error: Took too long to connect"
                Log.e(TAG, msg)
                msg
            } catch (e: IOException) {
                // Handle other IO exceptions here
                val msg = "IO Error: ${e.message}"
                Log.e(TAG, msg)
                msg
            }
        }
    }

    private fun buildRequest(fileUri: File): Request {

        val serverUrl = "localhost:8080" //"https://metalangue.link/upload" // Get the server URL
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
        val secureServerUrl = serverUrl
//        if (!serverUrl.startsWith("https://")) {
//            "https://$serverUrl"
//        } else {
//            serverUrl
//        }
        return Request.Builder()
            .url(secureServerUrl)
            .post(requestBody)
            .build()
    }

}
