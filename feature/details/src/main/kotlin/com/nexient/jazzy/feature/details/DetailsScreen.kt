package com.nexient.jazzy.feature.details

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexient.jazzy.core.common.formatDuration
import com.nexient.jazzy.core.designsystem.text.asString
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.PlaybackRequest
import com.nexient.jazzy.core.model.TitleRef
import com.nexient.jazzy.core.model.TorrentSource
import com.nexient.jazzy.feature.details.ui.DetailsPhoneScreen
import com.nexient.jazzy.feature.details.ui.DetailsTvScreen

data class DetailsActions(
    val back: () -> Unit,
    val openTitle: (TitleRef) -> Unit,
    val play: (PlaybackRequest) -> Unit,
    val openSitesSettings: () -> Unit,
)

@Composable
fun DetailsRoute(args: DetailsArgs, actions: DetailsActions) {
    val viewModel = hiltViewModel<DetailsViewModel, DetailsViewModel.Factory>(
        key = "${args.ref.tmdbId}:${args.ref.mediaType}:${args.season}:${args.episode}",
        creationCallback = { it.create(args) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.sources.rows.isNotEmpty() && android.os.Build.VERSION.SDK_INT >= 29) LaunchedEffect(Unit) { android.os.Trace.endAsyncSection("SourcesFirstRow", 0) } // E16-02 marker, begun in app Navigator.push
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var askResume by remember { mutableStateOf<Pair<TorrentSource, Long>?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DetailsEvent.Play -> actions.play(event.request)
                is DetailsEvent.AskResume -> askResume = event.source to event.positionMs
                is DetailsEvent.Message -> snackbar.showSnackbar(event.text.asString(context))
                is DetailsEvent.OpenTrailer -> {
                    val app = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:${event.youtubeKey}"))
                    val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${event.youtubeKey}"))
                    try { context.startActivity(app) } catch (_: ActivityNotFoundException) {
                        runCatching { context.startActivity(web) }
                    }
                }
            }
        }
    }

    askResume?.let { (source, position) ->
        AlertDialog(
            onDismissRequest = { askResume = null },
            title = { Text(stringResource(R.string.details_resume_dialog_title)) },
            text = { Text(stringResource(R.string.details_resume_dialog_message, formatDuration(position))) },
            confirmButton = { TextButton(onClick = { askResume = null; viewModel.launchPlayback(source, position) }) { Text(stringResource(R.string.details_resume_dialog_resume_from, formatDuration(position))) } },
            dismissButton = { TextButton(onClick = { askResume = null; viewModel.launchPlayback(source, 0) }) { Text(stringResource(R.string.details_resume_dialog_start_over)) } },
        )
    }

    if (JazzyTheme.isTv) DetailsTvScreen(state, viewModel, actions, snackbar)
    else DetailsPhoneScreen(state, viewModel, actions, snackbar)
}
