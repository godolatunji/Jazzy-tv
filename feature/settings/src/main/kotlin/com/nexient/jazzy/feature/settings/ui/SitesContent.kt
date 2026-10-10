package com.nexient.jazzy.feature.settings.ui

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.component.Badge
import com.nexient.jazzy.core.designsystem.component.BadgeTone
import com.nexient.jazzy.core.designsystem.component.PillButton
import com.nexient.jazzy.core.designsystem.component.SettingsSwitchRow
import com.nexient.jazzy.core.designsystem.component.jazzyClickable
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.FailureReason
import com.nexient.jazzy.core.model.ProviderAuthKind
import com.nexient.jazzy.core.model.ProviderSearchState
import com.nexient.jazzy.feature.settings.SiteItem
import com.nexient.jazzy.feature.settings.SiteTest
import com.nexient.jazzy.feature.settings.SitesSettingsViewModel
import com.nexient.jazzy.feature.settings.SitesUiState

private val CardShape = RoundedCornerShape(12.dp)

private const val EXPLAINER =
    "Torrent sites change domains often. If a site stops answering, try a mirror URL, but check a mirror before " +
        "you rely on it: some clones return the same unrelated file for every search or redirect to ads. " +
        "Sites that show a CAPTCHA or Cloudflare challenge are reported as \"Blocked by the site\" and skipped; " +
        "Jazzy never tries to bypass them."

@Composable
internal fun SitesPhoneScreen(
    state: SitesUiState,
    viewModel: SitesSettingsViewModel,
    onBack: () -> Unit,
    onEditConnection: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("Torrent sites", style = MaterialTheme.typography.titleLarge)
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = JazzyTheme.spacing.gutter, end = JazzyTheme.spacing.gutter, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "explainer") { Explainer() }
            items(state.items, key = { it.info.id }) { item ->
                Column(Modifier.fillMaxWidth().background(JazzyColors.Surface, CardShape).padding(12.dp)) {
                    SiteHeader(item)
                    Spacer(Modifier.height(4.dp))
                    SiteDetail(item, viewModel, onEditConnection)
                }
            }
        }
    }
}

/** TV: site list on the left (focus selects), the selected site's controls on the right. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SitesTvScreen(state: SitesUiState, viewModel: SitesSettingsViewModel, onEditConnection: (String) -> Unit) {
    if (state.items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No sites available", color = JazzyColors.TextSecondary) }
        return
    }
    var selectedId by rememberSaveable { mutableStateOf(state.items.first().info.id) }
    val selected = state.items.firstOrNull { it.info.id == selectedId } ?: state.items.first()
    val firstSite = remember { FocusRequester() }
    val detailFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstSite.requestFocus() } }

    Row(
        Modifier.fillMaxSize().padding(horizontal = JazzyTheme.spacing.gutter, vertical = JazzyTheme.spacing.verticalSafe),
        horizontalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        Column(
            Modifier.width(340.dp).fillMaxHeight().focusRestorer(firstSite).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Torrent sites", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 20.dp))
            state.items.forEachIndexed { index, item ->
                val isSelected = item.info.id == selected.info.id
                Row(
                    Modifier.fillMaxWidth()
                        .then(if (index == 0) Modifier.focusRequester(firstSite) else Modifier)
                        .heightIn(min = 56.dp)
                        .jazzyClickable(
                            RoundedCornerShape(10.dp), focusScale = 1.03f,
                            onFocused = { if (it) selectedId = item.info.id },
                            onClick = { runCatching { detailFocus.requestFocus() } },
                        )
                        .background(if (isSelected) JazzyColors.SurfaceVariant else JazzyColors.Background, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatusDot(item)
                    Text(item.info.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text(if (item.enabled) "On" else "Off", style = MaterialTheme.typography.labelMedium, color = JazzyColors.TextSecondary)
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(top = 16.dp)) {
            SiteHeader(selected)
            Spacer(Modifier.height(8.dp))
            SiteDetail(selected, viewModel, onEditConnection, firstFocus = detailFocus)
            Spacer(Modifier.height(24.dp))
            Explainer()
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun Explainer() {
    Row(
        Modifier.fillMaxWidth().background(JazzyColors.SurfaceRaised, CardShape).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.Info, null, tint = JazzyColors.Info)
        Text(EXPLAINER, style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextSecondary)
    }
}

@Composable
private fun StatusDot(item: SiteItem) {
    val color = when {
        !item.enabled -> JazzyColors.TextTertiary
        item.notResponding -> JazzyColors.Poor
        item.health?.lastOkAt != null -> JazzyColors.Good
        else -> JazzyColors.Fair
    }
    Box(Modifier.size(10.dp).background(color, CircleShape))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SiteHeader(item: SiteItem) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.info.name, style = MaterialTheme.typography.titleMedium)
            if (item.notResponding && item.enabled) Badge("Not responding", tone = BadgeTone.DANGER)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Badge(if (item.info.isPublic) "Public" else "Private tracker", tone = if (item.info.isPublic) BadgeTone.NEUTRAL else BadgeTone.INFO)
            if (item.info.supportsMovies) Badge("Movies", outlined = true)
            if (item.info.supportsTv) Badge("TV", outlined = true)
            if (item.hasMirror) Badge("Mirror", tone = BadgeTone.WARNING)
        }
        Text(item.effectiveBaseUrl, style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextTertiary)
    }
}

@Composable
private fun SiteDetail(
    item: SiteItem,
    viewModel: SitesSettingsViewModel,
    onEditConnection: (String) -> Unit,
    firstFocus: FocusRequester? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsSwitchRow(
            title = "Enabled",
            subtitle = when {
                item.hasAuth && item.settings?.let { !it.username.isNullOrBlank() || !it.cookies.isNullOrBlank() } != true ->
                    "Needs a login or cookies to search."
                item.info.enabledByDefault -> "On by default."
                else -> "Off by default: blocked or offline when this version shipped."
            },
            checked = item.enabled,
            onCheckedChange = { viewModel.setEnabled(item.info.id, it) },
            focusRequester = firstFocus,
        )
        HealthBlock(item)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PillButton(
                text = if (item.test == SiteTest.Running) "Testing…" else "Test",
                icon = Icons.Filled.NetworkCheck,
                onClick = { viewModel.test(item.info.id) },
                enabled = item.test != SiteTest.Running,
            )
            PillButton(
                text = if (item.hasAuth) "Mirror & login" else "Mirror URL",
                icon = if (item.hasAuth) Icons.Filled.Key else Icons.Filled.Link,
                onClick = { onEditConnection(item.info.id) },
            )
        }
        TestResult(item.test)
    }
}

@Composable
private fun HealthBlock(item: SiteItem) {
    val h = item.health
    val now = System.currentTimeMillis()
    val lines = buildList {
        add(
            "Last success: " + (h?.lastOkAt?.let { DateUtils.getRelativeTimeSpanString(it, now, DateUtils.MINUTE_IN_MILLIS).toString() } ?: "never"),
        )
        h?.avgLatencyMs?.let { add("Average response: $it ms") }
        if ((h?.errorCount ?: 0) > 0) add("Failures in a row: ${h?.errorCount}")
        h?.lastError?.takeIf { it.isNotBlank() }?.let { add("Last error: $it") }
    }
    Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        lines.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextSecondary) }
    }
}

@Composable
private fun TestResult(test: SiteTest?) {
    when (test) {
        null -> Unit
        SiteTest.Running -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = JazzyColors.Accent)
            Text("Running a test search…", style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextSecondary)
        }
        is SiteTest.Finished -> {
            val (text, color) = describe(test.status.state)
            Text("$text · ${test.tookMs} ms", style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}

private fun describe(state: ProviderSearchState): Pair<String, Color> = when (state) {
    is ProviderSearchState.Done -> "Working: ${state.count} results" to JazzyColors.Good
    ProviderSearchState.Searching -> "Still searching" to JazzyColors.TextSecondary
    ProviderSearchState.Unsupported -> "This site doesn't support the test search" to JazzyColors.Fair
    is ProviderSearchState.Failed -> {
        val reason = when (state.reason) {
            FailureReason.BLOCKED -> "Blocked by the site (CAPTCHA or Cloudflare challenge). Jazzy can't bypass this; try a mirror."
            FailureReason.TIMEOUT -> "Timed out"
            FailureReason.UNREACHABLE -> "Couldn't reach the site. The domain may have moved; try a mirror."
            FailureReason.HTTP_ERROR -> "The site returned an error"
            FailureReason.PARSE_ERROR -> "Unexpected page layout. The site may have changed."
            FailureReason.LOGIN_REQUIRED -> "Login required. Add credentials or cookies."
            FailureReason.OTHER -> "Failed"
        }
        (reason + (state.message?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: "")) to JazzyColors.Poor
    }
}

/**
 * Mirror URL override (SRC-4) plus login/cookies for private trackers (SRC-3). Blank fields reset to
 * the default URL or remove the stored secret. [onSave] returns an error message to show, or null.
 */
