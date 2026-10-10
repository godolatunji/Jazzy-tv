package com.nexient.jazzy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.navigation.DeepLinks
import com.nexient.jazzy.navigation.DetailsRoute
import com.nexient.jazzy.navigation.JazzyApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** A Watch Next / deep link waiting to be opened once a profile is active. */
    private val pendingLink = mutableStateOf<DetailsRoute?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) pendingLink.value = DeepLinks.parse(intent?.data)
        setContent {
            val formFactor = FormFactors.current(this, LocalConfiguration.current.screenWidthDp)
            JazzyTheme(formFactor) {
                JazzyApp(pendingLink = pendingLink.value, onLinkConsumed = { pendingLink.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        DeepLinks.parse(intent.data)?.let { pendingLink.value = it }
    }
}
