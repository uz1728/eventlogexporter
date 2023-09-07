package com.uza.eventlogexporter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.uza.eventlogexporter.utils.Utils.isServiceRunning
import com.uza.eventlogexporter.utils.Utils.startEventMonitoringService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        val TAG: String = MainActivity::class.java.simpleName
    }

    private lateinit var sharedPreferences: SharedPreferences

    private val enrollmentCompleted by lazy {
        sharedPreferences.getBoolean("enrollmentCompleted", false)
    }

    private val receiverRegistered by lazy {
        sharedPreferences.getBoolean("receiverRegistered", false)
    }

//    private val eventLogWriter by lazy {
//        EventLogWriter.getInstance(applicationContext)
//    }

    private val networkChangeListener by lazy {
        NetworkChangeListener.getInstance(applicationContext)
    }

    private val initializeOnStartupReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null && intent != null && intent.action == Intent.ACTION_BOOT_COMPLETED) {
                handleBootCompleted(context)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity Instance Created")
        setContentView(R.layout.main_layout)

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        if (!enrollmentCompleted) {
            navController.navigate(R.id.enrollmentFragment)
        } else {
            registerBootCompletedReceiver()
            val currentDestinationId = navController.currentDestination?.id
            initializeServices(currentDestinationId)
        }
    }

    private fun handleBootCompleted(context: Context) {
        try {
        lifecycleScope.launch {
                if (enrollmentCompleted) {
                    networkChangeListener.startListening(applicationContext)
                    networkChangeListener.logState("Device Boot Completed")

                    if (!isServiceRunning(EventMonitoringService::class.java, context)) {
                        startEventMonitoringService(TAG, context)
                    } else {
                        Log.i(TAG, "EventMonitoringService Already Running")
                    }
                }
            }
        }
        catch (exception: Exception) {
            Log.e(TAG, "Exception:", exception)
        }
    }

    private fun registerBootCompletedReceiver() {
        lifecycleScope.launch {
            val intentFilter = IntentFilter().apply {
                addAction(Intent.ACTION_BOOT_COMPLETED)
            }

            if (!receiverRegistered) {
                Log.i(TAG, "Trying to Register InitializeOnStartupReceiver")
                try {
                    registerReceiver(initializeOnStartupReceiver, intentFilter)
                    sharedPreferences.edit().putBoolean("receiverRegistered", true).apply()
                } catch (exception: Exception) {
                    Log.i(TAG, "Exception:", exception)
                }
            }
        }
    }

    private fun initializeServices(currentDestinationId: Int?) {
        lifecycleScope.launch {
            if (!isServiceRunning(EventMonitoringService::class.java, this@MainActivity)) {
                startEventMonitoringService(TAG, this@MainActivity)
            }
            networkChangeListener.startListening(applicationContext)
            if (currentDestinationId != R.id.exportFragment) {
                navigateToExportFragment()
            }
        }
    }

    private fun navigateToExportFragment() {
        val navController =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment)
                .navController
        navController.navigate(R.id.action_enrollmentFragment_to_exportFragment)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity Instance Destroyed")
    }
}

