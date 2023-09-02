package com.ap.eventlogexporter

import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import android.content.Context

abstract class ModifiedBroadcastReceiver : BroadcastReceiver() {
        open fun getValidReceiverActions(): IntentFilter{
            return IntentFilter()
            }
        open fun registerModifiedReceiver(context: Context) {
        val intentFilter = this.getValidReceiverActions()
        context.registerReceiver(this, intentFilter)
        Log.i(javaClass.name, "${this::class.java.canonicalName} is registered")
    }
}
