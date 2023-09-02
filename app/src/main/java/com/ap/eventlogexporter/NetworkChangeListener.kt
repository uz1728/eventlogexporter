package com.ap.eventlogexporter

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

class NetworkChangeListener(context: Context) {
    companion object {
        @Volatile
        private var instance: NetworkChangeListener? = null

        fun getInstance(context: Context): NetworkChangeListener {
            return instance ?: synchronized(this) {
                instance ?: NetworkChangeListener(context).also { instance = it }
            }
        }
    }

    private val conMan: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val fileWriter = FileWriter(context)

    private var networkCapabilities: NetworkCapabilities? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            networkCapabilities = conMan.getNetworkCapabilities(network)

            if (networkCapabilities == null) {
                // NetworkCapabilities is null, handle this case if necessary.
                val message = "Some Network is Connected but NetworkCapabilities is null"
                Log.d("NetworkChangeListener", message)
                fileWriter.writeToFile(message)
            } else {
                // Access the network capabilities here
                if (networkCapabilities!!.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    // WiFi capabilities
                    val message = "Wi-Fi is Connected"
                    Log.i("NetworkChangeListener", message)
                    fileWriter.writeToFile(message)
                } else if (networkCapabilities!!.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    // Cellular capabilities
                    val message = "Cellular Network is Connected"
                    Log.i("NetworkChangeListener", message)
                    fileWriter.writeToFile(message)
                }
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            if (networkCapabilities != null) {
                if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                    // WiFi connection is lost
                    val message = "Wi-Fi is Disconnected"
                    Log.i("NetworkChangeListener", message)
                    fileWriter.writeToFile(message)
                } else if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) {
                    // Cellular connection is lost
                    val message = "Cellular Network is Disconnected"
                    Log.i("NetworkChangeListener", message)
                    fileWriter.writeToFile(message)
                }
            }
            else {
                val message = "Some Network is Disconnected but its NetworkCapabilities is null"
                Log.i("NetworkChangeListener", message)
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

