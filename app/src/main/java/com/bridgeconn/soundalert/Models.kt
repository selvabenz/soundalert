package com.bridgeconn.soundalert

enum class AlertMode {
    ROAD,
    HOME;

    companion object {
        fun from(value: String?): AlertMode =
            entries.firstOrNull { it.name == value } ?: ROAD
    }
}

enum class SoundLabel {
    HORN,
    DOORBELL_OR_KNOCK
}

data class DetectedAlert(
    val id: Long = System.nanoTime(),
    val label: SoundLabel,
    val confidence: Float,
    val timestampMillis: Long = System.currentTimeMillis(),
    val detail: String = ""
)

data class ServiceState(
    val running: Boolean = false,
    val mode: AlertMode = AlertMode.ROAD,
    val status: String = "Stopped",
    val lastFrameConfidence: Float = 0f
)
