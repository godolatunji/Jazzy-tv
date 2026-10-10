package com.nexient.jazzy.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.R
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme

/*
 * Settings-style rows that work with touch and D-pad: the whole row is the focus target and the
 * centre button toggles / activates it (spec §5.10, §5.11). Trailing switches and radio buttons are
 * purely visual so focus never lands on a tiny control.
 */

private val RowShape = RoundedCornerShape(10.dp)

@Composable
fun SettingsSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = JazzyColors.Accent,
        modifier = modifier.padding(top = 20.dp, bottom = 6.dp, start = 4.dp),
    )
}

@Composable
private fun rowModifier(enabled: Boolean, role: Role, onClick: () -> Unit, focusRequester: FocusRequester?): Modifier {
    val base = Modifier.fillMaxWidth().heightIn(min = if (JazzyTheme.isTv) 64.dp else 56.dp)
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
    return if (enabled) base.jazzyClickable(RowShape, focusScale = 1.02f, role = role, onClick = onClick) else base
}

/** A labelled on/off setting. */
@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
) {
    Row(
        modifier.then(rowModifier(enabled, Role.Switch, { onCheckedChange(!checked) }, focusRequester))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RowText(title, subtitle, enabled, Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = JazzyColors.Accent, checkedThumbColor = Color.White),
        )
    }
}

/** A row that opens something or runs an action; [value] shows the current choice on the right. */
@Composable
fun SettingsActionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    icon: ImageVector? = null,
    destructive: Boolean = false,
    enabled: Boolean = true,
    showChevron: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    Row(
        modifier.then(rowModifier(enabled, Role.Button, onClick, focusRequester)).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = if (destructive) JazzyColors.Poor else JazzyColors.TextSecondary)
        RowText(title, subtitle, enabled, Modifier.weight(1f), if (destructive) JazzyColors.Poor else null)
        if (value != null) Text(value, style = MaterialTheme.typography.bodyMedium, color = JazzyColors.TextSecondary)
        if (showChevron) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = JazzyColors.TextTertiary)
    }
}

@Composable
private fun RowText(title: String, subtitle: String?, enabled: Boolean, modifier: Modifier, titleColor: Color? = null) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = (titleColor ?: JazzyColors.OnBackground).copy(alpha = if (enabled) 1f else 0.4f),
        )
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextSecondary.copy(alpha = if (enabled) 1f else 0.5f))
        }
    }
}

/** Single-choice list dialog; initial focus lands on the current choice (D-pad friendly). */
@Composable
fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedIndex = options.indexOf(selected).coerceAtLeast(0)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                itemsIndexed(options) { index, option ->
                    Row(
                        Modifier.fillMaxWidth()
                            .then(if (index == selectedIndex) Modifier.focusRequester(focus) else Modifier)
                            .jazzyClickable(RowShape, focusScale = 1.02f, role = Role.RadioButton) { onSelect(option) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Text(label(option), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.designsystem_common_cancel)) } },
        containerColor = JazzyColors.SurfaceRaised,
    )
}

/** Yes/no confirmation; the safe (cancel) button gets initial focus. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message, color = JazzyColors.TextSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = if (destructive) JazzyColors.Poor else JazzyColors.Accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.focusRequester(focus)) { Text(stringResource(R.string.designsystem_common_cancel)) } },
        containerColor = JazzyColors.SurfaceRaised,
    )
}

/** Wrapping multi-select chip group. [options] are (id, label) pairs. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToggleChipGroup(
    options: List<Pair<String, String>>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    firstChipFocusRequester: FocusRequester? = null,
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEachIndexed { index, (id, label) ->
            JazzyChip(
                label = label,
                selected = id in selected,
                onClick = { onToggle(id) },
                leadingIcon = if (id in selected) Icons.Filled.Check else null,
                modifier = if (index == 0 && firstChipFocusRequester != null) Modifier.focusRequester(firstChipFocusRequester) else Modifier,
            )
        }
    }
}
