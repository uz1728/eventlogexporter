package com.uza.eventlogexporter

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
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

    private val initializeOnStartupReceiver = InitializeOnStartupReceiver.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity Instance Created")
        setContentView(R.layout.main_layout)

        sharedPreferences = applicationContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        if (!enrollmentCompleted) {
            navController.navigate(R.id.enrollmentFragment)
        } else {
            registerInitializeOnStartupReceiver()
            val currentDestinationId = navController.currentDestination?.id
            initializeServices(currentDestinationId)
            if (currentDestinationId != R.id.exportFragment) {
                navigateToExportFragment()
            }
        }
    }

    private fun registerInitializeOnStartupReceiver() {
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
        }
    }

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            isEnabled = false // Disable this callback
            handleCustomBackNavigation()
            isEnabled = true // Re-enable this callback
            }
        }

    private fun handleCustomBackNavigation() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Exit Confirmation")
        alertDialogBuilder.setMessage("Are you sure you want to exit the app?")
        alertDialogBuilder.setPositiveButton("Yes") { _, _ ->
            // Perform any cleanup or exit the app as needed
            finish()
        }
        alertDialogBuilder.setNegativeButton("No") { _, _ ->
            // Do nothing or handle other actions
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
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

