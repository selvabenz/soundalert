package com.bridgeconn.soundalert

enum class AlertKind {
    NONE,
    HORN,
    DOOR,
    SIREN
}

enum class ContextProfile {
    ROAD,
    HOME,
    UNCERTAIN
}

data class ServiceSnapshot(
    val running: Boolean = false,
    val alert: AlertKind = AlertKind.NONE,
    val profile: ContextProfile = ContextProfile.UNCERTAIN,
    val sensitivity: Float = 0.70f,
    val flashEnabled: Boolean = false,
    val status: String = "OFF"
)
