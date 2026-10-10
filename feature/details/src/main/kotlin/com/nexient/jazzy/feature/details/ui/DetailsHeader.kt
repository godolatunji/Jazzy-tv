package com.nexient.jazzy.feature.details.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.common.formatDuration
import com.nexient.jazzy.core.common.formatRuntime
import com.nexient.jazzy.core.designsystem.component.JazzyImage
import com.nexient.jazzy.core.designsystem.component.MaturityBadge
import com.nexient.jazzy.core.designsystem.component.PosterCard
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.TitleDetails
import com.nexient.jazzy.core.model.TitleRef
import com.nexient.jazzy.core.model.TmdbImages
import com.nexient.jazzy.feature.details.DetailsUiState
import java.util.Locale

/** Title, meta, overview, actions (DT-1, DT-2). There is no generic Play button: Play lives on source rows. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DetailsInfo(
    details: TitleDetails,
    state: DetailsUiState,
    onToggleMyList: () -> Unit,
    onToggleLike: () -> Unit,
    onTrailer: () -> Unit,
    onResumeBanner: (() -> Unit)?,
    modifier: Modifier = Modifier,
    compactOverview: Boolean = false,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (details.logoPath != null) {
            JazzyImage(
                TmdbImages.logo(details.logoPath), details.title,
                Modifier.heightIn(max = if (JazzyTheme.isTv) 110.dp else 90.dp).widthIn(max = 320.dp),
                fallbackText = details.title, contentScale = ContentScale.Fit,
            )
        } else {
            Text(details.title, style = MaterialTheme.typography.headlineMedium)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            details.year?.let { Meta(it.toString()) }
            if (details.mediaType == MediaType.MOVIE) formatRuntime(details.runtimeMinutes)?.let { Meta(it) }
            else details.numberOfSeasons?.let { Meta(if (it == 1) "1 season" else "$it seasons") }
            MaturityBadge(details.certification)
            if (details.summary.voteAverage > 0) Meta("★ " + String.format(Locale.US, "%.1f", details.summary.voteAverage))
        }
        if (details.genres.isNotEmpty()) Meta(details.genres.joinToString(" • ") { it.name })
        details.tagline?.let { Text(it, style = MaterialTheme.typography.titleSmall, color = JazzyColors.TextSecondary) }
        Text(
            details.summary.overview,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (compactOverview) 4 else 8,
            overflow = TextOverflow.Ellipsis,
        )
        if (details.cast.isNotEmpty()) {
            Text("Cast: " + details.cast.take(6).joinToString(", ") { it.name }, style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        val crew = if (details.mediaType == MediaType.MOVIE) details.directors else details.creators
        if (crew.isNotEmpty()) {
            Text((if (details.mediaType == MediaType.MOVIE) "Director: " else "Created by: ") + crew.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onToggleMyList) {
                Icon(if (state.inMyList) Icons.Filled.Check else Icons.Filled.Add, null)
                Text(" My List")
            }
            OutlinedButton(onClick = onToggleLike) {
                Icon(if (state.liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null, tint = if (state.liked) JazzyColors.Accent else JazzyColors.OnBackground)
                Text(if (state.liked) " Liked" else " Like")
            }
            if (details.trailerYoutubeKey != null) {
                OutlinedButton(onClick = onTrailer) { Icon(Icons.Filled.Movie, null); Text(" Trailer") }
            }
        }
        val progress = state.progress
        if (progress != null && onResumeBanner != null) {
            FilledTonalButton(onClick = onResumeBanner, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                val ep = progress.playable.label?.let { "$it · " }.orEmpty()
                Text(" ${ep}You stopped at ${formatDuration(progress.positionMs)}" + (progress.lastQuality?.let { " · $it (last used)" } ?: ""))
            }
        }
    }
}

@Composable
private fun Meta(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = JazzyColors.TextSecondary)
}

/** "More Like This" (DT-5). */
@Composable
internal fun MoreLikeThis(details: TitleDetails, onOpen: (TitleRef) -> Unit, modifier: Modifier = Modifier) {
    if (details.recommendations.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("More Like This", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = JazzyTheme.spacing.gutter))
        LazyRow(contentPadding = PaddingValues(horizontal = JazzyTheme.spacing.gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(details.recommendations.take(20), key = { "${it.tmdbId}:${it.mediaType}" }) { t ->
                PosterCard(t.title, t.posterPath, onClick = { onOpen(t.ref) })
            }
        }
    }
}

@Composable
internal fun CastRow(details: TitleDetails, modifier: Modifier = Modifier) {
    if (details.cast.isEmpty()) return
    LazyRow(modifier, contentPadding = PaddingValues(horizontal = JazzyTheme.spacing.gutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(details.cast, key = { it.id }) { c ->
            Column(Modifier.width(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                JazzyImage(TmdbImages.profile(c.profilePath), c.name, Modifier.size(64.dp).clip(CircleShape), fallbackText = c.name.take(1))
                Text(c.name, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
