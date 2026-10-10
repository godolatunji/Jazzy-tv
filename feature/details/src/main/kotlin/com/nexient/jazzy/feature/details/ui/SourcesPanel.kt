package com.nexient.jazzy.feature.details.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.component.Badge
import com.nexient.jazzy.core.designsystem.component.BadgeTone
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.model.FailureReason
import com.nexient.jazzy.core.model.ProviderSearchState
import com.nexient.jazzy.core.model.Resolution
import com.nexient.jazzy.core.model.SourceSort
import com.nexient.jazzy.feature.details.SourceFilters
import com.nexient.jazzy.feature.details.SourcesUi

/** Header of the Sources list: title, per-site progress, filters and sort (SL-2, SL-5, SL-8). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SourcesHeader(
    sources: SourcesUi,
    title: String,
    onFilters: ((SourceFilters) -> SourceFilters) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sortMenu by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }
    val f = sources.filters
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (sources.searching) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = JazzyColors.Accent)
            IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, "Refresh sources") }
            androidx.compose.foundation.layout.Box {
                IconButton(onClick = { sortMenu = true }) { Icon(Icons.Filled.Sort, "Sort sources") }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    listOf(SourceSort.SEEDERS to "Most seeders", SourceSort.QUALITY to "Quality", SourceSort.SIZE to "Size").forEach { (sort, label) ->
                        DropdownMenuItem(text = { Text((if (f.sort == sort) "✓ " else "") + label) }, onClick = { sortMenu = false; onFilters { it.copy(sort = sort) } })
                    }
                }
            }
        }
        if (sources.unreleasedWarning) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Warning, null, tint = JazzyColors.Fair, modifier = Modifier.size(16.dp))
                Text("Not released yet — links may be fake or low quality.", style = MaterialTheme.typography.bodySmall, color = JazzyColors.Fair)
            }
        }
        val statuses = sources.result?.statuses.orEmpty()
        if (statuses.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                statuses.forEach { s ->
                    when (val st = s.state) {
                        ProviderSearchState.Searching -> Badge("${s.providerName} …", outlined = true)
                        is ProviderSearchState.Done -> Badge("${s.providerName} ${st.count}", tone = BadgeTone.SUCCESS, outlined = true)
                        ProviderSearchState.Unsupported -> Unit
                        is ProviderSearchState.Failed -> Badge("${s.providerName} ✕", tone = BadgeTone.DANGER, outlined = true)
                    }
                }
            }
            val failures = statuses.filter { it.state is ProviderSearchState.Failed }
            if (failures.isNotEmpty()) {
                TextButton(onClick = { showErrors = !showErrors }) { Text(if (showErrors) "Hide site errors" else "${failures.size} site(s) failed — details") }
                if (showErrors) failures.forEach { s ->
                    val st = s.state as ProviderSearchState.Failed
                    Text("${s.providerName}: ${reasonText(st.reason)}", style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextSecondary)
                }
            }
        }
        if (sources.searching && sources.rows.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth(), color = JazzyColors.Accent)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(Resolution.UHD_2160, Resolution.FHD_1080, Resolution.HD_720, Resolution.SD).forEach { r ->
                FilterChip(
                    selected = r in f.resolutions,
                    onClick = { onFilters { it.copy(resolutions = if (r in it.resolutions) it.resolutions - r else it.resolutions + r) } },
                    label = { Text(r.label) },
                )
            }
            FilterChip(selected = f.hideCam, onClick = { onFilters { it.copy(hideCam = !it.hideCam) } }, label = { Text("Hide CAM/TS") })
            FilterChip(selected = f.hideUnsupported, onClick = { onFilters { it.copy(hideUnsupported = !it.hideUnsupported) } }, label = { Text("Hide unsupported") })
            FilterChip(selected = f.showDead, onClick = { onFilters { it.copy(showDead = !it.showDead) } }, label = { Text("Show dead links") })
        }
        if (sources.hiddenCount > 0) {
            Text("${sources.hiddenCount} hidden by filters", style = MaterialTheme.typography.labelSmall, color = JazzyColors.TextTertiary)
        }
    }
}

/** Empty / failure states for the Sources list (SL-8). */
@Composable
internal fun SourcesEmpty(sources: SourcesUi, onRefresh: () -> Unit, onOpenSites: () -> Unit, modifier: Modifier = Modifier) {
    val result = sources.result ?: return
    if (!result.isComplete || sources.rows.isNotEmpty()) return
    val allDisabledOrFailed = result.statuses.isEmpty() || result.statuses.all { it.state is ProviderSearchState.Failed || it.state == ProviderSearchState.Unsupported }
    Column(modifier.fillMaxWidth().padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (result.statuses.isEmpty()) "All sites are turned off" else if (allDisabledOrFailed) "No site answered" else "No links found",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            if (sources.hiddenCount > 0) "${sources.hiddenCount} links are hidden by your filters." else "Try again, or check which sites are working.",
            style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextSecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onRefresh) { Text("Search again") }
            TextButton(onClick = onOpenSites) { Text("Check sites") }
        }
    }
}

private fun reasonText(reason: FailureReason) = when (reason) {
    FailureReason.TIMEOUT -> "timed out"
    FailureReason.BLOCKED -> "blocked by the site"
    FailureReason.UNREACHABLE -> "site unreachable"
    FailureReason.HTTP_ERROR -> "site returned an error"
    FailureReason.PARSE_ERROR -> "page layout changed"
    FailureReason.LOGIN_REQUIRED -> "login required"
    FailureReason.OTHER -> "failed"
}
