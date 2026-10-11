package com.nexient.jazzy.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme

/**
 * Clickable that works with touch and D-pad. On TV a focused element scales up and gets a
 * [JazzyColors.Focus] outline (spec §5.2 "TV focus state"); on phones focus only shows the outline,
 * for keyboard users.
 *
 * Don't `clip` before this modifier: the clip would cut off the scaled outline. Pass [clipContent]
 * to clip what follows (images, backgrounds) to [shape] instead.
 */
@Composable
fun Modifier.jazzyClickable(
    shape: Shape = RoundedCornerShape(8.dp),
    focusScale: Float = 1.08f,
    role: Role = Role.Button,
    clipContent: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onFocused: ((Boolean) -> Unit)? = null,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    if (onFocused != null) LaunchedEffect(focused) { onFocused(focused) }
    // On TV the ring is the focus indication; the ripple's tinted focus layer would wash over artwork.
    val indication = if (JazzyTheme.isTv) null else ripple()
    return this
        .focusIndication(focused, shape, focusScale)
        .then(if (clipContent) Modifier.clip(shape) else Modifier)
        .combinedClickable(
            interactionSource = interaction,
            indication = indication,
            role = role,
            onLongClick = onLongClick,
            onClick = onClick,
        )
}

/**
 * The [jazzyClickable] focus treatment for Material components (buttons, chips, icon buttons) that
 * handle clicks themselves: pass it in their `modifier`. It follows focus of the component (or of
 * anything inside it), so it also works on a container of focusable children.
 */
@Composable
fun Modifier.focusRing(shape: Shape = RoundedCornerShape(50), focusScale: Float = 1.05f): Modifier {
    var focused by remember { mutableStateOf(false) }
    return this
        .onFocusChanged { focused = it.hasFocus }
        .focusIndication(focused, shape, focusScale)
}

/**
 * Scale plus outline. The outline is always drawn (transparent when unfocused), so focus only
 * changes its colour; nothing is added to or removed from the node chain as focus moves.
 */
@Composable
private fun Modifier.focusIndication(focused: Boolean, shape: Shape, focusScale: Float): Modifier {
    val tv = JazzyTheme.isTv
    val scale by animateFloatAsState(if (focused && tv) focusScale else 1f, label = "focusScale")
    val ring by animateColorAsState(if (focused) JazzyColors.Focus else Color.Transparent, label = "focusRing")
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .border(if (tv) 3.dp else 2.dp, ring, shape)
}
