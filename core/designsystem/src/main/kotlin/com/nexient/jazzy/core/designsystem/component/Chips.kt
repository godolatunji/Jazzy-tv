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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme

private val ChipShape = RoundedCornerShape(50)

/**
 * A filter / choice chip that works with touch and D-pad: on TV a focused chip scales up and gets
 * the focus outline (via [jazzyClickable]); a selected chip is filled white.
 */
@Composable
fun JazzyChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val tv = JazzyTheme.isTv
    val content = if (selected) JazzyColors.Background else JazzyColors.OnBackground
    Row(
        modifier
            .semantics { this.selected = selected }
            .jazzyClickable(ChipShape, focusScale = 1.06f, role = Role.Tab, onLongClick = onLongClick, onClick = onClick)
            .clip(ChipShape)
            .background(if (selected) JazzyColors.OnBackground else JazzyColors.SurfaceVariant)
            .then(if (selected) Modifier else Modifier.border(1.dp, JazzyColors.Outline, ChipShape))
            .heightIn(min = if (tv) 40.dp else 32.dp)
            .padding(horizontal = if (tv) 16.dp else 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leadingIcon != null) Icon(leadingIcon, null, tint = content, modifier = Modifier.size(if (tv) 20.dp else 16.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (trailingIcon != null) Icon(trailingIcon, null, tint = content, modifier = Modifier.size(if (tv) 20.dp else 16.dp))
    }
}
