package id.kasir.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Skema W2 - Biru Baja (dipilih 2026-11-07, menggantikan hijau):
// baja navy untuk pita dan primary, permukaan abu-biru, dan tertiary
// amber hangat sebagai aksen pelengkap (jeda, stok menipis).
private val LightScheme = lightColorScheme(
    primary = Color(0xFF2E5A88),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3F3),
    onPrimaryContainer = Color(0xFF0E2A47),
    secondary = Color(0xFF54687E),
    tertiary = Color(0xFF7C5C10),
    tertiaryContainer = Color(0xFFF7E3AC),
    onTertiaryContainer = Color(0xFF4A3A10),
    background = Color(0xFFF5F7FA),
    surface = Color(0xFFF5F7FA),
    onSurface = Color(0xFF141B24),
    onSurfaceVariant = Color(0xFF5B6B7C),
    surfaceVariant = Color(0xFFE0E7EF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEEF2F7),
    surfaceContainer = Color(0xFFE8EDF3),
    surfaceContainerHigh = Color(0xFFE0E7EF),
    surfaceContainerHighest = Color(0xFFD6DEE8)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF6FA8DC),
    onPrimary = Color(0xFF0A2A47),
    primaryContainer = Color(0xFF1B3A5C),
    onPrimaryContainer = Color(0xFFD7E6F5),
    secondary = Color(0xFF9FB2C8),
    tertiary = Color(0xFFE3C878),
    tertiaryContainer = Color(0xFF4A3A12),
    onTertiaryContainer = Color(0xFFF7E3AC),
    background = Color(0xFF0A0E14),
    surface = Color(0xFF0A0E14),
    onSurface = Color(0xFFE8EEF5),
    onSurfaceVariant = Color(0xFF93A5B8),
    surfaceVariant = Color(0xFF232C38),
    surfaceContainerLowest = Color(0xFF06090E),
    surfaceContainerLow = Color(0xFF11161F),
    surfaceContainer = Color(0xFF161D27),
    surfaceContainerHigh = Color(0xFF1C232E),
    surfaceContainerHighest = Color(0xFF232C38)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KasirTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialExpressiveTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}
