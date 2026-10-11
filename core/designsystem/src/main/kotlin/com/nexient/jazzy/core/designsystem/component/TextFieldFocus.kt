package com.nexient.jazzy.core.designsystem.component

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme

/**
 * Text fields swallow D-pad Up/Down, trapping TV focus (spec §5.10: focus never gets trapped).
 * On TV, Up/Down move focus out of the field instead; Left/Right still move the cursor.
 */
@Composable
fun Modifier.dpadFocusEscape(): Modifier {
    if (!JazzyTheme.isTv) return this
    val focusManager = LocalFocusManager.current
    return this.onPreviewKeyEvent { e ->
        if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (e.key.nativeKeyCode) {
            AndroidKeyEvent.KEYCODE_DPAD_DOWN -> focusManager.moveFocus(FocusDirection.Down)
            AndroidKeyEvent.KEYCODE_DPAD_UP -> focusManager.moveFocus(FocusDirection.Up)
            else -> false
        }
    }
}
