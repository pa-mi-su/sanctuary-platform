package app.sanctuary.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SanctuaryColorScheme = darkColorScheme(
    primary = SanctuaryBlue,
    onPrimary = NightSky,
    background = NightSky,
    onBackground = SanctuaryText,
    surface = SanctuaryCard,
    onSurface = SanctuaryText
)

@Composable
fun SanctuaryTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SanctuaryColorScheme,
        typography = Typography,
        content = content
    )
}

