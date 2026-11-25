package app.sanctuary.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val SanctuaryColorScheme = lightColorScheme(
    primary = SanctuaryBlue,
    onPrimary = SanctuaryOnAccent,
    primaryContainer = DeepOcean,
    onPrimaryContainer = SanctuaryText,
    secondary = SanctuaryTabActive,
    onSecondary = SanctuaryOnAccent,
    secondaryContainer = SanctuaryCardSoft,
    onSecondaryContainer = SanctuaryText,
    surfaceVariant = SanctuaryCardSoft,
    onSurfaceVariant = SanctuaryMuted,
    outline = SanctuaryCardBorder,
    outlineVariant = SanctuaryCardBorder,
    surfaceTint = androidx.compose.ui.graphics.Color.Transparent,
    error = SanctuaryError,
    onError = SanctuaryOnAccent,
    errorContainer = SanctuaryErrorSurface,
    onErrorContainer = SanctuaryError,
    background = NightSky,
    onBackground = SanctuaryText,
    surface = SanctuaryCard,
    onSurface = SanctuaryText
)

private val SanctuaryShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun SanctuaryTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SanctuaryColorScheme,
        typography = Typography,
        shapes = SanctuaryShapes,
        content = content
    )
}
