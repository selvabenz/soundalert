package com.bridgeconn.soundalert

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.bridgeconn.soundalert.audio.HeuristicSoundClassifier
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

class SoundDetectionService : Service() {

    private val running = AtomicBoolean(false)
    private var workerThread: Thread? = null
    private var audioRecord: AudioRecord? = null
    private var classifier = HeuristicSoundClassifier(SAMPLE_RATE)

    @Volatile private var mode: AlertMode = AlertMode.ROAD
    @Volatile private var sensitivity: Float = 0.70f
    @Volatile private var cooldownMillis: Long = 2500L
    private var lastAlertMillis: Long = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_START -> {
                readSettings(intent)
                startAsForeground()
                startListening()
            }
            ACTION_UPDATE -> {
                readSettings(intent)
                AppBus.publishState(
                    AppBus.serviceState.value.copy(
                        running = running.get(),
                        mode = mode,
                        status = if (running.get()) statusText() else "Stopped"
                    )
                )
                updateForegroundNotification()
            }
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopListening()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        AppBus.publishState(ServiceState(running = false, mode = mode, status = "Stopped"))
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun readSettings(intent: Intent) {
        mode = AlertMode.from(intent.getStringExtra(EXTRA_MODE))
        sensitivity = intent.getFloatExtra(EXTRA_SENSITIVITY, sensitivity).coerceIn(0.30f, 0.95f)
        cooldownMillis = intent.getLongExtra(EXTRA_COOLDOWN_MS, cooldownMillis).coerceIn(1000L, 10_000L)
    }

    private fun startAsForeground() {
        val notification = buildListeningNotification()
        ServiceCompat.startForeground(
            this,
            LISTENING_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )
    }

    private fun startListening() {
        if (running.getAndSet(true)) return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            running.set(false)
            AppBus.publishState(ServiceState(false, mode, "Microphone permission required"))
            stopSelf()
            return
        }

        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat)
        if (minBufferBytes <= 0) {
            running.set(false)
            AppBus.publishState(ServiceState(false, mode, "Audio input unavailable"))
            stopSelf()
            return
        }

        val bufferBytes = max(minBufferBytes, FRAME_SIZE * 2 * 3)
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                channelConfig,
                audioFormat,
                bufferBytes
            )
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                throw IllegalStateException("AudioRecord could not initialize")
            }
            classifier.reset()
            audioRecord?.startRecording()
        } catch (t: Throwable) {
            running.set(false)
            audioRecord?.release()
            audioRecord = null
            AppBus.publishState(ServiceState(false, mode, "Microphone error: ${t.message ?: "unknown"}"))
            stopSelf()
            return
        }

        AppBus.publishState(ServiceState(true, mode, statusText()))
        workerThread = Thread({ detectionLoop() }, "SoundAlertDetector").apply { start() }
    }

    private fun detectionLoop() {
        val buffer = ShortArray(FRAME_SIZE)
        while (running.get()) {
            val record = audioRecord ?: break
            val read = try {
                record.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
            } catch (_: Throwable) {
                break
            }
            if (read <= 0) continue

            val result = classifier.analyze(buffer, read, mode, sensitivity)
            if (result != null) {
                val now = System.currentTimeMillis()
                if (now - lastAlertMillis >= cooldownMillis) {
                    lastAlertMillis = now
                    triggerAlert(result)
                }
            }
        }
    }

    private fun triggerAlert(result: HeuristicSoundClassifier.Classification) {
        val alertMode = if (result.label == SoundLabel.HORN) AlertMode.ROAD else AlertMode.HOME
        val event = DetectedAlert(
            label = result.label,
            confidence = result.confidence,
            detail = result.detail
        )
        AppBus.publishAlert(event)
        AppBus.publishState(
            AppBus.serviceState.value.copy(
                running = true,
                mode = mode,
                status = if (alertMode == AlertMode.ROAD) "Horn detected" else "Door sound detected",
                lastFrameConfidence = result.confidence
            )
        )
        Haptics.vibrate(this, alertMode)
        showAlertNotification(event)
    }

    private fun stopListening() {
        if (!running.getAndSet(false)) return
        try { audioRecord?.stop() } catch (_: Throwable) { }
        workerThread?.interrupt()
        try { workerThread?.join(300) } catch (_: InterruptedException) { }
        workerThread = null
        audioRecord?.release()
        audioRecord = null
        classifier.reset()
    }

    private fun statusText(): String = when (mode) {
        AlertMode.ROAD -> "Listening for horns"
        AlertMode.HOME -> "Listening for doorbell / knocks"
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                LISTENING_CHANNEL,
                getString(R.string.listening_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when SoundAlert is actively using the microphone"
                setSound(null, null)
                enableVibration(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                ALERT_CHANNEL,
                getString(R.string.alert_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Visual notifications for detected horns and door sounds"
                setSound(null, null)
                enableVibration(false) // custom vibration is generated directly by Haptics
            }
        )
    }

    private fun buildListeningNotification() =
        NotificationCompat.Builder(this, LISTENING_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("SoundAlert is listening")
            .setContentText(statusText())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(mainActivityPendingIntent())
            .build()

    private fun updateForegroundNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(LISTENING_NOTIFICATION_ID, buildListeningNotification())
    }

    private fun showAlertNotification(event: DetectedAlert) {
        val horn = event.label == SoundLabel.HORN
        val title = if (horn) "HORN DETECTED" else "SOMEONE MAY BE AT THE DOOR"
        val body = if (horn) "Traffic horn-like sound detected nearby" else "Doorbell or knock-like sound detected"
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(mainActivityPendingIntent())
            .build()
        getSystemService(NotificationManager::class.java).notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun mainActivityPendingIntent(): PendingIntent {
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

    companion object {
        private const val SAMPLE_RATE = 16_000
        private const val FRAME_SIZE = 2048
        private const val LISTENING_CHANNEL = "soundalert_listening"
        private const val ALERT_CHANNEL = "soundalert_alerts"
        private const val LISTENING_NOTIFICATION_ID = 4101
        private const val ALERT_NOTIFICATION_ID = 4102

        private const val ACTION_START = "com.bridgeconn.soundalert.START"
        private const val ACTION_UPDATE = "com.bridgeconn.soundalert.UPDATE"
        private const val ACTION_STOP = "com.bridgeconn.soundalert.STOP"
        private const val EXTRA_MODE = "mode"
        private const val EXTRA_SENSITIVITY = "sensitivity"
        private const val EXTRA_COOLDOWN_MS = "cooldown_ms"

        fun start(context: Context, mode: AlertMode, sensitivity: Float, cooldownMillis: Long) {
            val intent = Intent(context, SoundDetectionService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_MODE, mode.name)
                putExtra(EXTRA_SENSITIVITY, sensitivity)
                putExtra(EXTRA_COOLDOWN_MS, cooldownMillis)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun update(context: Context, mode: AlertMode, sensitivity: Float, cooldownMillis: Long) {
            val intent = Intent(context, SoundDetectionService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_MODE, mode.name)
                putExtra(EXTRA_SENSITIVITY, sensitivity)
                putExtra(EXTRA_COOLDOWN_MS, cooldownMillis)
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SoundDetectionService::class.java))
        }
    }
}
