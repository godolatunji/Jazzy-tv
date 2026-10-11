package com.nexient.jazzy.feature.details.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.component.Badge
import com.nexient.jazzy.core.designsystem.component.BadgeTone
import com.nexient.jazzy.core.designsystem.component.focusRing
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.model.FailureReason
import com.nexient.jazzy.core.model.ProviderSearchState
import com.nexient.jazzy.core.model.Resolution
import com.nexient.jazzy.core.model.SourceSort
import com.nexient.jazzy.feature.details.R
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
            IconButton(onClick = onRefresh, modifier = Modifier.focusRing(CircleShape)) { Icon(Icons.Filled.Refresh, stringResource(R.string.details_sources_refresh)) }
            androidx.compose.foundation.layout.Box {
                IconButton(onClick = { sortMenu = true }, modifier = Modifier.focusRing(CircleShape)) { Icon(Icons.Filled.Sort, stringResource(R.string.details_sources_sort)) }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    listOf(SourceSort.SEEDERS to R.string.details_sort_seeders, SourceSort.QUALITY to R.string.details_sort_quality, SourceSort.SIZE to R.string.details_sort_size).forEach { (sort, labelRes) ->
                        val label = stringResource(labelRes)
                        DropdownMenuItem(text = { Text(if (f.sort == sort) stringResource(R.string.details_sort_selected, label) else label) }, onClick = { sortMenu = false; onFilters { it.copy(sort = sort) } })
                    }
                }
            }
        }
        if (sources.unreleasedWarning) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Warning, null, tint = JazzyColors.Fair, modifier = Modifier.size(16.dp))
                Text(stringResource(R.string.details_sources_unreleased), style = MaterialTheme.typography.bodySmall, color = JazzyColors.Fair)
            }
        }
        val statuses = sources.result?.statuses.orEmpty()
        if (statuses.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                statuses.forEach { s ->
                    when (val st = s.state) {
                        ProviderSearchState.Searching -> Badge(stringResource(R.string.details_provider_searching, s.providerName), outlined = true)
                        is ProviderSearchState.Done -> Badge(stringResource(R.string.details_provider_done, s.providerName, st.count), tone = BadgeTone.SUCCESS, outlined = true)
                        ProviderSearchState.Unsupported -> Unit
                        is ProviderSearchState.Failed -> Badge(stringResource(R.string.details_provider_failed, s.providerName), tone = BadgeTone.DANGER, outlined = true)
                    }
                }
            }
            val failures = statuses.filter { it.state is ProviderSearchState.Failed }
            if (failures.isNotEmpty()) {
                TextButton(onClick = { showErrors = !showErrors }, modifier = Modifier.focusRing()) { Text(if (showErrors) stringResource(R.string.details_sources_hide_errors) else pluralStringResource(R.plurals.details_sources_failures, failures.size, failures.size)) }
                if (showErrors) failures.forEach { s ->
                    val st = s.state as ProviderSearchState.Failed
                    Text(stringResource(R.string.details_provider_failure_line, s.providerName, stringResource(reasonText(st.reason))), style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextSecondary)
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
                    modifier = Modifier.focusRing(ChipShape),
                )
            }
            FilterChip(selected = f.hideCam, onClick = { onFilters { it.copy(hideCam = !it.hideCam) } }, label = { Text(stringResource(R.string.details_filter_hide_cam)) }, modifier = Modifier.focusRing(ChipShape))
            FilterChip(selected = f.hideUnsupported, onClick = { onFilters { it.copy(hideUnsupported = !it.hideUnsupported) } }, label = { Text(stringResource(R.string.details_filter_hide_unsupported)) }, modifier = Modifier.focusRing(ChipShape))
            FilterChip(selected = f.showDead, onClick = { onFilters { it.copy(showDead = !it.showDead) } }, label = { Text(stringResource(R.string.details_filter_show_dead)) }, modifier = Modifier.focusRing(ChipShape))
        }
        if (sources.hiddenCount > 0) {
            Text(pluralStringResource(R.plurals.details_sources_hidden_count, sources.hiddenCount, sources.hiddenCount), style = MaterialTheme.typography.labelSmall, color = JazzyColors.TextTertiary)
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
            stringResource(
                if (result.statuses.isEmpty()) R.string.details_sources_all_off
                else if (allDisabledOrFailed) R.string.details_sources_no_answer
                else R.string.details_sources_no_links,
            ),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            if (sources.hiddenCount > 0) pluralStringResource(R.plurals.details_sources_hidden_links, sources.hiddenCount, sources.hiddenCount)
            else stringResource(R.string.details_sources_try_again),
            style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextSecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onRefresh, modifier = Modifier.focusRing()) { Text(stringResource(R.string.details_sources_search_again)) }
            TextButton(onClick = onOpenSites, modifier = Modifier.focusRing()) { Text(stringResource(R.string.details_sources_check_sites)) }
        }
    }
}

@StringRes
private fun reasonText(reason: FailureReason): Int = when (reason) {
    FailureReason.TIMEOUT -> R.string.details_failure_timeout
    FailureReason.BLOCKED -> R.string.details_failure_blocked
    FailureReason.UNREACHABLE -> R.string.details_failure_unreachable
    FailureReason.HTTP_ERROR -> R.string.details_failure_http_error
    FailureReason.PARSE_ERROR -> R.string.details_failure_parse_error
    FailureReason.LOGIN_REQUIRED -> R.string.details_failure_login_required
    FailureReason.OTHER -> R.string.details_failure_other
}

/** FilterChip's shape, for its focus ring. */
private val ChipShape = RoundedCornerShape(8.dp)
