package app.worn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class WornColors(
    val background: Color,
    val surface: Color,
    val text: Color,
    val secondary: Color,
    val tertiary: Color,
    val accent: Color,
    val wearing: Color,
    val removed: Color,
    val warning: Color,
    val ringTrack: Color,
    val hairline: Color,
)

val LightWornColors = WornColors(
    background = Color(0xFFF6F5F2),
    surface = Color(0xFFFFFFFF),
    text = Color(0xFF1C1C1A),
    secondary = Color(0xFF6F6F6A),
    tertiary = Color(0xFF8A8A84),
    accent = Color(0xFF5E8E8A),
    wearing = Color(0xFF3E8B7A),
    removed = Color(0xFFC4A574),
    warning = Color(0xFFB0894A),
    ringTrack = Color(0xFFE6E4DE),
    hairline = Color(0x1A1C1C1A),
)

val DarkWornColors = WornColors(
    background = Color(0xFF121211),
    surface = Color(0xFF1C1C1A),
    text = Color(0xFFF3F2EE),
    secondary = Color(0xFF9A9A94),
    tertiary = Color(0xFF6E6E68),
    accent = Color(0xFF7AAEA8),
    wearing = Color(0xFF62C2AE),
    removed = Color(0xFFD4B48A),
    warning = Color(0xFFC4A574),
    ringTrack = Color(0xFF2A2A27),
    hairline = Color(0x22F3F2EE),
)

val LocalWornColors = staticCompositionLocalOf { LightWornColors }

val WornTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 64.sp,
        letterSpacing = (-1.5).sp,
        lineHeight = 68.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        letterSpacing = 0.2.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 1.4.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp,
    ),
)

@Composable
fun WornTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkWornColors else LightWornColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = Color.White,
            background = colors.background,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            secondary = colors.secondary,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = Color.White,
            background = colors.background,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            secondary = colors.secondary,
        )
    }
    CompositionLocalProvider(LocalWornColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = WornTypography,
            content = content,
        )
    }
}

object WornTheme {
    val colors: WornColors
        @Composable get() = LocalWornColors.current
}
