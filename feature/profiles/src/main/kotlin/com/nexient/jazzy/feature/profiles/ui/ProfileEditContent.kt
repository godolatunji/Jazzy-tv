package com.nexient.jazzy.feature.profiles.ui

import com.nexient.jazzy.core.designsystem.text.labelRes
import com.nexient.jazzy.core.designsystem.text.genreLabelRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.component.AvatarTile
import com.nexient.jazzy.core.designsystem.component.PillButton
import com.nexient.jazzy.core.designsystem.component.PillButtonStyle
import com.nexient.jazzy.core.designsystem.component.SettingsActionRow
import com.nexient.jazzy.core.designsystem.component.SettingsSectionHeader
import com.nexient.jazzy.core.designsystem.component.SettingsSwitchRow
import com.nexient.jazzy.core.designsystem.component.ToggleChipGroup
import com.nexient.jazzy.core.designsystem.component.jazzyClickable
import com.nexient.jazzy.core.designsystem.text.asString
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.AVATAR_COUNT
import com.nexient.jazzy.core.recommendations.UnifiedGenre
import com.nexient.jazzy.feature.profiles.EditDialog
import com.nexient.jazzy.feature.profiles.ProfileEditUiState
import com.nexient.jazzy.feature.profiles.ProfileEditViewModel
import com.nexient.jazzy.feature.profiles.R
import com.nexient.jazzy.feature.profiles.languageLabel

@Composable
private fun ProfileEditUiState.title() = stringResource(
    when {
        firstRun -> R.string.profiles_edit_title_first_run
        isNew -> R.string.profiles_edit_title_add
        else -> R.string.profiles_edit_title_edit
    },
)

@Composable
private fun ProfileEditUiState.primaryLabel() =
    stringResource(if (isNew) R.string.profiles_edit_next else R.string.profiles_common_save)

@Composable
internal fun ProfileEditPhoneScreen(
    state: ProfileEditUiState,
    viewModel: ProfileEditViewModel,
    onCancel: () -> Unit,
    openDialog: (EditDialog) -> Unit,
) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!state.firstRun) TextButton(onClick = onCancel) { Text(stringResource(R.string.profiles_common_cancel)) } else Spacer(Modifier.width(16.dp))
            Text(state.title(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = viewModel::onPrimary, enabled = !state.saving) { Text(state.primaryLabel()) }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = JazzyTheme.spacing.gutter),
        ) {
            Spacer(Modifier.height(16.dp))
            AvatarButton(state.avatarId, size = 112.dp, onClick = { openDialog(EditDialog.AVATAR) }, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(20.dp))
            NameField(state, viewModel)
            ErrorText(state.error?.asString())
            ProfileFormRows(state, viewModel, openDialog)
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** TV: identity column on the left, settings list on the right; initial focus on the avatar. */
@Composable
internal fun ProfileEditTvScreen(
    state: ProfileEditUiState,
    viewModel: ProfileEditViewModel,
    onCancel: () -> Unit,
    openDialog: (EditDialog) -> Unit,
) {
    val avatarFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { avatarFocus.requestFocus() } }
    Row(
        Modifier.fillMaxSize().padding(horizontal = JazzyTheme.spacing.gutter, vertical = JazzyTheme.spacing.verticalSafe),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
    ) {
        Column(Modifier.width(320.dp).padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(state.title(), style = MaterialTheme.typography.headlineMedium)
            AvatarButton(state.avatarId, size = 140.dp, onClick = { openDialog(EditDialog.AVATAR) }, focusRequester = avatarFocus)
            NameField(state, viewModel)
            ErrorText(state.error?.asString())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PillButton(
                    state.primaryLabel(), onClick = viewModel::onPrimary, style = PillButtonStyle.ACCENT,
                    icon = Icons.Filled.Check, enabled = !state.saving,
                )
                if (!state.firstRun) PillButton(stringResource(R.string.profiles_common_cancel), onClick = onCancel, icon = Icons.Filled.Close)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
            ProfileFormRows(state, viewModel, openDialog)
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun AvatarButton(avatarId: Int, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit, modifier: Modifier = Modifier, focusRequester: FocusRequester? = null) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.withFocusRequester(focusRequester).jazzyClickable(RoundedCornerShape(size / 8), onClick = onClick)) {
            AvatarTile(avatarId, size = size)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            androidx.compose.material3.Icon(Icons.Filled.Edit, null, tint = JazzyColors.TextSecondary, modifier = Modifier.height(14.dp))
            Text(stringResource(R.string.profiles_edit_change_avatar), style = MaterialTheme.typography.labelMedium, color = JazzyColors.TextSecondary)
        }
    }
}

