package com.example.messengerwrapper

class NativeBridge {
    companion object {
        init {
            System.loadLibrary("bridge_worker")
        }
    }

    external fun nativeBridgeWorker(payload: String): String
}
