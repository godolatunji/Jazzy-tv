package com.nexient.jazzy.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The bundled avatar set (24, PR-2): gradient tiles with a glyph. Placeholder art until design
 * delivers the final set; IDs are stable so profiles keep their avatar.
 */
object Avatars {
    private val palettes = listOf(
        0xFFE8384F to 0xFF7A1E2B, 0xFF5AA9FF to 0xFF1D3F7A, 0xFF3DDC84 to 0xFF0F5A34, 0xFFFFB020 to 0xFF7A4A00,
        0xFFB35CFF to 0xFF4A1A7A, 0xFFFF7AC6 to 0xFF7A1F55, 0xFF00C2C7 to 0xFF00585A, 0xFFFF8A3D to 0xFF7A3510,
    )
    private val glyphs = listOf("★", "☾", "♪", "✿", "◆", "☀", "♞", "✈", "⚡", "☁", "♣", "❄", "☂", "✦", "♥", "⚓",
        "☘", "♛", "✪", "☯", "♬", "⚑", "✧", "◎")

    fun colors(id: Int): Pair<Color, Color> = palettes[id.mod(palettes.size)].let { Color(it.first) to Color(it.second) }
    fun glyph(id: Int): String = glyphs[id.mod(glyphs.size)]
}

@Composable
fun AvatarTile(avatarId: Int, modifier: Modifier = Modifier, size: Dp = 96.dp) {
    val (a, b) = Avatars.colors(avatarId)
    Box(
        modifier.size(size).background(Brush.linearGradient(listOf(a, b)), RoundedCornerShape(size / 8)),
        contentAlignment = Alignment.Center,
    ) {
        Text(Avatars.glyph(avatarId), fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
