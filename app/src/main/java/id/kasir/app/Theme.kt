package id.kasir.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = Color(0xFF146C43),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7E4C7),
    onPrimaryContainer = Color(0xFF0B3D24),
    secondary = Color(0xFF4C8055),
    tertiary = Color(0xFF38656A),
    surface = Color(0xFFF7FBF7),
    onSurface = Color(0xFF14201A)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF7BD88F),
    onPrimary = Color(0xFF06391C),
    primaryContainer = Color(0xFF14532D),
    onPrimaryContainer = Color(0xFFB7E4C7),
    secondary = Color(0xFFA7C4A9),
    tertiary = Color(0xFFA0CFD4),
    surface = Color(0xFF0E1511),
    onSurface = Color(0xFFDFEAE2)
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