@Composable
private fun NameField(state: ProfileEditUiState, viewModel: ProfileEditViewModel) {
    val error = state.showNameError && !state.nameValid
    OutlinedTextField(
        value = state.name,
        onValueChange = viewModel::setName,
        label = { Text(stringResource(R.string.profiles_edit_name_label)) },
        singleLine = true,
        isError = error,
        supportingText = {
            Text(
                if (error) {
                    stringResource(R.string.profiles_edit_name_error, ProfileEditUiState.MAX_NAME)
                } else {
                    stringResource(R.string.profiles_edit_name_counter, state.name.length, ProfileEditUiState.MAX_NAME)
                },
            )
        },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JazzyColors.Focus, cursorColor = JazzyColors.Accent),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ErrorText(error: String?) {
    if (error != null) Text(error, color = JazzyColors.Poor, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun ProfileFormRows(state: ProfileEditUiState, viewModel: ProfileEditViewModel, openDialog: (EditDialog) -> Unit) {
    val off = stringResource(R.string.profiles_common_off)
    SettingsSectionHeader(stringResource(R.string.profiles_edit_section_viewing))
    SettingsSwitchRow(
        title = stringResource(R.string.profiles_edit_kids_title),
        subtitle = stringResource(R.string.profiles_edit_kids_subtitle),
        checked = state.isKids,
        onCheckedChange = viewModel::setKids,
    )
    SettingsActionRow(
        title = stringResource(R.string.profiles_edit_maturity_title),
        subtitle = stringResource(R.string.profiles_edit_maturity_subtitle),
        value = stringResource(state.maturity.labelRes),
        icon = Icons.Filled.Shield,
        onClick = { openDialog(EditDialog.MATURITY) },
    )
    SettingsActionRow(
        title = stringResource(R.string.profiles_edit_content_language_title),
        subtitle = stringResource(R.string.profiles_edit_content_language_subtitle),
        value = languageLabel(state.language, off),
        icon = Icons.Filled.Language,
        onClick = { openDialog(EditDialog.LANGUAGE) },
    )
    SettingsActionRow(
        title = stringResource(R.string.profiles_edit_subtitle_language_title),
        value = languageLabel(state.subtitleLanguage, off),
        icon = Icons.Filled.Subtitles,
        onClick = { openDialog(EditDialog.SUBTITLES) },
    )
    SettingsSwitchRow(
        title = stringResource(R.string.profiles_edit_autoplay_title),
        checked = state.autoplayNext,
        onCheckedChange = viewModel::setAutoplayNext,
    )
    if (!state.isNew) {
        SettingsActionRow(
            title = stringResource(R.string.profiles_edit_genres_title),
            subtitle = stringResource(R.string.profiles_edit_genres_subtitle),
            value = if (state.genres.isEmpty()) {
                stringResource(R.string.profiles_common_none)
            } else {
                pluralStringResource(R.plurals.profiles_genres_selected, state.genres.size, state.genres.size)
            },
            icon = Icons.Filled.Favorite,
            showChevron = true,
            onClick = viewModel::openGenres,
        )
    }

    SettingsSectionHeader(stringResource(R.string.profiles_edit_section_lock))
    if (!state.pinEnabled) {
        SettingsActionRow(
            title = stringResource(R.string.profiles_edit_pin_title),
            subtitle = stringResource(R.string.profiles_edit_pin_subtitle),
            value = off,
            icon = Icons.Filled.LockOpen,
            onClick = viewModel::startSetPin,
        )
    } else {
        SettingsActionRow(
            title = stringResource(R.string.profiles_edit_change_pin_title),
            subtitle = stringResource(
                if (state.hasPin) R.string.profiles_edit_change_pin_subtitle_locked else R.string.profiles_edit_change_pin_subtitle_pending,
            ),
            icon = Icons.Filled.Lock,
            onClick = viewModel::startChangePin,
        )
        SettingsActionRow(title = stringResource(R.string.profiles_edit_remove_pin_title), icon = Icons.Filled.LockOpen, onClick = viewModel::startRemovePin)
    }

    if (!state.isNew) {
        SettingsSectionHeader(stringResource(R.string.profiles_edit_section_delete))
        SettingsActionRow(
            title = stringResource(R.string.profiles_edit_delete_title),
            subtitle = stringResource(
                if (state.canDelete) R.string.profiles_edit_delete_subtitle else R.string.profiles_edit_error_last_profile,
            ),
            icon = Icons.Filled.Delete,
            destructive = true,
            enabled = state.canDelete,
            onClick = { openDialog(EditDialog.DELETE) },
        )
    }
}

/** All 24 bundled avatars (PR-2); initial focus on the current one. */
@Composable
internal fun AvatarPickerDialog(selected: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val tileSize = if (JazzyTheme.isTv) 64.dp else 56.dp
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profiles_edit_avatar_picker_title)) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (JazzyTheme.isTv) 6 else 4),
                modifier = Modifier.heightIn(max = 420.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items((0 until AVATAR_COUNT).toList(), key = { it }) { id ->
                    val shape = RoundedCornerShape(tileSize / 8)
                    Box(
                        Modifier
                            .withFocusRequester(if (id == selected) focus else null)
                            .then(if (id == selected) Modifier.border(3.dp, JazzyColors.Accent, shape) else Modifier)
                            .jazzyClickable(shape) { onSelect(id) },
                        contentAlignment = Alignment.Center,
                    ) { AvatarTile(id, size = tileSize) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.profiles_common_cancel)) } },
        containerColor = JazzyColors.SurfaceRaised,
    )
}

