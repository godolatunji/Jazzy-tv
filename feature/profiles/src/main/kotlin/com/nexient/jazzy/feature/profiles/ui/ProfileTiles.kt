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
import androidx.compose.ui.res.stringResource
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
import com.nexient.jazzy.feature.profiles.R

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
    val kidsSuffix = stringResource(R.string.profiles_picker_tile_a11y_kids)
    val pinSuffix = stringResource(R.string.profiles_picker_tile_a11y_pin)
    val editSuffix = stringResource(R.string.profiles_picker_tile_a11y_edit)
    val description = buildString {
        append(profile.name)
        if (profile.isKids) append(kidsSuffix)
        if (profile.hasPin) append(pinSuffix)
        if (editMode) append(editSuffix)
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
            if (profile.isKids) Badge(stringResource(R.string.profiles_picker_kids_badge), tone = BadgeTone.INFO)
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
            Icon(Icons.Filled.Add, stringResource(R.string.profiles_picker_add_profile), tint = JazzyColors.TextSecondary, modifier = Modifier.size(size * 0.4f))
        }
        Text(
            stringResource(R.string.profiles_picker_add_profile),
            style = MaterialTheme.typography.titleSmall,
            color = if (focused) JazzyColors.OnBackground else JazzyColors.TextSecondary,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}

internal fun Modifier.withFocusRequester(requester: FocusRequester?): Modifier =
    if (requester != null) focusRequester(requester) else this
