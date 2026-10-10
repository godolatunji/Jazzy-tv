package com.nexient.jazzy.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Two-step "enter a new PIN, then confirm it" flow on top of [PinDialog] (PR-5).
 * Calls [onPinSet] with the confirmed 4-digit PIN.
 */
@Composable
fun PinSetupDialog(
    title: String,
    onDismiss: () -> Unit,
    onPinSet: (String) -> Unit,
) {
    var first by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val stage = if (first == null) 0 else 1
    key(stage) {
        PinDialog(
            title = if (stage == 0) title else "Confirm the PIN",
            onDismiss = onDismiss,
            error = error,
            onPinEntered = { pin ->
                val entered = first
                if (entered == null) {
                    first = pin
                    error = null
                } else if (entered == pin) {
                    onPinSet(pin)
                } else {
                    first = null
                    error = "The PINs didn't match. Try again."
                }
            },
        )
    }
}
