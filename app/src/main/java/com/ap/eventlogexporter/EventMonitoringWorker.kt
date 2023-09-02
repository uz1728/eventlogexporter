package com.ap.eventlogexporter

import android.content.Context
import java.util.concurrent.TimeUnit
import androidx.work.*

abstract class EventMonitoringWorker(context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {
        val a = context
    }

fun startEventMonitoringWork(context: Context) {

    val workRequest: PeriodicWorkRequest =
        PeriodicWorkRequestBuilder<EventMonitoringWorker>(1, TimeUnit.MINUTES)
            .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "event",
        ExistingPeriodicWorkPolicy.UPDATE,
        workRequest
    )
}
