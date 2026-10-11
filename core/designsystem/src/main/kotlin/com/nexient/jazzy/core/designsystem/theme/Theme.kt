package com.nexient.jazzy.core.designsystem.theme

import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Colour tokens (dark-first, spec §8 / plan E1-01). */
object JazzyColors {
    val Background = Color(0xFF0B0B0F)
    val Surface = Color(0xFF16161D)
    val SurfaceRaised = Color(0xFF202029)
    val SurfaceVariant = Color(0xFF2A2A35)
    val Outline = Color(0xFF3A3A47)
    val Accent = Color(0xFFE8384F)
    val AccentMuted = Color(0xFF7A1E2B)
    val OnBackground = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFFB3B3BD)
    val TextTertiary = Color(0xFF7E7E8A)
    val Good = Color(0xFF3DDC84)
    val Fair = Color(0xFFFFB020)
    val Poor = Color(0xFFFF5A5F)
    val Info = Color(0xFF5AA9FF)
    /**
     * D-pad focus ring and focused fills. Not white: white is the primary-button, selected-chip and
     * text colour, so a white ring or highlight disappears against them.
     */
    val Focus = Color(0xFFFFC93C)
    val OnFocus = Color(0xFF14110A)
    val Scrim = Color(0xCC0B0B0F)
}

/** Form factor drives sizes, image sizes and focus treatment. */
enum class FormFactor { PHONE, TABLET, TV }

val LocalFormFactor = staticCompositionLocalOf { FormFactor.PHONE }

@Immutable
data class JazzySpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    /** Horizontal page gutter; TV uses overscan-safe 48 dp (spec §5.10). */
    val gutter: Dp = 16.dp,
    val verticalSafe: Dp = 0.dp,
    val posterWidth: Dp = 112.dp,
    val backdropWidth: Dp = 220.dp,
)

val LocalSpacing = staticCompositionLocalOf { JazzySpacing() }

private val PhoneTypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Black, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp),
    titleLarge = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
)

/** 10-foot UI: everything one step larger. */
private val TvTypography = Typography(
    displaySmall = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Black, lineHeight = 50.sp),
    headlineMedium = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp),
    headlineSmall = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    titleSmall = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
)

private val ColorScheme = darkColorScheme(
    primary = JazzyColors.Accent,
    onPrimary = Color.White,
    primaryContainer = JazzyColors.AccentMuted,
    onPrimaryContainer = Color.White,
    secondary = JazzyColors.OnBackground,
    onSecondary = JazzyColors.Background,
    background = JazzyColors.Background,
    onBackground = JazzyColors.OnBackground,
    surface = JazzyColors.Surface,
    onSurface = JazzyColors.OnBackground,
    surfaceVariant = JazzyColors.SurfaceVariant,
    onSurfaceVariant = JazzyColors.TextSecondary,
    surfaceContainer = JazzyColors.Surface,
    surfaceContainerHigh = JazzyColors.SurfaceRaised,
    surfaceContainerHighest = JazzyColors.SurfaceVariant,
    outline = JazzyColors.Outline,
    error = JazzyColors.Poor,
)

private val TvColorScheme = androidx.tv.material3.darkColorScheme(
    primary = JazzyColors.Accent,
    onPrimary = Color.White,
    secondary = JazzyColors.OnBackground,
    background = JazzyColors.Background,
    onBackground = JazzyColors.OnBackground,
    surface = JazzyColors.Surface,
    onSurface = JazzyColors.OnBackground,
    surfaceVariant = JazzyColors.SurfaceVariant,
    onSurfaceVariant = JazzyColors.TextSecondary,
    inverseSurface = JazzyColors.OnBackground,
    inverseOnSurface = JazzyColors.Background,
    border = JazzyColors.Outline,
)

private val TvRipple = RippleConfiguration(
    color = JazzyColors.Focus,
    rippleAlpha = RippleAlpha(draggedAlpha = 0.16f, focusedAlpha = 0.30f, hoveredAlpha = 0.08f, pressedAlpha = 0.24f),
)

@Composable
fun JazzyTheme(formFactor: FormFactor = FormFactor.PHONE, content: @Composable () -> Unit) {
    val spacing = when (formFactor) {
        FormFactor.TV -> JazzySpacing(gutter = 48.dp, verticalSafe = 27.dp, posterWidth = 150.dp, backdropWidth = 300.dp, lg = 20.dp, xl = 28.dp)
        FormFactor.TABLET -> JazzySpacing(gutter = 24.dp, posterWidth = 132.dp, backdropWidth = 260.dp)
        FormFactor.PHONE -> JazzySpacing()
    }
    CompositionLocalProvider(LocalFormFactor provides formFactor, LocalSpacing provides spacing) {
        MaterialTheme(
            colorScheme = ColorScheme,
            typography = if (formFactor == FormFactor.TV) TvTypography else PhoneTypography,
        ) {
            // Screens without a Scaffold/Surface (most TV layouts) would otherwise draw text black.
            androidx.tv.material3.MaterialTheme(colorScheme = TvColorScheme) {
                CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides JazzyColors.OnBackground,
                    androidx.tv.material3.LocalContentColor provides JazzyColors.OnBackground,
                    // On TV, Material buttons, chips and icon buttons show focus only as a state layer,
                    // which is 10% white by default and unreadable at 10 feet. Tint it with the focus colour.
                    LocalRippleConfiguration provides if (formFactor == FormFactor.TV) TvRipple else LocalRippleConfiguration.current,
                    content = content,
                )
            }
        }
    }
}

object JazzyTheme {
    val spacing: JazzySpacing @Composable get() = LocalSpacing.current
    val formFactor: FormFactor @Composable get() = LocalFormFactor.current
    val isTv: Boolean @Composable get() = LocalFormFactor.current == FormFactor.TV
}
