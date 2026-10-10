package com.nexient.jazzy.tv

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.tvprovider.media.tv.PreviewChannelHelper
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.WatchNextProgram
import com.nexient.jazzy.FormFactors
import com.nexient.jazzy.core.common.ApplicationScope
import com.nexient.jazzy.core.data.library.LibraryRepository
import com.nexient.jazzy.core.data.library.NextEpisodeResolver
import com.nexient.jazzy.core.data.profile.ProfileRepository
import com.nexient.jazzy.core.model.ContinueWatchingItem
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.TmdbImages
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Publishes the active profile's Continue Watching to the Google TV "Watch Next" row (spec §5.10,
 * plan E14-10). Items deep-link back to Details with the Sources list focused.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Singleton
class WatchNextPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profiles: ProfileRepository,
    private val library: LibraryRepository,
    private val nextEpisodes: NextEpisodeResolver,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        if (!FormFactors.isTv(context)) return
        scope.launch(Dispatchers.IO) {
            profiles.activeProfile.filterNotNull()
                .flatMapLatest { p -> library.continueWatching(p.id) { nextEpisodes.next(it, p.language) } }
                .debounce(2_000)
                .collect { items -> runCatching { publish(items) }.onFailure { Log.w(TAG, "Watch Next update failed", it) } }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun publish(items: List<ContinueWatchingItem>) {
        val helper = PreviewChannelHelper(context)
        // Replace everything we published before (queries only return this app's rows).
        context.contentResolver.query(TvContractCompat.WatchNextPrograms.CONTENT_URI, arrayOf(TvContractCompat.WatchNextPrograms._ID), null, null, null)?.use { c ->
            while (c.moveToNext()) {
                context.contentResolver.delete(ContentUris.withAppendedId(TvContractCompat.WatchNextPrograms.CONTENT_URI, c.getLong(0)), null, null)
            }
        }
        items.take(10).forEach { item ->
            val p = item.progress.playable
            val isEpisode = p.isEpisode
            val builder = WatchNextProgram.Builder()
                .setType(if (isEpisode) TvContractCompat.WatchNextPrograms.TYPE_TV_EPISODE else TvContractCompat.WatchNextPrograms.TYPE_MOVIE)
                .setWatchNextType(
                    if (item.progress.positionMs > 0) TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE
                    else TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_NEXT,
                )
                .setLastEngagementTimeUtcMillis(item.progress.updatedAt)
                .setLastPlaybackPositionMillis(item.progress.positionMs.toInt())
                .setDurationMillis(item.progress.durationMs.toInt())
                .setTitle(item.snapshot.title)
                .setInternalProviderId("${item.snapshot.mediaType.apiValue}:${item.snapshot.tmdbId}")
                .setIntentUri(deepLink(item))
            TmdbImages.backdrop(item.snapshot.backdropPath, tv = true)?.let { builder.setPosterArtUri(Uri.parse(it)).setPosterArtAspectRatio(TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9) }
            if (isEpisode) {
                builder.setSeasonNumber(p.season).setEpisodeNumber(p.episode)
                item.episodeTitle?.let(builder::setEpisodeTitle)
            }
            helper.publishWatchNextProgram(builder.build())
        }
    }

    private fun deepLink(item: ContinueWatchingItem): Uri {
        val p = item.progress.playable
        return Uri.Builder().scheme("jazzy").authority("title")
            .appendPath(if (item.snapshot.mediaType == MediaType.TV) "tv" else "movie")
            .appendPath(item.snapshot.tmdbId.toString())
            .apply { if (p.isEpisode) appendQueryParameter("s", p.season.toString()).appendQueryParameter("e", p.episode.toString()) }
            .build()
    }

    private companion object {
        const val TAG = "WatchNext"
    }
}
