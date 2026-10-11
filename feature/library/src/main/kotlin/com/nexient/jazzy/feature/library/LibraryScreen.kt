package com.nexient.jazzy.feature.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexient.jazzy.core.designsystem.component.PinDialog
import com.nexient.jazzy.core.designsystem.text.asString
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.TitleRef
import com.nexient.jazzy.feature.library.ui.LibraryPhoneScreen
import com.nexient.jazzy.feature.library.ui.LibraryTvScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withTimeoutOrNull

/** How long the Undo snackbar stays up (ML-4, LK-4). */
private const val UNDO_WINDOW_MS = 5_000L

/** Navigation out of Library, plus the tab state shared by both layouts. */
internal class LibraryNav(
    val tab: Int,
    val onTab: (Int) -> Unit,
    val openTitle: (TitleRef) -> Unit,
    val openHistory: (HistoryItem) -> Unit,
)

/**
 * Library (spec §5.6): My List (tab 0), Liked (1) and History (2). History entries open Details
 * on that episode with the sources focused (HS-4).
 */
@Composable
fun LibraryRoute(
    initialTab: Int,
    onOpenTitle: (ref: TitleRef, season: Int?, episode: Int?, focusSources: Boolean) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, LibraryTab.entries.lastIndex)) }
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(viewModel, snackbar) {
        viewModel.messages.collectLatest { message ->
            snackbar.currentSnackbarData?.dismiss()
            val undo = message.undo
            val text = message.text.asString(resources)
            if (undo == null) {
                snackbar.showSnackbar(text, duration = SnackbarDuration.Short)
            } else {
                // Material's durations are 4 s / 10 s; the spec asks for exactly 5 s.
                val result = withTimeoutOrNull(UNDO_WINDOW_MS) {
                    snackbar.showSnackbar(text, actionLabel = resources.getString(R.string.library_common_undo), duration = SnackbarDuration.Indefinite)
                }
                if (result == SnackbarResult.ActionPerformed) viewModel.undo(undo)
            }
        }
    }

    val nav = LibraryNav(
        tab = tab,
        onTab = { tab = it },
        openTitle = { onOpenTitle(it, null, null, false) },
        openHistory = { item ->
            val p = item.entry.latest.playable
            onOpenTitle(item.snapshot.ref, p.season.takeIf { p.isEpisode }, p.episode.takeIf { p.isEpisode }, true)
        },
    )

    Box(Modifier.fillMaxSize()) {
        if (JazzyTheme.isTv) LibraryTvScreen(state, viewModel, nav) else LibraryPhoneScreen(state, viewModel, nav)
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = JazzyTheme.spacing.gutter, vertical = JazzyTheme.spacing.verticalSafe + 8.dp),
        )
    }

    ClearHistoryDialogs(state, viewModel)
}

/** "Clear all history": confirm, then the profile PIN on protected profiles (HS-6). */
@Composable
private fun ClearHistoryDialogs(state: LibraryUiState, viewModel: LibraryViewModel) {
    val name = state.profile?.name
    when (state.clearStep) {
        ClearHistoryStep.HIDDEN -> Unit
        ClearHistoryStep.CONFIRM -> AlertDialog(
            onDismissRequest = viewModel::dismissClearHistory,
            title = { Text(stringResource(R.string.library_history_clear_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.library_history_clear_dialog_message,
                        name ?: stringResource(R.string.library_history_clear_dialog_this_profile),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmClearHistory) {
                    Text(stringResource(R.string.library_history_clear_dialog_confirm), color = JazzyColors.Poor)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissClearHistory) { Text(stringResource(R.string.library_common_cancel)) } },
            containerColor = JazzyColors.SurfaceRaised,
        )
        ClearHistoryStep.PIN -> PinDialog(
            title = if (name != null) {
                stringResource(R.string.library_history_pin_title, name)
            } else {
                stringResource(R.string.library_history_pin_title_no_name)
            },
            onDismiss = viewModel::dismissClearHistory,
            onPinEntered = viewModel::submitPin,
            error = state.pinError?.let { stringResource(it) },
        )
    }
}
