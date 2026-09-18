package com.bridgeconn.soundalert

import java.util.concurrent.CopyOnWriteArrayList

object AppState {
    interface Listener {
        fun onSoundAlertState(snapshot: ServiceSnapshot)
    }

    @Volatile
    var snapshot: ServiceSnapshot = ServiceSnapshot()
        private set

    private val listeners = CopyOnWriteArrayList<Listener>()

    fun addListener(listener: Listener) {
        listeners.addIfAbsent(listener)
        listener.onSoundAlertState(snapshot)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun publish(newSnapshot: ServiceSnapshot) {
        snapshot = newSnapshot
        listeners.forEach { it.onSoundAlertState(newSnapshot) }
    }

    fun update(transform: (ServiceSnapshot) -> ServiceSnapshot) {
        publish(transform(snapshot))
    }
}
