package com.nexient.jazzy.feature.profiles.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.component.FullScreenLoading
import com.nexient.jazzy.core.designsystem.component.PillButton
import com.nexient.jazzy.core.designsystem.component.SettingsSwitchRow
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.feature.profiles.ProfilePickerUiState
import com.nexient.jazzy.feature.profiles.ProfilePickerViewModel
import com.nexient.jazzy.feature.profiles.R

@Composable
private fun ProfilePickerUiState.heading() = stringResource(
    when {
        editMode -> R.string.profiles_picker_heading_manage
        profiles.isEmpty() -> R.string.profiles_picker_heading_empty
        else -> R.string.profiles_picker_heading
    },
)

@Composable
private fun manageLabel(editMode: Boolean) =
    stringResource(if (editMode) R.string.profiles_picker_done else R.string.profiles_picker_manage)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfilePickerPhoneScreen(state: ProfilePickerUiState, viewModel: ProfilePickerViewModel) {
    if (state.loading) {
        FullScreenLoading(Modifier.background(JazzyColors.Background))
        return
    }
    Column(
        Modifier.fillMaxSize()
            .background(JazzyColors.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = JazzyTheme.spacing.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.profiles_picker_brand), style = MaterialTheme.typography.headlineSmall, color = JazzyColors.Accent, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(40.dp))
        Text(state.heading(), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            maxItemsInEachRow = 3,
        ) {
            state.profiles.forEach { p ->
                ProfileTile(p, state.editMode, size = 96.dp, onClick = { viewModel.onProfileClick(p) })
            }
            if (state.canAdd) AddProfileTile(size = 96.dp, onClick = viewModel::onAddClick)
        }
        Spacer(Modifier.height(32.dp))
        if (state.profiles.isNotEmpty()) {
            PillButton(
                text = manageLabel(state.editMode),
                icon = if (state.editMode) Icons.Filled.Check else Icons.Filled.Edit,
                onClick = viewModel::toggleManage,
            )
            Spacer(Modifier.height(24.dp))
            SettingsSwitchRow(
                title = stringResource(R.string.profiles_picker_remember_title),
                subtitle = stringResource(R.string.profiles_picker_remember_subtitle),
                checked = state.rememberProfile,
                onCheckedChange = viewModel::setRememberProfile,
                modifier = Modifier.widthIn(max = 480.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** 10-foot picker: one row of large tiles, initial focus on the first tile (spec §5.10). */
@Composable
internal fun ProfilePickerTvScreen(state: ProfilePickerUiState, viewModel: ProfilePickerViewModel) {
    if (state.loading) {
        FullScreenLoading(Modifier.background(JazzyColors.Background))
        return
    }
    val firstTile = remember { FocusRequester() }
    LaunchedEffect(state.profiles.isEmpty()) { runCatching { firstTile.requestFocus() } }

    Box(
        Modifier.fillMaxSize()
            .background(JazzyColors.Background)
            .padding(horizontal = JazzyTheme.spacing.gutter, vertical = JazzyTheme.spacing.verticalSafe),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(state.heading(), style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(40.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                state.profiles.forEachIndexed { index, p ->
                    ProfileTile(
                        p, state.editMode, size = 120.dp,
                        onClick = { viewModel.onProfileClick(p) },
                        focusRequester = if (index == 0) firstTile else null,
                    )
                }
                if (state.canAdd) {
                    AddProfileTile(
                        size = 120.dp,
                        onClick = viewModel::onAddClick,
                        focusRequester = if (state.profiles.isEmpty()) firstTile else null,
                    )
                }
            }
            Spacer(Modifier.height(36.dp))
            if (state.profiles.isNotEmpty()) {
                PillButton(
                    text = manageLabel(state.editMode),
                    icon = if (state.editMode) Icons.Filled.Check else Icons.Filled.Edit,
                    onClick = viewModel::toggleManage,
                )
                Spacer(Modifier.height(16.dp))
                SettingsSwitchRow(
                    title = stringResource(R.string.profiles_picker_remember_title),
                    subtitle = stringResource(R.string.profiles_picker_remember_subtitle),
                    checked = state.rememberProfile,
                    onCheckedChange = viewModel::setRememberProfile,
                    modifier = Modifier.widthIn(max = 560.dp),
                )
            }
        }
    }
}
