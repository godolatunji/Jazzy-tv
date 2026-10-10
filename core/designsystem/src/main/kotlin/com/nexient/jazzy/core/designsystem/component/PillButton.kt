package com.nexient.jazzy.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme

enum class PillButtonStyle { PRIMARY, SECONDARY, ACCENT }

/**
 * A button with the shared D-pad focus treatment ([jazzyClickable]): Material buttons only show a
 * faint state layer when focused, which is too subtle at 10 feet.
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: PillButtonStyle = PillButtonStyle.SECONDARY,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(50)
    val (container, content) = when (style) {
        PillButtonStyle.PRIMARY -> Color.White to Color.Black
        PillButtonStyle.ACCENT -> JazzyColors.Accent to Color.White
        PillButtonStyle.SECONDARY -> JazzyColors.SurfaceVariant to JazzyColors.OnBackground
    }
    val alpha = if (enabled) 1f else 0.4f
    Row(
        modifier
            .then(if (enabled) Modifier.jazzyClickable(shape, focusScale = 1.05f, onClick = onClick) else Modifier)
            .background(container.copy(alpha = container.alpha * alpha), shape)
            .then(if (style == PillButtonStyle.SECONDARY) Modifier.border(1.dp, JazzyColors.Outline, shape) else Modifier)
            .heightIn(min = if (JazzyTheme.isTv) 48.dp else 40.dp)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, null, tint = content.copy(alpha = alpha), modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = content.copy(alpha = alpha))
    }
}
