package com.nexient.jazzy.core.designsystem.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.TmdbImages

/** What the hero billboard shows (spec §5.2). */
data class HeroContent(
    val key: String,
    val title: String,
    val overview: String,
    val backdropPath: String?,
    val logoPath: String?,
    val genres: List<String>,
    val certification: String?,
    val meta: String?,
)

/** Full-bleed backdrop with gradient scrims; the backdrop cross-fades when [content] changes. */
@Composable
fun HeroBackdrop(content: HeroContent?, modifier: Modifier = Modifier) {
    Box(modifier) {
        Crossfade(content?.backdropPath, label = "heroBackdrop") { path ->
            JazzyImage(TmdbImages.backdrop(path, tv = true), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, JazzyColors.Background), startY = 200f)))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(JazzyColors.Background.copy(alpha = 0.85f), Color.Transparent))))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeroText(content: HeroContent, modifier: Modifier = Modifier, actions: @Composable () -> Unit) {
    val tv = JazzyTheme.isTv
    Column(modifier.widthIn(max = if (tv) 560.dp else 600.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (content.logoPath != null) {
            JazzyImage(
                TmdbImages.logo(content.logoPath), content.title,
                Modifier.heightIn(max = if (tv) 120.dp else 96.dp).widthIn(max = if (tv) 420.dp else 300.dp),
                fallbackText = content.title, contentScale = ContentScale.Fit,
            )
        } else {
            Text(content.title, style = MaterialTheme.typography.displaySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            content.certification?.let { MaturityBadge(it) }
            content.meta?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = JazzyColors.TextSecondary) }
            content.genres.take(3).forEach { Text("• $it", style = MaterialTheme.typography.labelLarge, color = JazzyColors.TextSecondary) }
        }
        if (content.overview.isNotBlank()) {
            Text(content.overview, style = MaterialTheme.typography.bodyMedium, color = JazzyColors.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { actions() }
    }
}

@Composable
fun BoxScope.BottomFade(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, JazzyColors.Background))).padding(24.dp))
}
