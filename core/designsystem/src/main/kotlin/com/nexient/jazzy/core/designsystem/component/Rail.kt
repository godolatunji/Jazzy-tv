package com.nexient.jazzy.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.R
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme

/** A titled horizontal rail (spec §5.2). With [onTitleClick] the title opens the rail's own page. */
@Composable
fun Rail(
    title: String,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    trailing: (@Composable () -> Unit)? = null,
    onTitleClick: (() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val spacing = JazzyTheme.spacing
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = spacing.gutter).padding(bottom = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onTitleClick == null) {
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
            } else {
                Row(Modifier.weight(1f)) {
                    // Pulled back by its inner padding so the text lines up with plain rail titles.
                    Row(
                        Modifier
                            .offset(x = (-4).dp)
                            .jazzyClickable(RoundedCornerShape(8.dp), focusScale = 1.03f, onClick = onTitleClick)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.designsystem_rail_see_all), tint = JazzyColors.TextSecondary)
                    }
                }
            }
            trailing?.invoke()
        }
        LazyRow(
            state = state,
            contentPadding = PaddingValues(horizontal = spacing.gutter, vertical = if (JazzyTheme.isTv) 12.dp else 0.dp),
            horizontalArrangement = Arrangement.spacedBy(if (JazzyTheme.isTv) 16.dp else 8.dp),
            content = content,
        )
    }
}

/** Skeleton placeholder shown while a rail has no cached data (HM-1). */
@Composable
fun RailSkeleton(modifier: Modifier = Modifier, backdrop: Boolean = false) {
    val spacing = JazzyTheme.spacing
    Column(modifier.fillMaxWidth().padding(vertical = spacing.sm)) {
        ShimmerBox(Modifier.padding(horizontal = spacing.gutter).width(140.dp).height(18.dp).clip(RoundedCornerShape(4.dp)))
        Row(Modifier.padding(start = spacing.gutter, top = spacing.sm), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(6) {
                ShimmerBox(
                    Modifier
                        .width(if (backdrop) spacing.backdropWidth else spacing.posterWidth)
                        .aspectRatio(if (backdrop) 16f / 9f else 2f / 3f)
                        .clip(RoundedCornerShape(8.dp)),
                )
            }
        }
    }
}

/** Inline rail error with retry (HM-1). */
@Composable
fun RailError(title: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = JazzyTheme.spacing
    Column(modifier.fillMaxWidth().padding(horizontal = spacing.gutter, vertical = spacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.designsystem_rail_error), color = JazzyColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onRetry) { Text(stringResource(R.string.designsystem_common_retry)) }
        }
    }
}
