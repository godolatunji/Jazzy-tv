package com.nexient.jazzy.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.component.AvatarTile
import com.nexient.jazzy.core.designsystem.component.SettingsSectionHeader
import com.nexient.jazzy.core.designsystem.component.jazzyClickable
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.feature.settings.SettingsUiState
import com.nexient.jazzy.feature.settings.SettingsViewModel

private const val FOOTNOTE = "Settings are stored only on this device."

@Composable
internal fun SettingsPhoneScreen(state: SettingsUiState, viewModel: SettingsViewModel, nav: SettingsNav) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = JazzyTheme.spacing.gutter, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            state.profile?.let { p ->
                AvatarTile(p.avatarId, Modifier.jazzyClickable(onClick = nav.manageProfiles), size = 32.dp)
            }
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = JazzyColors.Accent)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = JazzyTheme.spacing.gutter, end = JazzyTheme.spacing.gutter, bottom = 32.dp),
        ) {
            state.visibleSections().forEach { section ->
                item(key = section.name) {
                    Column {
                        SettingsSectionHeader(section.title)
                        SettingsSectionContent(section, state, viewModel, nav)
                    }
                }
            }
            item(key = "footnote") {
                Text(
                    FOOTNOTE,
                    style = MaterialTheme.typography.bodySmall,
                    color = JazzyColors.TextTertiary,
                    modifier = Modifier.padding(top = 24.dp, start = 4.dp),
                )
            }
        }
    }
}

/**
 * TV: a section list on the left and the focused section's items on the right. Focusing a section
 * shows it; Right / centre moves into its items, and Left returns to the same section (focus restorer).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SettingsTvScreen(state: SettingsUiState, viewModel: SettingsViewModel, nav: SettingsNav) {
    val sections = state.visibleSections()
    var selectedName by rememberSaveable { mutableStateOf(sections.first().name) }
    val selected = sections.firstOrNull { it.name == selectedName } ?: sections.first()
    val firstSection = remember { FocusRequester() }
    val firstItem = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstSection.requestFocus() } }

    Row(
        Modifier.fillMaxSize().padding(horizontal = JazzyTheme.spacing.gutter, vertical = JazzyTheme.spacing.verticalSafe),
        horizontalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        Column(
            Modifier.width(340.dp).fillMaxHeight().focusRestorer(firstSection).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(Modifier.padding(start = 12.dp, top = 16.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                state.profile?.let { AvatarTile(it.avatarId, size = 36.dp) }
            }
            sections.forEachIndexed { index, section ->
                SectionTab(
                    section = section,
                    selected = section == selected,
                    onFocused = { selectedName = section.name },
                    onClick = { runCatching { firstItem.requestFocus() } },
                    modifier = if (index == 0) Modifier.focusRequester(firstSection) else Modifier,
                )
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(top = 16.dp)) {
            Text(selected.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 12.dp, bottom = 12.dp))
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom = 8.dp), color = JazzyColors.Accent)
            SettingsSectionContent(selected, state, viewModel, nav, firstItemFocus = firstItem)
            Spacer(Modifier.height(24.dp))
            Text(FOOTNOTE, style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextTertiary, modifier = Modifier.padding(start = 12.dp))
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun SectionTab(
    section: SettingsSection,
    selected: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .jazzyClickable(shape, focusScale = 1.03f, onFocused = { if (it) onFocused() }, onClick = onClick)
            .background(if (selected) JazzyColors.SurfaceVariant else JazzyColors.Background, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(section.icon, null, tint = if (selected) JazzyColors.Accent else JazzyColors.TextSecondary, modifier = Modifier.size(24.dp))
        Text(
            section.title,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) JazzyColors.OnBackground else JazzyColors.TextSecondary,
        )
    }
}
