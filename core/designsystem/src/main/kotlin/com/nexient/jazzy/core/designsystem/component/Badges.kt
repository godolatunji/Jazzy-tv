package com.nexient.jazzy.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.model.SourceHealth

enum class BadgeTone { NEUTRAL, ACCENT, WARNING, DANGER, INFO, SUCCESS }

@Composable
fun Badge(text: String, modifier: Modifier = Modifier, tone: BadgeTone = BadgeTone.NEUTRAL, outlined: Boolean = false) {
    val color = when (tone) {
        BadgeTone.NEUTRAL -> JazzyColors.TextSecondary
        BadgeTone.ACCENT -> JazzyColors.Accent
        BadgeTone.WARNING -> JazzyColors.Fair
        BadgeTone.DANGER -> JazzyColors.Poor
        BadgeTone.INFO -> JazzyColors.Info
        BadgeTone.SUCCESS -> JazzyColors.Good
    }
    val shape = RoundedCornerShape(4.dp)
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = if (outlined) color else if (tone == BadgeTone.NEUTRAL) JazzyColors.OnBackground else Color.Black,
        modifier = modifier
            .then(
                if (outlined) Modifier.border(1.dp, color.copy(alpha = 0.7f), shape)
                else Modifier.background(if (tone == BadgeTone.NEUTRAL) JazzyColors.SurfaceVariant else color, shape),
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Green ≥ 50 seeders, amber 10–49, red < 10 (SL-3). */
@Composable
fun HealthDot(health: SourceHealth, modifier: Modifier = Modifier) {
    val color = when (health) {
        SourceHealth.GOOD -> JazzyColors.Good
        SourceHealth.FAIR -> JazzyColors.Fair
        SourceHealth.POOR -> JazzyColors.Poor
    }
    Box(modifier.size(10.dp).background(color, CircleShape))
}

@Composable
fun MaturityBadge(certification: String?, modifier: Modifier = Modifier) {
    if (!certification.isNullOrBlank()) Badge(certification, modifier, outlined = true)
}
