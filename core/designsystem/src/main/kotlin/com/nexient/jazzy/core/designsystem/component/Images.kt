package com.nexient.jazzy.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.nexient.jazzy.core.designsystem.theme.JazzyColors

/**
 * Network image with a shimmer placeholder and a title fallback when missing/failed.
 *
 * Uses [AsyncImage] (not SubcomposeAsyncImage): subcomposition per image is markedly heavier and,
 * on a Home full of rails, was a real scroll cost. The shimmer/fallback are a cheap sibling overlay
 * driven by [onState] instead of subcomposed loading/error slots.
 */
@Composable
fun JazzyImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallbackText: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (url == null) {
        Fallback(fallbackText, modifier)
        return
    }
    var state by remember(url) { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
    Box(modifier) {
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
            onState = { state = it },
        )
        when (state) {
            is AsyncImagePainter.State.Loading, is AsyncImagePainter.State.Empty -> Box(Modifier.fillMaxSize().shimmer())
            is AsyncImagePainter.State.Error -> Fallback(fallbackText, Modifier.fillMaxSize())
            is AsyncImagePainter.State.Success -> Unit
        }
    }
}

@Composable
private fun Fallback(text: String?, modifier: Modifier) {
    Box(modifier.background(JazzyColors.SurfaceVariant), contentAlignment = Alignment.Center) {
        if (text != null) {
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = JazzyColors.TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}
