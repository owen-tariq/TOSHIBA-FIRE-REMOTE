package com.owentariq.emberlink.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Ember: warm orange against near-black, like a remote lit by the TV. */
val Ember = Color(0xFFFF7A18)
val EmberDim = Color(0xFFB4530E)
val Charcoal = Color(0xFF121316)
val KeyFace = Color(0xFF25272D)
val KeyFaceHot = Color(0xFF34373F)
val RingFace = Color(0xFF1B1D22)
val TextPrimary = Color(0xFFF2F3F5)
val TextMuted = Color(0xFF9BA1AC)

val NetflixRed = Color(0xFFE50914)
val PrimeBlue = Color(0xFF00A8E1)
val HboPurple = Color(0xFF7B2FF7)
val VueBlue = Color(0xFF2E6DE6)
val DisneyBlue = Color(0xFF1B44A7)
val HuluGreen = Color(0xFF1CE783)

private val DarkColors = darkColorScheme(
    primary = Ember,
    onPrimary = Color.Black,
    secondary = EmberDim,
    background = Charcoal,
    onBackground = TextPrimary,
    surface = Charcoal,
    onSurface = TextPrimary,
    surfaceVariant = KeyFace,
    onSurfaceVariant = TextMuted,
    error = Color(0xFFFF5449),
)

private val LightColors = lightColorScheme(
    primary = EmberDim,
    background = Color(0xFFF6F6F7),
    surface = Color(0xFFFFFFFF),
)

private val EmberTypography = Typography(
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
    bodySmall = TextStyle(fontSize = 12.sp),
)

@Composable
fun EmberlinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        // The remote is a dark object. Force the dark scheme regardless of system setting;
        // a white remote glowing in a dark living room is nobody's friend.
        colorScheme = DarkColors,
        typography = EmberTypography,
        content = content,
    )
}
