package com.bridgeconn.soundalert

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppBus {
    private val _serviceState = MutableStateFlow(ServiceState())
    val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

    private val _lastAlert = MutableStateFlow<DetectedAlert?>(null)
    val lastAlert: StateFlow<DetectedAlert?> = _lastAlert.asStateFlow()

    fun publishState(state: ServiceState) {
        _serviceState.value = state
    }

    fun publishAlert(alert: DetectedAlert) {
        _lastAlert.value = alert
    }
}
