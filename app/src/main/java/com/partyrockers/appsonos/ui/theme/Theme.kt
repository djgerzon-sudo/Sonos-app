package com.partyrockers.appsonos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Modo dark general, como pidió Jonathan: negro, azul, blanco,
// escalas de gris y poco rojo (solo para avisos).
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3D7BFF), // azul
    secondary = Color(0xFF8AA8D8),
    tertiary = Color(0xFF5C6B80),
    background = Color(0xFF0B0E14), // negro
    surface = Color(0xFF161A22),
    surfaceVariant = Color(0xFF232936),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color(0xFFE8EAF0),
    onSurfaceVariant = Color(0xFF9AA3B2), // gris
    error = Color(0xFFE5484D), // rojo, solo avisos
)

@Composable
fun AppSonosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // La app es dark siempre en v1, como pidió Jonathan.
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}