@Composable
internal fun ConnectionDialog(
    item: SiteItem,
    onSave: (url: String, username: String, password: String, cookies: String) -> String?,
    onDismiss: () -> Unit,
) {
    val current = item.settings
    var url by rememberSaveable { mutableStateOf(current?.baseUrlOverride.orEmpty()) }
    var username by rememberSaveable { mutableStateOf(current?.username.orEmpty()) }
    // Secrets stay out of saved instance state.
    var password by remember { mutableStateOf(current?.password.orEmpty()) }
    var cookies by remember { mutableStateOf(current?.cookies.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    val credentials = ProviderAuthKind.CREDENTIALS in item.info.authKinds
    val cookieAuth = ProviderAuthKind.COOKIES in item.info.authKinds
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JazzyColors.Focus, cursorColor = JazzyColors.Accent)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.info.name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; error = null },
                    label = { Text("Mirror URL") },
                    placeholder = { Text(item.info.defaultBaseUrl) },
                    supportingText = { Text(error ?: "Leave blank to use ${item.info.defaultBaseUrl}") },
                    isError = error != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Check a mirror before using it: some clones return the same unrelated magnet for every result, " +
                        "have no magnets, or redirect to ads.",
                    style = MaterialTheme.typography.bodySmall,
                    color = JazzyColors.TextSecondary,
                )
                if (credentials) {
                    OutlinedTextField(
                        value = username, onValueChange = { username = it }, label = { Text("Username") },
                        singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = password, onValueChange = { password = it }, label = { Text("Password") },
                        singleLine = true, visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = fieldColors, modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (cookieAuth) {
                    OutlinedTextField(
                        value = cookies, onValueChange = { cookies = it }, label = { Text("Cookies") },
                        placeholder = { Text("name=value; name2=value2") },
                        minLines = 2, maxLines = 4, colors = fieldColors, modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (credentials || cookieAuth) {
                    Text(
                        "Logins and cookies are encrypted with a key kept in this device's Android Keystore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = JazzyColors.TextTertiary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { error = onSave(url, username, password, cookies) }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (url.isNotEmpty()) TextButton(onClick = { url = ""; error = null }) { Text("Use default URL") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
        containerColor = JazzyColors.SurfaceRaised,
    )
}
