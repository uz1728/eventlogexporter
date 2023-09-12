package com.uza.eventlogexporter

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.IBinder
import android.util.Log

class NetworkChangeListener private constructor(context: Context) {

    companion object {
        val TAG: String = NetworkChangeListener::class.java.simpleName
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

    private val eventLogWriter = EventLogWriter.getInstance(context)

    private var eventMonitoringService: EventMonitoringService? = null

    private var lastLoggedConnectionType: String? = null
    private var lastLoggedNetworkType: String? = null
    private var isListening = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            handleNetworkStateChanged("Connected", conMan.getNetworkCapabilities(network))
        }

        override fun onLost(network: Network) {
            handleNetworkStateChanged("Disconnected", conMan.getNetworkCapabilities(network))
        }

        override fun onUnavailable() {
            handleNetworkStateChanged("Unavailable", conMan.getNetworkCapabilities(null))
        }

        override fun onLosing(network: Network, maxMsToLive: Int) {
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
            logState("Network State Changed")
        }
    }

    fun logState(deviceStatus: String?) {
        val message =
            "$deviceStatus,$lastLoggedConnectionType,$lastLoggedNetworkType"
        Log.i(TAG, message)
        eventLogWriter.logMessageWithTimestamp(message)

        // Check if the service reference is not null before calling the function
        eventMonitoringService?.updateNotificationContent(message)
    }


    // Combine the logic for starting to listen and binding to the service
    fun startListening(context: Context) {
        if (!isListening) {
            val intent = Intent(context, EventMonitoringService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            try {
                conMan.registerNetworkCallback(networkRequest, networkCallback)
                isListening = true
                Log.d(TAG, "Started Listening")
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to register network callback: ${e.message}")
            }
        }
    }

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
}

//    fun stopListening() {
//        if (isListening) {
//            try {
//                conMan.unregisterNetworkCallback(networkCallback)
//                isListening = false
//                Log.d(TAG, "Stopped Listening")
//            } catch (e: IllegalArgumentException) {
//                Log.e(TAG, "Failed to unregister network callback: ${e.message}")
//            }
//        }
//    }
//
//    fun getNetworkState(): Any {
//        val pair = Pair(lastLoggedConnectionType ?: "null", lastLoggedNetworkType ?: "null")
//        return pair.toString()
//    }
//}
