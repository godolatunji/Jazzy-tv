package com.nexient.jazzy.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.R
import com.nexient.jazzy.core.designsystem.theme.JazzyColors

/** 4-digit PIN entry that works by touch and D-pad (PR-5). */
@Composable
fun PinDialog(
    title: String,
    onDismiss: () -> Unit,
    onPinEntered: (String) -> Unit,
    error: String? = null,
) {
    var pin by remember { mutableStateOf("") }
    LaunchedEffect(pin) {
        if (pin.length == 4) {
            onPinEntered(pin)
            pin = ""
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(4) { i ->
                        Box(
                            Modifier.size(16.dp)
                                .background(if (i < pin.length) JazzyColors.OnBackground else JazzyColors.Surface, CircleShape)
                                .border(1.dp, JazzyColors.Outline, CircleShape),
                        )
                    }
                }
                if (error != null) Text(error, color = JazzyColors.Poor, style = MaterialTheme.typography.bodySmall)
                val keys = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫"))
                keys.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { key ->
                            Box(
                                Modifier.size(56.dp).then(
                                    if (key.isEmpty()) Modifier
                                    else Modifier.background(JazzyColors.SurfaceVariant, CircleShape).jazzyClickable(CircleShape) {
                                        pin = if (key == "⌫") pin.dropLast(1) else (pin + key).take(4)
                                    },
                                ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (key == "⌫") Icon(Icons.AutoMirrored.Filled.Backspace, stringResource(R.string.designsystem_pin_delete_digit))
                                else Text(key, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.designsystem_common_cancel)) } },
        containerColor = JazzyColors.SurfaceRaised,
    )
}
