package com.bridgeconn.soundalert.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.SystemClock
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifierResult
import com.google.mediapipe.tasks.audio.core.RunningMode
import com.google.mediapipe.tasks.components.containers.AudioData
import com.google.mediapipe.tasks.components.containers.AudioData.AudioDataFormat
import com.google.mediapipe.tasks.core.BaseOptions
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class YamnetEngine(
    context: Context,
    private val listener: Listener
) : AutoCloseable {
    interface Listener {
        fun onScores(scores: List<LabelScore>)
        fun onError(message: String)
    }

    private val classifier: AudioClassifier
    private val recorder: AudioRecord
    private val executor = ScheduledThreadPoolExecutor(1)

    init {
        val options = AudioClassifier.AudioClassifierOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath(MODEL_PATH)
                    .build()
            )
            .setRunningMode(RunningMode.AUDIO_STREAM)
            .setScoreThreshold(0.01f)
            .setMaxResults(40)
            .setResultListener(::onResult)
            .setErrorListener { error -> listener.onError(error.message ?: "classifier error") }
            .build()

        classifier = AudioClassifier.createFromOptions(context, options)
        recorder = classifier.createAudioRecord(
            AudioFormat.CHANNEL_IN_DEFAULT,
            SAMPLE_RATE,
            BUFFER_SIZE_BYTES
        )
    }

    @SuppressLint("MissingPermission")
    fun start() {
        recorder.startRecording()
        executor.scheduleAtFixedRate(
            { classifyLatest() },
            0L,
            CLASSIFY_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        )
    }

    private fun classifyLatest() {
        try {
            val format = AudioDataFormat.create(recorder.format)
            val audioData = AudioData.create(format, SAMPLE_RATE)
            audioData.load(recorder)
            classifier.classifyAsync(audioData, SystemClock.uptimeMillis())
        } catch (t: Throwable) {
            listener.onError(t.message ?: "audio classification failed")
        }
    }

    private fun onResult(result: AudioClassifierResult) {
        val scores = result.classificationResults()
            .flatMap { it.classifications() }
            .flatMap { it.categories() }
            .asSequence()
            .filter { it.categoryName().isNotBlank() && it.score() >= 0.01f }
            .sortedByDescending { it.score() }
            .take(40)
            .map { LabelScore(it.categoryName(), it.score()) }
            .toList()
        listener.onScores(scores)
    }

    override fun close() {
        executor.shutdownNow()
        try { recorder.stop() } catch (_: Throwable) { }
        try { recorder.release() } catch (_: Throwable) { }
        try { classifier.close() } catch (_: Throwable) { }
    }

    companion object {
        const val MODEL_PATH = "yamnet.tflite"
        private const val SAMPLE_RATE = 16_000
        private const val EXPECTED_INPUT_LENGTH = 0.975f
        private const val REQUIRED_SAMPLES = (SAMPLE_RATE * EXPECTED_INPUT_LENGTH).toInt()
        private const val BUFFER_SIZE_BYTES = REQUIRED_SAMPLES * Float.SIZE_BYTES * 2
        private const val CLASSIFY_INTERVAL_MS = 480L
    }
}