/** "Pick 3 or more genres you like" (PR-3): skippable for new profiles, editable later. */
@Composable
internal fun GenrePickerStep(state: ProfileEditUiState, viewModel: ProfileEditViewModel) {
    val tv = JazzyTheme.isTv
    val firstChip = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (tv) runCatching { firstChip.requestFocus() } }
    val count = state.genres.size
    val enough = count >= ProfileEditUiState.MIN_GENRES
    Column(
        Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = JazzyTheme.spacing.gutter, vertical = if (tv) JazzyTheme.spacing.verticalSafe + 16.dp else 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            if (state.isNew) {
                stringResource(R.string.profiles_genres_pick_title, ProfileEditUiState.MIN_GENRES)
            } else {
                stringResource(R.string.profiles_edit_genres_title)
            },
            style = if (tv) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
        )
        Text(
            stringResource(R.string.profiles_genres_explainer),
            style = MaterialTheme.typography.bodyMedium,
            color = JazzyColors.TextSecondary,
            modifier = Modifier.widthIn(max = 640.dp),
        )
        ToggleChipGroup(
            options = UnifiedGenre.pickable.map { it.id to (genreLabelRes(it.id)?.let { r -> stringResource(r) } ?: it.label) },
            selected = state.genres,
            onToggle = viewModel::toggleGenre,
            firstChipFocusRequester = firstChip,
            modifier = Modifier.widthIn(max = 900.dp),
        )
        Text(
            when {
                count == 0 -> stringResource(R.string.profiles_genres_nothing_selected)
                !enough && state.isNew -> (ProfileEditUiState.MIN_GENRES - count).let { more ->
                    pluralStringResource(R.plurals.profiles_genres_selected_pick_more, more, count, more)
                }
                else -> pluralStringResource(R.plurals.profiles_genres_selected, count, count)
            },
            style = MaterialTheme.typography.bodySmall,
            color = JazzyColors.TextTertiary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.isNew) {
                PillButton(stringResource(R.string.profiles_genres_skip), onClick = { viewModel.finishGenres(skip = true) }, enabled = !state.saving)
                PillButton(
                    stringResource(R.string.profiles_genres_continue), onClick = { viewModel.finishGenres(skip = false) }, style = PillButtonStyle.ACCENT,
                    enabled = enough && !state.saving,
                )
            } else {
                PillButton(stringResource(R.string.profiles_common_cancel), onClick = viewModel::backToForm)
                PillButton(stringResource(R.string.profiles_common_save), onClick = { viewModel.finishGenres(skip = false) }, style = PillButtonStyle.ACCENT)
            }
        }
    }
}
