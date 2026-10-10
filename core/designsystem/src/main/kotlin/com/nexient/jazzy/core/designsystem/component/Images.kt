package com.nexient.jazzy.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.nexient.jazzy.core.designsystem.theme.JazzyColors

/** Network image with a dark placeholder and a title fallback when the image is missing. */
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
    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        loading = { Box(Modifier.fillMaxSize().shimmer()) },
        error = { Fallback(fallbackText, Modifier.fillMaxSize()) },
    )
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
