package com.nexient.jazzy.feature.search.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * Voice input for Search (SR-2): launches the system recognizer (`ACTION_RECOGNIZE_SPEECH`) and
 * hands back the best transcript. On TV this is the same recognizer the remote's mic uses.
 * Returns null when no recognizer is installed, so the mic button can be hidden.
 */
@Composable
internal fun rememberVoiceSearch(onResult: (String) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val callback = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.takeIf { it.isNotBlank() }?.let {
            callback.value(it)
        }
    }
    val available = remember(context) {
        runCatching { context.packageManager.resolveActivity(speechIntent(), 0) != null }.getOrDefault(false)
    }
    if (!available) return null
    return {
        try {
            launcher.launch(speechIntent())
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Voice search isn't available on this device", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun speechIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
    putExtra(RecognizerIntent.EXTRA_PROMPT, "Search movies, shows and people")
    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
}
