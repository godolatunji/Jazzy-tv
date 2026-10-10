package com.nexient.jazzy.feature.details.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.component.FullScreenLoading
import com.nexient.jazzy.core.designsystem.component.HeroBackdrop
import com.nexient.jazzy.core.designsystem.component.HeroContent
import com.nexient.jazzy.core.designsystem.component.MessageState
import com.nexient.jazzy.core.designsystem.component.SourceRow
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.TitleDetails
import com.nexient.jazzy.feature.details.DetailsActions
import com.nexient.jazzy.feature.details.DetailsUiState
import com.nexient.jazzy.feature.details.DetailsViewModel

/**
 * Phone Details. Portrait: header, then the Sources list (movie) or season + episodes with an
 * episode's Sources in a bottom sheet (show). Expanded width: info left, episodes/sources right.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailsPhoneScreen(state: DetailsUiState, viewModel: DetailsViewModel, actions: DetailsActions, snackbar: SnackbarHostState) {
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = JazzyColors.Background) { padding ->
        val details = state.details.data
        when {
            details == null && state.details.isErrorWithoutData -> MessageState(
                "Couldn't load this title", state.details.error?.message, Modifier.padding(padding),
                actionLabel = "Retry", onAction = viewModel::retryDetails,
            )
            details == null -> FullScreenLoading()
            state.blockedByMaturity -> MessageState("Not available on this profile", "This title is above the profile's maturity setting.", Modifier.padding(padding), actionLabel = "Back", onAction = actions.back)
            else -> BoxWithConstraints(Modifier.fillMaxSize()) {
                if (maxWidth >= 840.dp) ExpandedLayout(details, state, viewModel, actions)
                else PortraitLayout(details, state, viewModel, actions)
            }
        }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onBack,
        modifier = modifier.windowInsetsPadding(WindowInsets.statusBars).padding(8.dp),
        colors = IconButtonDefaults.iconButtonColors(containerColor = JazzyColors.Scrim),
    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
}

internal fun TitleDetails.toHeroContent() = HeroContent(
    key = "$tmdbId", title = title, overview = "", backdropPath = summary.backdropPath, logoPath = null,
    genres = emptyList(), certification = null, meta = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortraitLayout(details: TitleDetails, state: DetailsUiState, viewModel: DetailsViewModel, actions: DetailsActions) {
    val gutter = JazzyTheme.spacing.gutter
    val listState = rememberLazyListState()
    var sheetOpen by remember { mutableStateOf(false) }
    val isShow = details.mediaType == MediaType.TV
    // Arriving from Continue Watching / History / hero "Watch": jump to the Sources list (HM-7).
    LaunchedEffect(state.focusSources, details.tmdbId) {
        if (state.focusSources) {
            if (isShow) sheetOpen = true else listState.animateScrollToItem(2)
        }
    }
    Box {
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item(key = "backdrop") {
                HeroBackdrop(details.toHeroContent(), Modifier.fillMaxWidth().aspectRatio(16f / 10f))
            }
            item(key = "info") {
                DetailsInfo(
                    details, state, viewModel::toggleMyList, viewModel::toggleLike, viewModel::openTrailer,
                    onResumeBanner = { if (isShow) sheetOpen = true else state.sources.rows.firstOrNull { it.flags.lastPlayed }?.let { viewModel.resume(it.source) } },
                    modifier = Modifier.padding(horizontal = gutter),
                )
            }
            if (!isShow) {
                item(key = "sources_header") {
                    SourcesHeader(state.sources, "Sources", viewModel::setFilters, viewModel::refreshSources, Modifier.padding(horizontal = gutter))
                }
                items(state.sources.rows, key = { it.source.key }) { row ->
                    SourceRow(
                        row.source, row.flags,
                        onPlay = { viewModel.play(row.source) },
                        onResume = { viewModel.resume(row.source) }.takeIf { row.flags.resumePositionMs != null },
                        modifier = Modifier.padding(horizontal = gutter),
                    )
                }
                item(key = "sources_empty") { SourcesEmpty(state.sources, viewModel::refreshSources, actions.openSitesSettings) }
            } else {
                item(key = "seasons") {
                    Row(Modifier.padding(horizontal = gutter), verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("Episodes", Modifier.weight(1f))
                        SeasonDropdown(details.seasons, state.selectedSeason, viewModel::selectSeason)
                    }
                }
                item(key = "episodes") {
                    EpisodeList(state, onEpisode = { viewModel.selectEpisode(it); sheetOpen = true }, Modifier.padding(horizontal = gutter - 8.dp), compact = true)
                }
            }
            item(key = "more") { MoreLikeThis(details, actions.openTitle) }
        }
        BackButton(actions.back)
    }
    if (sheetOpen && isShow) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(onDismissRequest = { sheetOpen = false }, sheetState = sheetState, containerColor = JazzyColors.Surface) {
            EpisodeSources(state, viewModel, actions, Modifier.fillMaxHeight(0.85f))
        }
    }
}

/** An episode's Sources list (bottom sheet on phones, side pane on wide screens). */
@Composable
internal fun EpisodeSources(state: DetailsUiState, viewModel: DetailsViewModel, actions: DetailsActions, modifier: Modifier = Modifier) {
    val ep = state.selectedEpisode
    val gutter = JazzyTheme.spacing.gutter
    LazyColumn(modifier, contentPadding = PaddingValues(horizontal = gutter, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(key = "h") {
            SourcesHeader(
                state.sources,
                title = ep?.let { "S${it.seasonNumber}:E${it.episodeNumber} · ${it.name}" } ?: "Sources",
                onFilters = viewModel::setFilters,
                onRefresh = viewModel::refreshSources,
            )
        }
        items(state.sources.rows, key = { it.source.key }) { row ->
            SourceRow(
                row.source, row.flags,
                onPlay = { viewModel.play(row.source) },
                onResume = { viewModel.resume(row.source) }.takeIf { row.flags.resumePositionMs != null },
            )
        }
        item(key = "empty") { SourcesEmpty(state.sources, viewModel::refreshSources, actions.openSitesSettings) }
    }
}

@Composable
private fun ExpandedLayout(details: TitleDetails, state: DetailsUiState, viewModel: DetailsViewModel, actions: DetailsActions) {
    val gutter = JazzyTheme.spacing.gutter
    var showSources by remember { mutableStateOf(details.mediaType == MediaType.MOVIE || state.focusSources) }
    Box(Modifier.fillMaxSize()) {
        HeroBackdrop(details.toHeroContent(), Modifier.fillMaxSize())
        Row(Modifier.fillMaxSize().padding(top = 56.dp)) {
            LazyColumn(Modifier.weight(0.42f).fillMaxHeight(), contentPadding = PaddingValues(gutter), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { DetailsInfo(details, state, viewModel::toggleMyList, viewModel::toggleLike, viewModel::openTrailer, onResumeBanner = { showSources = true }) }
                if (details.mediaType == MediaType.TV) item {
                    SeasonDropdown(details.seasons, state.selectedSeason, { viewModel.selectSeason(it); showSources = false })
                }
                item { CastRow(details) }
            }
            Box(Modifier.weight(0.58f).fillMaxHeight()) {
                if (details.mediaType == MediaType.TV && !showSources) {
                    LazyColumn(contentPadding = PaddingValues(gutter)) {
                        item { SectionTitle("Episodes") }
                        item { EpisodeList(state, onEpisode = { viewModel.selectEpisode(it); showSources = true }) }
                    }
                } else {
                    Column {
                        if (details.mediaType == MediaType.TV) {
                            IconButton(onClick = { showSources = false }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to episodes") }
                        }
                        EpisodeSources(state, viewModel, actions)
                    }
                }
            }
        }
        BackButton(actions.back)
    }
}
