package com.nexient.jazzy.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.nexient.jazzy.core.designsystem.theme.JazzyColors

/** Skeleton shimmer for loading rails and cards (HM-1). */
fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX",
    )
    background(
        Brush.linearGradient(
            colors = listOf(JazzyColors.Surface, JazzyColors.SurfaceVariant, JazzyColors.Surface),
            start = Offset(x, 0f),
            end = Offset(x + 400f, 400f),
        ),
    )
}

@Composable
fun ShimmerBox(modifier: Modifier) {
    androidx.compose.foundation.layout.Box(modifier.shimmer())
}
