package com.nexient.jazzy.feature.discover.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.component.JazzyChip
import com.nexient.jazzy.core.designsystem.component.ShimmerBox
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.feature.discover.DiscoverFilters
import com.nexient.jazzy.feature.discover.DiscoverLanguages
import com.nexient.jazzy.feature.discover.DiscoverSort
import com.nexient.jazzy.feature.discover.DiscoverUiState
import com.nexient.jazzy.feature.discover.R
import com.nexient.jazzy.feature.discover.RatingOptions
import com.nexient.jazzy.feature.discover.RuntimeRange
import com.nexient.jazzy.feature.discover.VoteOptions
import com.nexient.jazzy.feature.discover.runtimePresets
import com.nexient.jazzy.core.common.formatCount

/** Filter mutations, shared by the phone sheet and the TV filter column. */
internal class DiscoverCallbacks(
    val onMediaType: (MediaType) -> Unit,
    val onToggleGenre: (Int) -> Unit,
    val onClearGenres: () -> Unit,
    val onYears: (Int?, Int?) -> Unit,
    val onMinRating: (Int?) -> Unit,
    val onMinVotes: (Int) -> Unit,
    val onLanguage: (String?) -> Unit,
    val onRuntime: (RuntimeRange?) -> Unit,
    val onSort: (DiscoverSort) -> Unit,
    val onReset: () -> Unit,
    val onRetryGenres: () -> Unit,
    val currentYear: Int,
)

