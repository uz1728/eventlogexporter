package com.ap.eventlogexporter

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

class NetworkChangeListener private constructor(context: Context) {

    companion object {
        val TAG = NetworkChangeListener::class.java.simpleName
        private var instance: NetworkChangeListener? = null

        @JvmStatic
        fun getInstance(context: Context): NetworkChangeListener {
            return instance ?: synchronized(this) {
                instance ?: NetworkChangeListener(context.applicationContext).also { instance = it }
            }
        }
    }

    private val conMan: ConnectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val fileWriter = FileWriter(context)

    var lastLoggedConnectionType: String? = null
        private set

    var lastLoggedNetworkType: String? = null
        private set

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            val networkCapabilities = conMan.getNetworkCapabilities(network)
            handleNetworkStateChanged("Connected", networkCapabilities)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            handleNetworkStateChanged("Disconnected", conMan.getNetworkCapabilities(network))
        }

        override fun onUnavailable() {
            super.onUnavailable()
            handleNetworkStateChanged("Unavailable", conMan.getNetworkCapabilities(null))
        }

        override fun onLosing(network: Network, maxMsToLive: Int) {
            super.onLosing(network, maxMsToLive)
            handleNetworkStateChanged(
                "Estimated to Lose Connection in $maxMsToLive ms",
                conMan.getNetworkCapabilities(network)
            )
        }
    }

    private fun handleNetworkStateChanged(
        connectionType: String,
        networkCapabilities: NetworkCapabilities?
    ) {
        val networkType = when {
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
            else -> null
        }

        if (connectionType != lastLoggedConnectionType || networkType != lastLoggedNetworkType) {
            this.lastLoggedConnectionType = connectionType
            this.lastLoggedNetworkType = networkType
            logState()
        }
    }

    fun logState() {
        val message =
            "Network Status: Connection Type: $lastLoggedConnectionType, Network Type: $lastLoggedNetworkType"
        Log.i(TAG, message)
        fileWriter.writeToFile(message)
    }

    private var isListening = false

    fun startListening() {
        if (!isListening) {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            try {
                conMan.registerNetworkCallback(networkRequest, networkCallback)
                isListening = true
                Log.d(TAG, "Started Listening")
            } catch (e: SecurityException) {
                // Handle the case when you don't have the necessary permissions.
                Log.e(TAG, "Failed to register network callback: ${e.message}")
            }
        }
    }

    fun stopListening() {
        if (isListening) {
            try {
                conMan.unregisterNetworkCallback(networkCallback)
                isListening = false
                Log.d(TAG, "Stopped Listening")
            } catch (e: IllegalArgumentException) {
                // Handle the case when the callback is not registered.
                Log.e(TAG, "Failed to unregister network callback: ${e.message}")
            }
        }
    }

    fun getNetworkState(): Pair<String, String?> {
        return Pair(lastLoggedConnectionType ?: "Disconnected", lastLoggedNetworkType)
    }
}
