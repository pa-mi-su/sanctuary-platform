package app.sanctuary.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val SanctuaryColorScheme = darkColorScheme(
    primary = SanctuaryBlue,
    onPrimary = NightSky,
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
