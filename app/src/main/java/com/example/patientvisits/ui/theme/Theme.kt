package com.example.patientvisits.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Green40,
    onPrimary = Color.White,
    primaryContainer = Cream90,
    onPrimaryContainer = CreamDark,
    secondary = Color(0xFF55624C),
    onSecondary = Color.White,
    secondaryContainer = Sage90,
    onSecondaryContainer = SageDark,
    tertiary = Color(0xFF5E7A10),
    onTertiary = Color.White,
    tertiaryContainer = Lime70,
    onTertiaryContainer = LimeDark,
    background = Color(0xFFFDFCF5),
    onBackground = Color(0xFF1B1C18),
    surface = Color(0xFFFDFCF5),
    onSurface = Color(0xFF1B1C18),
    surfaceVariant = RowAltLight,
    onSurfaceVariant = Color(0xFF44483D),
    outline = Color(0xFF75796C)
)

private val DarkColors = darkColorScheme(
    primary = Green80,
    onPrimary = Color(0xFF123300),
    primaryContainer = Color(0xFF4A3C10),
    onPrimaryContainer = Cream90,
    secondary = Color(0xFFBCCBB0),
    onSecondary = Color(0xFF273421),
    secondaryContainer = Color(0xFF2F4A2A),
    onSecondaryContainer = Sage90,
    tertiary = Color(0xFFC3D87A),
    onTertiary = Color(0xFF2B3600),
    tertiaryContainer = Color(0xFF4F6619),
    onTertiaryContainer = Color(0xFFE3F2B8),
    background = Color(0xFF121410),
    onBackground = Color(0xFFE3E3DA),
    surface = Color(0xFF121410),
    onSurface = Color(0xFFE3E3DA),
    surfaceVariant = RowAltDark,
    onSurfaceVariant = Color(0xFFC5C9BA),
    outline = Color(0xFF8F9285)
)

@Composable
fun PatientVisitsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dynamic (wallpaper) colour is deliberately off so the app keeps the mockup's look.
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