/** Every Discover filter as chip groups (spec §5.5). The caller provides scrolling. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FilterPanel(
    state: DiscoverUiState,
    callbacks: DiscoverCallbacks,
    modifier: Modifier = Modifier,
    includeTypeAndSort: Boolean = true,
) {
    val f = state.filters
    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (includeTypeAndSort) {
            Section(stringResource(R.string.discover_section_type)) {
                MediaTypeChips(f.mediaType, callbacks.onMediaType)
            }
            Section(stringResource(R.string.discover_section_sort)) {
                SortChips(f, callbacks.onSort)
            }
        }
        Section(
            stringResource(R.string.discover_section_genres),
            trailing = if (f.genreIds.isNotEmpty()) ({ TextButton(onClick = callbacks.onClearGenres) { Text(stringResource(R.string.discover_genres_clear)) } }) else null,
        ) {
            val g = state.genres
            when {
                g.loading && g.genres.isEmpty() -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(8) { ShimmerBox(Modifier.size(width = 84.dp, height = 32.dp).clip(RoundedCornerShape(50))) }
                }
                g.failed && g.genres.isEmpty() -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.discover_genres_error), color = JazzyColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = callbacks.onRetryGenres) { Text(stringResource(R.string.discover_common_retry)) }
                }
                else -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    g.genres.forEach { genre ->
                        JazzyChip(genre.name, genre.id in f.genreIds, onClick = { callbacks.onToggleGenre(genre.id) })
                    }
                }
            }
            if (f.genreIds.size > 1) {
                Text(stringResource(R.string.discover_genres_all_selected), style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextTertiary)
            }
        }
        Section(stringResource(R.string.discover_section_year)) {
            YearFilter(f, callbacks.currentYear, callbacks.onYears)
        }
        Section(stringResource(R.string.discover_section_rating)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                JazzyChip(stringResource(R.string.discover_common_any), f.minRating == null, onClick = { callbacks.onMinRating(null) })
                RatingOptions.forEach { r -> JazzyChip(stringResource(R.string.discover_rating_option, r), f.minRating == r, onClick = { callbacks.onMinRating(r) }) }
            }
        }
        Section(stringResource(R.string.discover_section_votes)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VoteOptions.forEach { v -> JazzyChip(if (v == 0) stringResource(R.string.discover_common_any) else stringResource(R.string.discover_votes_option, formatCount(v)), f.minVotes == v, onClick = { callbacks.onMinVotes(v) }) }
            }
        }
        Section(stringResource(R.string.discover_section_language)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                JazzyChip(stringResource(R.string.discover_common_any), f.language == null, onClick = { callbacks.onLanguage(null) })
                DiscoverLanguages.forEach { (code, label) -> JazzyChip(stringResource(label), f.language == code, onClick = { callbacks.onLanguage(code) }) }
            }
        }
        Section(stringResource(if (f.mediaType == MediaType.MOVIE) R.string.discover_section_runtime else R.string.discover_section_episode_runtime)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                JazzyChip(stringResource(R.string.discover_common_any), f.runtimeMin == null && f.runtimeMax == null, onClick = { callbacks.onRuntime(null) })
                runtimePresets(f.mediaType).forEach { r -> JazzyChip(stringResource(r.labelRes), f.hasRuntime(r), onClick = { callbacks.onRuntime(r) }) }
            }
        }
        if (f.activeCount > 0) {
            TextButton(onClick = callbacks.onReset) { Text(stringResource(R.string.discover_reset_all)) }
        }
    }
}

@Composable
internal fun MediaTypeChips(selected: MediaType, onSelect: (MediaType) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        JazzyChip(stringResource(R.string.discover_type_movies), selected == MediaType.MOVIE, onClick = { onSelect(MediaType.MOVIE) })
        JazzyChip(stringResource(R.string.discover_type_tv), selected == MediaType.TV, onClick = { onSelect(MediaType.TV) })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SortChips(f: DiscoverFilters, onSort: (DiscoverSort) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DiscoverSort.entries.filter { it.supports(f.mediaType) }.forEach { s ->
            JazzyChip(stringResource(s.labelRes), f.sort == s, onClick = { onSort(s) })
        }
    }
}

/** A chip that opens a dropdown of sort orders (phone header). */
@Composable
internal fun SortDropdownChip(f: DiscoverFilters, onSort: (DiscoverSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        JazzyChip(stringResource(R.string.discover_sort_chip, stringResource(f.sort.labelRes)), selected = false, onClick = { open = true }, trailingIcon = Icons.Filled.ArrowDropDown)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DiscoverSort.entries.filter { it.supports(f.mediaType) }.forEach { s ->
                DropdownMenuItem(
                    text = { Text(stringResource(s.labelRes).let { if (s == f.sort) stringResource(R.string.discover_common_selected, it) else it }) },
                    onClick = { open = false; onSort(s) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun YearFilter(f: DiscoverFilters, currentYear: Int, onYears: (Int?, Int?) -> Unit) {
    val presets = listOf(
        Triple(stringResource(R.string.discover_year_this), currentYear, currentYear),
        Triple(stringResource(R.string.discover_year_last_5), currentYear - 4, currentYear),
        Triple(stringResource(R.string.discover_year_decade, 2010), 2010, 2019),
        Triple(stringResource(R.string.discover_year_decade, 2000), 2000, 2009),
        Triple(stringResource(R.string.discover_year_decade, 1990), 1990, 1999),
        Triple(stringResource(R.string.discover_year_decade, 1980), 1980, 1989),
        Triple(stringResource(R.string.discover_year_before, 1980), null, 1979),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            JazzyChip(stringResource(R.string.discover_common_any), f.yearFrom == null && f.yearTo == null, onClick = { onYears(null, null) })
            presets.forEach { (label, from, to) ->
                JazzyChip(label, f.yearFrom == from && f.yearTo == to, onClick = { onYears(from, to) })
            }
        }
        val years = remember(currentYear) { (currentYear + 1 downTo 1900).toList() }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            YearDropdown(stringResource(R.string.discover_year_from), f.yearFrom, years) { onYears(it, f.yearTo) }
            Text("–", color = JazzyColors.TextSecondary)
            YearDropdown(stringResource(R.string.discover_year_to), f.yearTo, years) { onYears(f.yearFrom, it) }
        }
    }
}

@Composable
private fun YearDropdown(label: String, value: Int?, years: List<Int>, onSelect: (Int?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        JazzyChip(stringResource(R.string.discover_year_dropdown, label, value?.toString() ?: stringResource(R.string.discover_common_any)), selected = value != null, onClick = { open = true }, trailingIcon = Icons.Filled.ArrowDropDown)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 360.dp)) {
            DropdownMenuItem(text = { Text(stringResource(R.string.discover_common_any)) }, onClick = { open = false; onSelect(null) })
            years.forEach { y ->
                DropdownMenuItem(text = { Text(if (y == value) stringResource(R.string.discover_common_selected, y.toString()) else y.toString()) }, onClick = { open = false; onSelect(y) })
            }
        }
    }
}

@Composable
private fun Section(title: String, trailing: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
            trailing?.invoke()
        }
        content()
    }
}
