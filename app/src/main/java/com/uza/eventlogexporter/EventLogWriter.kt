package com.uza.eventlogexporter

import android.content.Context

class EventLogWriter private constructor(context: Context) : BaseFileWriter(context, "event_log", "Participant,Timestamp,Device Status,Connection Type,Network Type\n") {
    companion object {
        private var instance: EventLogWriter? = null

        @JvmStatic
        fun getInstance(context: Context): EventLogWriter {
            return instance ?: synchronized(this) {
                instance ?: EventLogWriter(context).also {
                    instance = it
                }
            }
        }
    }
    fun logMessageWithTimestamp(message: String) {
        writeToFileWithTimestamp(message)
    }
}