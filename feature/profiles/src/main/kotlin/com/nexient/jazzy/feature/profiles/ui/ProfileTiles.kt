package com.nexient.jazzy.feature.profiles.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.component.AvatarTile
import com.nexient.jazzy.core.designsystem.component.Badge
import com.nexient.jazzy.core.designsystem.component.BadgeTone
import com.nexient.jazzy.core.designsystem.component.jazzyClickable
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.model.Profile

/** Avatar + name; in manage mode a pencil overlay shows that selecting edits the profile. */
@Composable
internal fun ProfileTile(
    profile: Profile,
    editMode: Boolean,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(size / 8)
    val description = buildString {
        append(profile.name)
        if (profile.isKids) append(", Kids profile")
        if (profile.hasPin) append(", PIN protected")
        if (editMode) append(", edit")
    }
    Column(
        modifier.width(size + 16.dp).semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.withFocusRequester(focusRequester).jazzyClickable(shape, onFocused = { focused = it }, onClick = onClick)) {
            AvatarTile(profile.avatarId, size = size)
            if (editMode) {
                Box(Modifier.size(size).background(Color.Black.copy(alpha = 0.55f), shape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Edit, null, tint = Color.White, modifier = Modifier.size(size * 0.3f))
                }
            }
        }
        Text(
            profile.name,
            style = MaterialTheme.typography.titleSmall,
            color = if (focused) JazzyColors.OnBackground else JazzyColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (profile.isKids) Badge("KIDS", tone = BadgeTone.INFO)
            if (profile.hasPin) Icon(Icons.Filled.Lock, null, tint = JazzyColors.TextTertiary, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
internal fun AddProfileTile(size: Dp, onClick: () -> Unit, modifier: Modifier = Modifier, focusRequester: FocusRequester? = null) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(size / 8)
    Column(
        modifier.width(size + 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(size)
                .border(2.dp, JazzyColors.Outline, shape)
                .withFocusRequester(focusRequester)
                .jazzyClickable(shape, onFocused = { focused = it }, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, "Add profile", tint = JazzyColors.TextSecondary, modifier = Modifier.size(size * 0.4f))
        }
        Text(
            "Add profile",
            style = MaterialTheme.typography.titleSmall,
            color = if (focused) JazzyColors.OnBackground else JazzyColors.TextSecondary,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}

internal fun Modifier.withFocusRequester(requester: FocusRequester?): Modifier =
    if (requester != null) focusRequester(requester) else this
