package com.bridgeconn.soundalert

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.View
import android.view.WindowManager

class MainActivity : Activity(), AppState.Listener {
    private lateinit var alertView: SoundAlertView
    private val prefs by lazy { getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    private var requestedStartAfterPermission = false
    private var requestedFlashAfterPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )

        val sensitivity = prefs.getFloat(KEY_SENSITIVITY, 0.70f).coerceIn(0.30f, 0.95f)
        val flash = prefs.getBoolean(KEY_FLASH, false)

        alertView = SoundAlertView(this).apply {
            setSensitivity(sensitivity)
            setFlashEnabled(flash)
            callbacks = object : SoundAlertView.Callbacks {
                override fun onPowerToggle(turnOn: Boolean) {
                    if (turnOn) enableListening() else disableListening()
                }

                override fun onSensitivityChanged(value: Float) {
                    val bounded = value.coerceIn(0.30f, 0.95f)
                    prefs.edit().putFloat(KEY_SENSITIVITY, bounded).apply()
                    SoundDetectionService.update(this@MainActivity, bounded, prefs.getBoolean(KEY_FLASH, false))
                }

                override fun onFlashToggle() {
                    val currentlyEnabled = prefs.getBoolean(KEY_FLASH, false)
                    if (currentlyEnabled) {
                        applyFlashSetting(false)
                    } else if (hasCameraPermission()) {
                        applyFlashSetting(true)
                    } else {
                        requestedFlashAfterPermission = true
                        requestPermissions(arrayOf(Manifest.permission.CAMERA), FLASH_PERMISSION_REQUEST)
                    }
                }
            }
        }
        setContentView(alertView)

        // If the user left SoundAlert ON previously, reopening the visible activity resumes listening.
        if (prefs.getBoolean(KEY_ENABLED, false)) {
            enableListening()
        }
    }

    override fun onStart() {
        super.onStart()
        AppState.addListener(this)
    }

    override fun onStop() {
        AppState.removeListener(this)
        super.onStop()
    }

    override fun onSoundAlertState(snapshot: ServiceSnapshot) {
        runOnUiThread {
            alertView.setSnapshot(snapshot)
        }
    }

    private fun enableListening() {
        if (!hasMicrophonePermission()) {
            requestedStartAfterPermission = true
            requestNeededPermissions()
            return
        }
        val sensitivity = sensitivityValue()
        val flash = prefs.getBoolean(KEY_FLASH, false)
        prefs.edit().putBoolean(KEY_ENABLED, true).apply()
        SoundDetectionService.start(this, sensitivity, flash)
        alertView.setDesiredOn(true)
    }

    private fun disableListening() {
        requestedStartAfterPermission = false
        prefs.edit().putBoolean(KEY_ENABLED, false).apply()
        stopService(android.content.Intent(this, SoundDetectionService::class.java))
        alertView.setDesiredOn(false)
    }

    private fun sensitivityValue(): Float =
        prefs.getFloat(KEY_SENSITIVITY, 0.70f).coerceIn(0.30f, 0.95f)

    private fun applyFlashSetting(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FLASH, enabled).apply()
        alertView.setFlashEnabled(enabled)
        SoundDetectionService.update(this, sensitivityValue(), enabled)
    }

    private fun hasCameraPermission(): Boolean =
        checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun hasMicrophonePermission(): Boolean =
        checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun requestNeededPermissions() {
        val list = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) list += Manifest.permission.POST_NOTIFICATIONS
        requestPermissions(list.toTypedArray(), PERMISSION_REQUEST)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST -> if (requestedStartAfterPermission) {
                requestedStartAfterPermission = false
                if (hasMicrophonePermission()) enableListening() else disableListening()
            }
            FLASH_PERMISSION_REQUEST -> if (requestedFlashAfterPermission) {
                requestedFlashAfterPermission = false
                applyFlashSetting(hasCameraPermission())
            }
        }
    }

    companion object {
        const val PREFS = "soundalert_user"
        const val KEY_ENABLED = "enabled"
        const val KEY_SENSITIVITY = "sensitivity"
        const val KEY_FLASH = "flash"
        private const val PERMISSION_REQUEST = 4202
        private const val FLASH_PERMISSION_REQUEST = 4203
    }
}
