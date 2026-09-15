package com.example.messengerwrapper

object DebugConsoleStore {
    private val logs = mutableListOf<String>()

    fun addLog(message: String) {
        synchronized(logs) {
            if (logs.size > 150) logs.removeAt(0)
            logs.add(message)
        }
    }

    fun getLogs(): List<String> = synchronized(logs) { logs.toList() }
}
