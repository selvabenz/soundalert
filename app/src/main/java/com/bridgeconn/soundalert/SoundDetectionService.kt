package com.bridgeconn.soundalert

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import com.bridgeconn.soundalert.audio.DetectionPolicy
import com.bridgeconn.soundalert.audio.LabelScore
import com.bridgeconn.soundalert.audio.MultiFrameGate
import com.bridgeconn.soundalert.audio.YamnetEngine
import java.util.concurrent.atomic.AtomicBoolean

class SoundDetectionService : Service(), YamnetEngine.Listener {
    private val running = AtomicBoolean(false)
    private var engine: YamnetEngine? = null
    private lateinit var policy: DetectionPolicy
    private lateinit var gate: MultiFrameGate
    private lateinit var contextDetector: ContextDetector
    private lateinit var calibration: CalibrationManager
    private lateinit var flashController: FlashController
    private lateinit var wearBridge: WearBridge
    private var wakeLock: PowerManager.WakeLock? = null

    @Volatile private var sensitivity = 0.70f
    @Volatile private var flashEnabled = false
    private var armed = true
    private var rearmAt = 0L
    private var quietFrames = 0
    private var lastProfile = ContextProfile.UNCERTAIN

    override fun onCreate() {
        super.onCreate()
        policy = DetectionPolicy()
        gate = MultiFrameGate()
        contextDetector = ContextDetector(this)
        calibration = CalibrationManager(this)
        flashController = FlashController(this)
        wearBridge = WearBridge(this)
        createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_START -> {
                readSettings(intent)
                if (!hasAudioPermission()) {
                    publishStopped("MIC")
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForegroundListening()
                startListeningIfNeeded()
            }
            ACTION_UPDATE -> {
                readSettings(intent)
                AppState.update { it.copy(sensitivity = sensitivity, flashEnabled = flashEnabled) }
            }
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopListening()
        flashController.close()
        publishStopped("OFF")
        try { stopForeground(STOP_FOREGROUND_REMOVE) } catch (_: Throwable) { }
        super.onDestroy()
    }

    private fun readSettings(intent: Intent?) {
        val prefs = getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
        sensitivity = (intent?.getFloatExtra(
            EXTRA_SENSITIVITY,
            prefs.getFloat(MainActivity.KEY_SENSITIVITY, 0.70f)
        ) ?: prefs.getFloat(MainActivity.KEY_SENSITIVITY, 0.70f)).coerceIn(0.30f, 0.95f)

        flashEnabled = intent?.getBooleanExtra(
            EXTRA_FLASH,
            prefs.getBoolean(MainActivity.KEY_FLASH, false)
        ) ?: prefs.getBoolean(MainActivity.KEY_FLASH, false)
    }

    private fun hasAudioPermission(): Boolean =
        checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun startListeningIfNeeded() {
        if (!running.compareAndSet(false, true)) return

        acquireWakeLock()
        contextDetector.start()
        armed = true
        quietFrames = 0
        gate.reset()

        AppState.publish(
            ServiceSnapshot(
                running = true,
                alert = AlertKind.NONE,
                profile = ContextProfile.UNCERTAIN,
                sensitivity = sensitivity,
                flashEnabled = flashEnabled,
                status = "ON"
            )
        )

        try {
            engine = YamnetEngine(this, this).also { it.start() }
        } catch (_: Throwable) {
            running.set(false)
            publishStopped("MODEL")
            stopSelf()
        }
    }

    @Synchronized
    override fun onScores(scores: List<LabelScore>) {
        if (!running.get() || scores.isEmpty()) return

        val profile = contextDetector.updateFromAudio(scores)
        if (profile != lastProfile) {
            lastProfile = profile
            AppState.update { it.copy(profile = profile) }
        }

        val background = policy.backgroundScore(scores)
        calibration.observe(profile, background)

        if (!armed) {
            handleRearm(scores)
            return
        }

        val candidate = policy.evaluate(
            scores = scores,
            sensitivity = sensitivity,
            profile = profile,
            calibrationAdjustment = calibration.thresholdAdjustment(profile)
        )
        val confirmed = gate.push(candidate)
        if (confirmed != null) triggerAlert(confirmed.kind)
    }

    override fun onError(message: String) {
        // One transient classifier error is not fatal. The foreground service remains alive.
        AppState.update { it.copy(status = if (it.running) "ON" else it.status) }
    }

    private fun triggerAlert(kind: AlertKind) {
        if (kind == AlertKind.NONE || !armed) return
        armed = false
        quietFrames = 0
        gate.reset()
        rearmAt = SystemClock.elapsedRealtime() + Haptics.duration(kind) + 350L

        AppState.update {
            it.copy(
                running = true,
                alert = kind,
                profile = lastProfile,
                sensitivity = sensitivity,
                flashEnabled = flashEnabled,
                status = kind.name
            )
        }

        Haptics.vibrate(this, kind)
        flashController.flash(kind, flashEnabled)
        wearBridge.send(kind)
        showAlertNotification(kind)
    }

    private fun handleRearm(scores: List<LabelScore>) {
        if (SystemClock.elapsedRealtime() < rearmAt) return
        if (policy.maxRelevantScore(scores) < 0.13f) quietFrames++ else quietFrames = 0
        if (quietFrames >= 2) {
            armed = true
            quietFrames = 0
            gate.reset()
            AppState.update {
                it.copy(
                    running = true,
                    alert = AlertKind.NONE,
                    status = "ON",
                    sensitivity = sensitivity,
                    flashEnabled = flashEnabled
                )
            }
        }
    }

    private fun stopListening() {
        running.set(false)
        try { engine?.close() } catch (_: Throwable) { }
        engine = null
        try { contextDetector.stop() } catch (_: Throwable) { }
        releaseWakeLock()
        gate.reset()
        armed = true
        quietFrames = 0
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SoundAlert:Listening").apply {
            setReferenceCounted(false)
            try { acquire() } catch (_: Throwable) { }
        }
    }

