package com.nexient.jazzy.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.component.Badge
import com.nexient.jazzy.core.designsystem.component.SettingsSectionHeader
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.feature.settings.AboutText
import com.nexient.jazzy.feature.settings.Licence
import com.nexient.jazzy.feature.settings.R

private val BlockShape = RoundedCornerShape(12.dp)

@Composable
internal fun AboutPhoneScreen(version: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.windowInsetsPadding(WindowInsets.statusBars)) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_common_back)) }
            Text(stringResource(R.string.settings_about_title), style = MaterialTheme.typography.titleLarge)
        }
        LazyColumn(
            contentPadding = PaddingValues(start = JazzyTheme.spacing.gutter, end = JazzyTheme.spacing.gutter, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            aboutItems(version, focusFirst = null)
        }
    }
}

/**
 * TV: every block is focusable so the D-pad can move (and scroll) through plain text; the first
 * block gets initial focus.
 */
@Composable
internal fun AboutTvScreen(version: String, modifier: Modifier = Modifier) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 900.dp),
            contentPadding = PaddingValues(
                start = JazzyTheme.spacing.gutter, end = JazzyTheme.spacing.gutter,
                top = JazzyTheme.spacing.verticalSafe + 16.dp, bottom = 64.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            aboutItems(version, focusFirst = first)
        }
    }
}

private fun LazyListScope.aboutItems(version: String, focusFirst: FocusRequester?) {
    item(key = "app") {
        Block(focusRequester = focusFirst) {
            Text(stringResource(R.string.settings_about_brand), style = MaterialTheme.typography.displaySmall, color = JazzyColors.Accent)
            Text(stringResource(R.string.settings_about_version, version), style = MaterialTheme.typography.bodyMedium, color = JazzyColors.TextSecondary)
        }
    }
    item(key = "tmdb") {
        Block {
            TmdbWordmark()
            Text(stringResource(R.string.settings_about_tmdb_attribution), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.settings_about_tmdb_detail), style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextSecondary)
        }
    }
    item(key = "disclaimer") {
        Block {
            Text(stringResource(R.string.settings_about_disclaimer_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.settings_about_disclaimer), style = MaterialTheme.typography.bodyMedium, color = JazzyColors.TextSecondary)
        }
    }
    item(key = "privacy") {
        Block {
            Text(stringResource(R.string.settings_about_privacy_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.settings_about_privacy), style = MaterialTheme.typography.bodyMedium, color = JazzyColors.TextSecondary)
        }
    }
    item(key = "licences-header") { SettingsSectionHeader(stringResource(R.string.settings_about_licences_header)) }
    items(AboutText.LICENCES, key = { "licence:" + it.name }) { LicenceBlock(it) }
}

@Composable
private fun LicenceBlock(licence: Licence) {
    Block {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(licence.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f, fill = false))
            Badge(licence.licence, outlined = true)
        }
        Text(stringResource(licence.note), style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextSecondary)
    }
}

/** Text-only TMDB wordmark (no downloaded logo). */
@Composable
private fun TmdbWordmark() {
    Box(
        Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF90CEA1), Color(0xFF01B4E4))), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(stringResource(R.string.settings_about_tmdb_wordmark), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color(0xFF0D253F))
    }
}

/** A card-like block; on TV it is focusable with a visible outline so long text can be scrolled by D-pad. */
@Composable
private fun Block(focusRequester: FocusRequester? = null, content: @Composable () -> Unit) {
    val tv = JazzyTheme.isTv
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(
        Modifier.fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .then(if (tv) Modifier.focusable(interactionSource = interaction) else Modifier)
            .background(JazzyColors.Surface, BlockShape)
            .then(if (focused) Modifier.border(3.dp, JazzyColors.Focus, BlockShape) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}
