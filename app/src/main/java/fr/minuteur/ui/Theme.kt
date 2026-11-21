package fr.minuteur.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

val Cream = Color(0xFFFFF8F0)
val Ink = Color(0xFF2B2118)
val InkSoft = Color(0xFF6B5E55)
val Coral = Color(0xFFFF6B4A)

data class TimerColors(val container: Color, val accent: Color)

/** Une couleur par minuteur, pour les repérer d'un coup d'œil. */
val TimerPalette = listOf(
    TimerColors(Color(0xFFFFE1D6), Color(0xFFFF6B4A)), // corail
    TimerColors(Color(0xFFFFF0C2), Color(0xFFE09B00)), // moutarde
    TimerColors(Color(0xFFD7F5DD), Color(0xFF2EAD5B)), // vert
    TimerColors(Color(0xFFD3F1F4), Color(0xFF0E9AAB)), // turquoise
    TimerColors(Color(0xFFDDE6FF), Color(0xFF4A6CF7)), // bleu
    TimerColors(Color(0xFFEBDDFF), Color(0xFF8E5CF0)), // violet
    TimerColors(Color(0xFFFFDDEE), Color(0xFFE8508F)), // rose
)

private val Scheme = lightColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE1D6),
    onPrimaryContainer = Color(0xFF3A1206),
    secondary = Color(0xFF4A6CF7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFEBDD),
    onSecondaryContainer = Ink,
    background = Cream,
    onBackground = Ink,
    surface = Cream,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF3E7DC),
    onSurfaceVariant = InkSoft,
    outline = Color(0xFFD9C9BC),
    outlineVariant = Color(0xFFEADCD0),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFBF7),
    surfaceContainer = Color(0xFFFFF4EA),
    surfaceContainerHigh = Color(0xFFFCEEE2),
    surfaceContainerHighest = Color(0xFFF7E7DA),
)

private val BaseTypography = Typography()

private val AppTypography = BaseTypography.copy(
    headlineLarge = BaseTypography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
    headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun MinuteurTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = AppTypography, content = content)
}
