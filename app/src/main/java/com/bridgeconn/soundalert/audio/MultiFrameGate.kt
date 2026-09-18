package com.bridgeconn.soundalert.audio

import com.bridgeconn.soundalert.AlertKind

class MultiFrameGate(private val windowSize: Int = 3, private val requiredHits: Int = 2) {
    private val history = mutableListOf<DetectionPolicy.Candidate?>()

    fun push(candidate: DetectionPolicy.Candidate?): DetectionPolicy.Candidate? {
        history.add(candidate)
        while (history.size > windowSize) history.removeAt(0)

        var bestKind = AlertKind.NONE
        var bestCount = 0
        var bestAverage = 0f

        for (kind in listOf(AlertKind.SIREN, AlertKind.HORN, AlertKind.DOOR)) {
            val matches = history.filterNotNull().filter { it.kind == kind }
            if (matches.size >= requiredHits) {
                val average = matches.map { it.score }.average().toFloat()
                if (matches.size > bestCount || (matches.size == bestCount && average > bestAverage)) {
                    bestKind = kind
                    bestCount = matches.size
                    bestAverage = average
                }
            }
        }

        return if (bestKind == AlertKind.NONE) null else DetectionPolicy.Candidate(bestKind, bestAverage)
    }

    fun reset() = history.clear()
}
