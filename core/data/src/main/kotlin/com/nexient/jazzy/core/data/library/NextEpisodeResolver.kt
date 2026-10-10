package com.nexient.jazzy.core.data.library

import com.nexient.jazzy.core.data.catalog.TitleRepository
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.PlayableRef
import com.nexient.jazzy.core.model.TitleRef
import com.nexient.jazzy.core.model.WatchProgress
import javax.inject.Inject

/** Finds the episode after a finished one (RS-3, PL-10), crossing into the next season. */
class NextEpisodeResolver @Inject constructor(private val titles: TitleRepository) {
    suspend fun next(progress: WatchProgress, language: String): Pair<PlayableRef, String?>? =
        next(progress.playable, language)

    suspend fun next(current: PlayableRef, language: String): Pair<PlayableRef, String?>? = runCatching {
        if (!current.isEpisode) return null
        val season = titles.seasonOnce(current.tmdbId, current.season, language)
        season.episodes.firstOrNull { it.episodeNumber == current.episode + 1 }?.let {
            return PlayableRef(current.tmdbId, MediaType.TV, it.seasonNumber, it.episodeNumber) to it.name
        }
        val details = titles.detailsOnce(TitleRef(current.tmdbId, MediaType.TV), language)
        val nextSeason = details.seasons.map { it.seasonNumber }.filter { it > current.season && it != 0 }.minOrNull() ?: return null
        val first = titles.seasonOnce(current.tmdbId, nextSeason, language).episodes.minByOrNull { it.episodeNumber } ?: return null
        PlayableRef(current.tmdbId, MediaType.TV, first.seasonNumber, first.episodeNumber) to first.name
    }.getOrNull()
}
