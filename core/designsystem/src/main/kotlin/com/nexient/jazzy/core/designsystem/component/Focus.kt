package com.nexient.jazzy.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme

/**
 * Clickable that works with touch and D-pad. On TV a focused element scales up and gets an
 * outline (spec §5.2 "TV focus state"); on phones focus only shows an outline for keyboard users.
 */
@Composable
fun Modifier.jazzyClickable(
    shape: Shape = RoundedCornerShape(8.dp),
    focusScale: Float = 1.08f,
    role: Role = Role.Button,
    onLongClick: (() -> Unit)? = null,
    onFocused: ((Boolean) -> Unit)? = null,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val tv = JazzyTheme.isTv
    val scale by animateFloatAsState(if (focused && tv) focusScale else 1f, label = "focusScale")
    if (onFocused != null) androidx.compose.runtime.LaunchedEffect(focused) { onFocused(focused) }
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .then(if (focused) Modifier.border(if (tv) 3.dp else 2.dp, JazzyColors.Focus, shape) else Modifier)
        .combinedClickable(
            interactionSource = interaction,
            indication = ripple(),
            role = role,
            onLongClick = onLongClick,
            onClick = onClick,
        )
}
