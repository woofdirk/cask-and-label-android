package be.webweaver.apps.caskandlabel.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** The amber accent of the iOS app (AccentColor: 0.722, 0.467, 0.129). */
val Amber = Color(0xFFB87721)

private val Light = lightColorScheme(
    primary = Amber,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDB5),
    onPrimaryContainer = Color(0xFF2B1700),
    secondaryContainer = Color(0xFFF6E0C8),
    onSecondaryContainer = Color(0xFF2A1A08),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFF2B866),
    onPrimary = Color(0xFF462A00),
    primaryContainer = Color(0xFF643F00),
    onPrimaryContainer = Color(0xFFFFDDB5),
    secondaryContainer = Color(0xFF55432F),
    onSecondaryContainer = Color(0xFFF6E0C8),
)

@Composable
fun CaskTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
