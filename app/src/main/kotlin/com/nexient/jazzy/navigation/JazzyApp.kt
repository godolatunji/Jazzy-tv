package com.nexient.jazzy.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.nexient.jazzy.AppViewModel
import com.nexient.jazzy.R
import com.nexient.jazzy.StartDestination
import com.nexient.jazzy.core.designsystem.component.FullScreenLoading
import com.nexient.jazzy.core.designsystem.theme.JazzyColors

/** Root composable: waits for the launch decision, shows the disclaimer once, then the nav host. */
@Composable
fun JazzyApp(
    pendingLink: DetailsRoute? = null,
    onLinkConsumed: () -> Unit = {},
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val launch by appViewModel.launch.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().background(JazzyColors.Background)) {
        val start = launch.start
        if (start == null) {
            FullScreenLoading()
        } else {
            val startKey: NavKey = when (start) {
                StartDestination.FIRST_PROFILE -> ProfileEditRoute(firstRun = true)
                StartDestination.PICKER -> ProfilePickerRoute
                StartDestination.HOME -> HomeRoute
            }
            JazzyNavHost(startKey, appViewModel, pendingLink, onLinkConsumed)
        }
        if (!launch.disclaimerAccepted && start != null) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.app_disclaimer_title)) },
                text = {
                    Text(stringResource(R.string.app_disclaimer_message))
                },
                confirmButton = { TextButton(onClick = appViewModel::acceptDisclaimer) { Text(stringResource(R.string.app_disclaimer_accept)) } },
            )
        }
    }
}
