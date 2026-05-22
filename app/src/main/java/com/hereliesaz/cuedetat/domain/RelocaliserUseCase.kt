package com.hereliesaz.cuedetat.domain

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class RelocaliserUseCase @Inject constructor() {

    fun validateHistograms(
        saved: Map<PocketId, List<Float>>,
        current: Map<PocketId, List<Float>>
    ): Boolean {
        if (saved.isEmpty()) return true
        var matches = 0
        for ((id, savedHist) in saved) {
            val currentHist = current[id] ?: continue
            if (bhattacharyyaSimilarity(savedHist, currentHist) > MATCH_THRESHOLD) matches++
        }
        return matches >= REQUIRED_MATCHES
    }

    fun bhattacharyyaSimilarity(a: List<Float>, b: List<Float>): Float {
        if (a.size != b.size || a.isEmpty()) return 0f
        return a.zip(b).sumOf { (ai, bi) -> sqrt((ai * bi).toDouble()) }.toFloat()
    }

    companion object {
        private const val MATCH_THRESHOLD = 0.7f
        private const val REQUIRED_MATCHES = 4
    }
}
