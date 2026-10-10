package com.nexient.jazzy.core.recommendations

import com.nexient.jazzy.core.model.MediaType
import kotlin.math.pow
import kotlin.math.sqrt

/** Everything the scorer needs to know about one title the profile interacted with. */
data class TitleSignal(
    val mediaType: MediaType,
    val genreIds: List<Int>,
    /** 0..1, or null when never played. For shows: average across watched episodes. */
    val completion: Float?,
    val daysSinceLastPlayed: Double?,
    val liked: Boolean = false,
    val daysSinceLiked: Double? = null,
    val inMyList: Boolean = false,
    val daysSinceAddedToList: Double? = null,
)

/**
 * Genre affinity scoring from spec §5.7. Pure and deterministic; normalised to 0..1.
 */
object AffinityCalculator {
    private const val PLAY_HALF_LIFE_DAYS = 30.0
    private const val LIKE_HALF_LIFE_DAYS = 90.0
    private const val LIST_HALF_LIFE_DAYS = 60.0
    private const val LIKE_WEIGHT = 1.5
    private const val LIST_WEIGHT = 0.5
    private const val SEED_WEIGHT = 0.8
    private const val SEED_FADE_SESSIONS = 10

    fun engagement(completion: Float): Double = when {
        completion >= 0.90f -> 1.0
        completion >= 0.50f -> 0.7
        completion >= 0.10f -> 0.4
        else -> 0.1
    }

    fun decay(days: Double, halfLife: Double): Double = 0.5.pow(days.coerceAtLeast(0.0) / halfLife)

    fun weight(signal: TitleSignal): Double {
        var w = 0.0
        if (signal.completion != null && signal.daysSinceLastPlayed != null) {
            w += engagement(signal.completion) * decay(signal.daysSinceLastPlayed, PLAY_HALF_LIFE_DAYS)
        }
        if (signal.liked) {
            w += LIKE_WEIGHT * decay(signal.daysSinceLiked ?: 0.0, LIKE_HALF_LIFE_DAYS)
        }
        if (signal.inMyList && signal.completion == null) {
            w += LIST_WEIGHT * decay(signal.daysSinceAddedToList ?: 0.0, LIST_HALF_LIFE_DAYS)
        }
        return w
    }

    /**
     * @param seeds genres picked at profile creation (PR-3)
     * @param sessionCount qualifying playback sessions so far; seeds fade out over the first 10
     * @return unified genre → score in 0..1, highest first; empty when there's no signal at all
     */
    fun compute(signals: List<TitleSignal>, seeds: Set<UnifiedGenre>, sessionCount: Int): List<Pair<UnifiedGenre, Double>> {
        val raw = mutableMapOf<UnifiedGenre, Double>()
        for (signal in signals) {
            val w = weight(signal)
            if (w <= 0.0) continue
            val genres = UnifiedGenre.of(signal.mediaType, signal.genreIds)
            if (genres.isEmpty()) continue
            val share = w / sqrt(genres.size.toDouble())
            genres.forEach { raw[it] = (raw[it] ?: 0.0) + share }
        }
        val seedFactor = (1.0 - sessionCount.toDouble() / SEED_FADE_SESSIONS).coerceAtLeast(0.0)
        if (seedFactor > 0) seeds.forEach { raw[it] = (raw[it] ?: 0.0) + SEED_WEIGHT * seedFactor }
        val max = raw.values.maxOrNull() ?: return emptyList()
        if (max <= 0.0) return emptyList()
        return raw.map { (g, s) -> g to s / max }.sortedWith(compareByDescending<Pair<UnifiedGenre, Double>> { it.second }.thenBy { it.first.ordinal })
    }
}
