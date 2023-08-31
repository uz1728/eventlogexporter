package com.ap.eventlogexporter

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

class NetworkChangeListener(private val context: Context) {

    private val conMan: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val fileWriter = FileWriter(context)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            val networkCapabilities = conMan.getNetworkCapabilities(network)
            if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                // WiFi connection becomes available
                val message = "Connected to Wi-Fi"
                Log.d("NetworkChangeListener", message)
                fileWriter.writeToFile(message)
            } else if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) {
                // Cellular connection becomes available
                val message = "Connected to Cellular Network"
                Log.d("NetworkChangeListener", message)
                fileWriter.writeToFile(message)
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            val networkCapabilities = conMan.getNetworkCapabilities(network)
            if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                // WiFi connection is lost
                val message = "Disconnected from Wi-Fi"
                Log.d("NetworkChangeListener", message)
                fileWriter.writeToFile(message)
            } else if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) {
                // Cellular connection is lost
                val message = "Disconnected from Cellular Network"
                Log.d("NetworkChangeListener", message)
                fileWriter.writeToFile(message)
            }
        }
    }

    private var isListening = false

    fun startListening() {
        if (!isListening) {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            conMan.registerNetworkCallback(networkRequest, networkCallback)
            isListening = true
        }
    }

    fun stopListening() {
        if (isListening) {
            conMan.unregisterNetworkCallback(networkCallback)
            isListening = false
        }
    }
}

