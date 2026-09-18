package com.bridgeconn.soundalert.audio

import com.bridgeconn.soundalert.AlertMode
import com.bridgeconn.soundalert.SoundLabel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Small, fully offline first-pass environmental sound classifier.
 *
 * It does NOT claim ML-grade accuracy. It intentionally isolates detection behind one class so
 * a trained TFLite/MediaPipe model can replace it later without changing the UI/service/haptics.
 */
class HeuristicSoundClassifier(
    private val sampleRate: Int = 16_000
) {
    data class Classification(
        val label: SoundLabel,
        val confidence: Float,
        val detail: String
    )

    private var hornStreak = 0
    private var bellStreak = 0

    fun reset() {
        hornStreak = 0
        bellStreak = 0
    }

    fun analyze(
        pcm: ShortArray,
        size: Int,
        mode: AlertMode,
        sensitivity: Float
    ): Classification? {
        if (size < 256) return null
        val s = sensitivity.coerceIn(0.30f, 0.95f)
        val samples = centeredSamples(pcm, size)
        val features = extractFeatures(samples)

        if (features.rmsDb < -58.0) {
            hornStreak = 0
            bellStreak = 0
            return null
        }

        val threshold = (0.84 - (s * 0.40)).coerceIn(0.44, 0.72)

        return when (mode) {
            AlertMode.ROAD -> classifyHorn(samples, features, threshold)
            AlertMode.HOME -> classifyHome(samples, features, threshold)
        }
    }

    private fun classifyHorn(
        samples: DoubleArray,
        f: Features,
        threshold: Double
    ): Classification? {
        val lowTone = maxToneAmplitude(samples, doubleArrayOf(300.0, 350.0, 400.0, 450.0, 500.0, 550.0, 600.0))
        val harmonic = maxToneAmplitude(samples, doubleArrayOf(700.0, 800.0, 900.0, 1000.0, 1100.0, 1200.0))

        val tonality = normalizedTonality(lowTone, f.rms)
        val harmonicTonality = normalizedTonality(harmonic, f.rms)
        val loudness = normalize(f.rmsDb, -48.0, -14.0)
        val zcrFit = when {
            f.zeroCrossingRate in 0.020..0.135 -> 1.0
            f.zeroCrossingRate in 0.010..0.180 -> 0.65
            else -> 0.25
        }

        var score = 0.46 * loudness + 0.42 * tonality + 0.07 * harmonicTonality + 0.05 * zcrFit
        if (f.crestFactor > 8.0) score *= 0.72 // sharp impacts are less horn-like
        score = score.coerceIn(0.0, 1.0)

        hornStreak = if (score >= threshold) (hornStreak + 1).coerceAtMost(4) else (hornStreak - 1).coerceAtLeast(0)
        bellStreak = 0

        if (hornStreak >= 2) {
            hornStreak = 0
            return Classification(
                label = SoundLabel.HORN,
                confidence = score.toFloat(),
                detail = "sustained low-frequency tonal sound"
            )
        }
        return null
    }

    private fun classifyHome(
        samples: DoubleArray,
        f: Features,
        threshold: Double
    ): Classification? {
        val highTone = maxToneAmplitude(samples, doubleArrayOf(650.0, 800.0, 1000.0, 1200.0, 1500.0, 1800.0, 2200.0))
        val highTonality = normalizedTonality(highTone, f.rms)
        val loudness = normalize(f.rmsDb, -52.0, -14.0)
        val crest = normalize(f.crestFactor, 2.5, 9.0)
        val peak = normalize(f.peak, 0.12, 0.90)
        val zcrFit = normalize(f.zeroCrossingRate, 0.025, 0.24)

        val chimeScore = (0.43 * loudness + 0.50 * highTonality + 0.07 * zcrFit).coerceIn(0.0, 1.0)
        val knockScore = (0.28 * loudness + 0.40 * crest + 0.32 * peak).coerceIn(0.0, 1.0)
        val score = max(chimeScore, knockScore)

        val chimeCandidate = chimeScore >= threshold
        val strongKnockCandidate = knockScore >= (threshold + 0.10).coerceAtMost(0.86)
        bellStreak = if (chimeCandidate) (bellStreak + 1).coerceAtMost(4) else (bellStreak - 1).coerceAtLeast(0)
        hornStreak = 0

        if (bellStreak >= 2 || strongKnockCandidate) {
            bellStreak = 0
            val reason = if (chimeScore >= knockScore) "doorbell-like high tonal sound" else "knock-like transient"
            return Classification(
                label = SoundLabel.DOORBELL_OR_KNOCK,
                confidence = score.toFloat(),
                detail = reason
            )
        }
        return null
    }

    private data class Features(
        val rms: Double,
        val rmsDb: Double,
        val peak: Double,
        val zeroCrossingRate: Double,
        val crestFactor: Double
    )

    private fun centeredSamples(pcm: ShortArray, size: Int): DoubleArray {
        var mean = 0.0
        for (i in 0 until size) mean += pcm[i].toDouble() / 32768.0
        mean /= size
        return DoubleArray(size) { i -> (pcm[i].toDouble() / 32768.0) - mean }
    }

    private fun extractFeatures(samples: DoubleArray): Features {
        var sumSq = 0.0
        var peak = 0.0
        var crossings = 0
        var previous = samples[0]

        for (i in samples.indices) {
            val x = samples[i]
            sumSq += x * x
            peak = max(peak, kotlin.math.abs(x))
            if (i > 0 && (x >= 0.0) != (previous >= 0.0)) crossings++
            previous = x
        }

        val rms = sqrt(sumSq / samples.size).coerceAtLeast(1e-9)
        val db = 20.0 * (ln(rms) / ln(10.0))
        val zcr = crossings.toDouble() / (samples.size - 1).coerceAtLeast(1)
        val crest = peak / rms
        return Features(rms, db, peak, zcr, crest)
    }

    private fun maxToneAmplitude(samples: DoubleArray, frequencies: DoubleArray): Double {
        var best = 0.0
        for (frequency in frequencies) {
            best = max(best, goertzelAmplitude(samples, frequency))
        }
        return best
    }

    private fun goertzelAmplitude(samples: DoubleArray, frequency: Double): Double {
        val omega = 2.0 * PI * frequency / sampleRate
        val coeff = 2.0 * cos(omega)
        var sPrev = 0.0
        var sPrev2 = 0.0
        for (sample in samples) {
            val s = sample + coeff * sPrev - sPrev2
            sPrev2 = sPrev
            sPrev = s
        }
        val power = (sPrev2 * sPrev2 + sPrev * sPrev - coeff * sPrev * sPrev2).coerceAtLeast(0.0)
        return 2.0 * sqrt(power) / samples.size
    }

    private fun normalizedTonality(amplitude: Double, rms: Double): Double {
        // A clean sine wave approaches amplitude/rms ~= sqrt(2).
        return (amplitude / (rms * 1.41421356237 + 1e-9)).coerceIn(0.0, 1.0)
    }

    private fun normalize(value: Double, low: Double, high: Double): Double {
        if (high <= low) return 0.0
        return ((value - low) / (high - low)).coerceIn(0.0, 1.0)
    }
}
