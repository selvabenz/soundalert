package com.bridgeconn.soundalert

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class FlashController(context: Context) {
    private val manager = context.getSystemService(CameraManager::class.java)
    private val executor = Executors.newSingleThreadExecutor()
    private val busy = AtomicBoolean(false)
    private val cameraId: String? by lazy {
        try {
            manager.cameraIdList.firstOrNull { id ->
                manager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun flash(kind: AlertKind, enabled: Boolean) {
        if (!enabled || kind == AlertKind.NONE || !busy.compareAndSet(false, true)) return
        val id = cameraId ?: run {
            busy.set(false)
            return
        }
        executor.execute {
            try {
                val sequence = when (kind) {
                    AlertKind.HORN -> intArrayOf(140, 110, 140)
                    AlertKind.DOOR -> intArrayOf(80, 80, 80, 80, 80, 80, 80)
                    AlertKind.SIREN -> intArrayOf(80, 70, 80, 70, 80, 70, 80, 70, 80, 70, 80)
                    AlertKind.NONE -> intArrayOf()
                }
                var on = true
                for (duration in sequence) {
                    manager.setTorchMode(id, on)
                    Thread.sleep(duration.toLong())
                    on = !on
                }
            } catch (_: Throwable) {
                // Flash is optional. Camera contention or unsupported hardware must never stop alerts.
            } finally {
                try { manager.setTorchMode(id, false) } catch (_: Throwable) { }
                busy.set(false)
            }
        }
    }

    fun close() {
        try { cameraId?.let { manager.setTorchMode(it, false) } } catch (_: Throwable) { }
        executor.shutdownNow()
    }
}
