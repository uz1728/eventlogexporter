package com.uza.eventlogexporter

class EventLogWriter private constructor() : BaseFileWriter("event_log", "Participant,Timestamp,Device Status,Connection Type,Network Type\n") {
    companion object {
        private var instance: EventLogWriter? = null

        @JvmStatic
        fun getInstance(): EventLogWriter {
            return instance ?: synchronized(this) {
                instance ?: EventLogWriter().also {
                    instance = it
                }
            }
        }
    }
    fun logMessageWithTimestamp(message: String) {
        writeToFileWithTimestamp(message)
    }
}