    private fun releaseWakeLock() {
        try { wakeLock?.takeIf { it.isHeld }?.release() } catch (_: Throwable) { }
        wakeLock = null
    }

    private fun createChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(LISTENING_CHANNEL, getString(R.string.listening_channel), NotificationManager.IMPORTANCE_LOW).apply {
                setSound(null, null)
                enableVibration(false)
                description = "SoundAlert active"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(ALERT_CHANNEL, getString(R.string.alert_channel), NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(false)
                description = "Important sound alerts"
            }
        )
    }

    private fun startForegroundListening() {
        val notification = buildListeningNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                LISTENING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(LISTENING_NOTIFICATION_ID, notification)
        }
    }

    private fun buildListeningNotification(): Notification =
        Notification.Builder(this, LISTENING_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("SoundAlert")
            .setContentText("●")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(mainPendingIntent())
            .build()

    private fun showAlertNotification(kind: AlertKind) {
        val title = when (kind) {
            AlertKind.HORN -> "HORN"
            AlertKind.DOOR -> "DOOR"
            AlertKind.SIREN -> "SIREN"
            AlertKind.NONE -> "SoundAlert"
        }
        val notification = Notification.Builder(this, ALERT_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("SoundAlert")
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent())
            .build()
        try { getSystemService(NotificationManager::class.java).notify(ALERT_NOTIFICATION_ID, notification) } catch (_: Throwable) { }
    }

    private fun mainPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun publishStopped(status: String) {
        AppState.publish(
            ServiceSnapshot(
                running = false,
                alert = AlertKind.NONE,
                profile = ContextProfile.UNCERTAIN,
                sensitivity = sensitivity,
                flashEnabled = flashEnabled,
                status = status
            )
        )
    }

    companion object {
        private const val ACTION_START = "com.bridgeconn.soundalert.START"
        private const val ACTION_UPDATE = "com.bridgeconn.soundalert.UPDATE"
        private const val ACTION_STOP = "com.bridgeconn.soundalert.STOP"
        private const val EXTRA_SENSITIVITY = "sensitivity"
        private const val EXTRA_FLASH = "flash"
        private const val LISTENING_CHANNEL = "soundalert_listening_v2"
        private const val ALERT_CHANNEL = "soundalert_alerts_v2"
        private const val LISTENING_NOTIFICATION_ID = 42020
        private const val ALERT_NOTIFICATION_ID = 42021

        fun start(context: Context, sensitivity: Float, flashEnabled: Boolean) {
            val intent = Intent(context, SoundDetectionService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SENSITIVITY, sensitivity)
                putExtra(EXTRA_FLASH, flashEnabled)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun update(context: Context, sensitivity: Float, flashEnabled: Boolean) {
            if (!AppState.snapshot.running) return
            val intent = Intent(context, SoundDetectionService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_SENSITIVITY, sensitivity)
                putExtra(EXTRA_FLASH, flashEnabled)
            }
            try { context.startService(intent) } catch (_: Throwable) { }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SoundDetectionService::class.java).apply { action = ACTION_STOP }
            try { context.startService(intent) } catch (_: Throwable) { context.stopService(intent) }
        }
    }
}
