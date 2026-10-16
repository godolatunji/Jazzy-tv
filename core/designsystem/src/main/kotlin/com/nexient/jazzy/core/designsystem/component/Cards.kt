package com.nexient.jazzy.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexient.jazzy.core.designsystem.R
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.TmdbImages

private val CardShape = RoundedCornerShape(8.dp)

/** 2:3 poster card with My List (✓) and Liked (♥) markers (spec §5.2 card styles). */
@Composable
fun PosterCard(
    title: String,
    posterPath: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    inMyList: Boolean = false,
    liked: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onFocused: ((Boolean) -> Unit)? = null,
) {
    val width = JazzyTheme.spacing.posterWidth
    Box(
        modifier
            .width(width)
            .aspectRatio(2f / 3f)
            .jazzyClickable(CardShape, clipContent = true, onLongClick = onLongClick, onFocused = onFocused, onClick = onClick),
    ) {
        JazzyImage(TmdbImages.poster(posterPath, JazzyTheme.isTv), title, Modifier.fillMaxSize(), fallbackText = title)
        if (inMyList || liked) {
            Row(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                if (inMyList) MarkerIcon { Icon(Icons.Filled.Check, stringResource(R.string.designsystem_card_in_my_list), Modifier.size(12.dp), tint = Color.White) }
                if (liked) MarkerIcon { Icon(Icons.Filled.Favorite, stringResource(R.string.designsystem_card_liked), Modifier.size(12.dp), tint = JazzyColors.Accent) }
            }
        }
    }
}

/** Poster-sized "See more" card that ends a rail and opens the rail's own page. */
@Composable
fun SeeMoreCard(onClick: () -> Unit, modifier: Modifier = Modifier, onFocused: ((Boolean) -> Unit)? = null) {
    Box(
        modifier
            .width(JazzyTheme.spacing.posterWidth)
            .aspectRatio(2f / 3f)
            .jazzyClickable(CardShape, clipContent = true, onFocused = onFocused, onClick = onClick)
            .background(JazzyColors.SurfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(48.dp).background(JazzyColors.Scrim, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White)
            }
            Text(
                stringResource(R.string.designsystem_card_see_more),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun MarkerIcon(content: @Composable () -> Unit) {
    Box(Modifier.padding(start = 2.dp).size(20.dp).background(JazzyColors.Scrim, CircleShape), contentAlignment = Alignment.Center) { content() }
}

/** 16:9 Continue Watching card with progress and a ⋮ menu (spec §5.2). */
@Composable
fun BackdropProgressCard(
    title: String,
    subtitle: String?,
    backdropPath: String?,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMenu: (() -> Unit)? = null,
    onFocused: ((Boolean) -> Unit)? = null,
) {
    val width = JazzyTheme.spacing.backdropWidth
    Column(modifier.width(width)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .jazzyClickable(CardShape, clipContent = true, onLongClick = onMenu, onFocused = onFocused, onClick = onClick),
        ) {
            JazzyImage(TmdbImages.backdrop(backdropPath, JazzyTheme.isTv), title, Modifier.fillMaxSize(), fallbackText = title)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, JazzyColors.Scrim))))
            Icon(
                Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White,
                modifier = Modifier.align(Alignment.Center).size(40.dp).background(JazzyColors.Scrim, CircleShape).padding(6.dp),
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp),
                color = JazzyColors.Accent,
                trackColor = Color.White.copy(alpha = 0.25f),
                drawStopIndicator = {},
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(top = 6.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextSecondary, maxLines = 1)
            }
            if (onMenu != null && !JazzyTheme.isTv) {
                IconButton(onClick = onMenu) { Icon(Icons.Filled.MoreVert, stringResource(R.string.designsystem_card_more_options, title), tint = JazzyColors.TextSecondary) }
            }
        }
    }
}

/** Top 10 card: a large outlined rank numeral beside the poster (spec §5.2). */
@Composable
fun TopTenCard(
    rank: Int,
    title: String,
    posterPath: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: ((Boolean) -> Unit)? = null,
) {
    val poster = JazzyTheme.spacing.posterWidth
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            text = rank.toString(),
            style = TextStyle(
                fontSize = if (JazzyTheme.isTv) 130.sp else 100.sp,
                fontWeight = FontWeight.Black,
                color = JazzyColors.Background,
                drawStyle = Stroke(width = 6f),
            ),
            modifier = Modifier.padding(end = 0.dp),
        )
        PosterCard(title, posterPath, onClick, Modifier.width(poster * 0.9f), onFocused = onFocused)
    }
}

/** Landscape still card used in person filmography and episode lists. */
@Composable
fun StillCard(url: String?, label: String, modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(16f / 9f).clip(CardShape)) {
        JazzyImage(url, label, Modifier.fillMaxSize(), fallbackText = label)
    }
}
