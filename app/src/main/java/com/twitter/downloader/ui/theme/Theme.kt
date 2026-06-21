package com.twitter.downloader.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val TwitterBlue = Color(0xFF1DA1F2)
private val DarkGray = Color(0xFF14171A)
private val LightGray = Color(0xFF657786)

private val DarkColorScheme = darkColorScheme(
    primary = TwitterBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0D47A1),
    onPrimaryContainer = Color(0xFFBBDEFB),
    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF003D33),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = LightGray,
    background = Color(0xFF0F1419),
    onBackground = Color(0xFFE1E8ED),
    surface = Color(0xFF1A1F25),
    onSurface = Color(0xFFE1E8ED),
    surfaceVariant = Color(0xFF253341),
    onSurfaceVariant = Color(0xFF8899A6),
    error = Color(0xFFE0245E),
    onError = Color.White,
    errorContainer = Color(0xFF3D0A14),
    onErrorContainer = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary = TwitterBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E8FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF00695C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF002020),
    tertiary = LightGray,
    background = Color(0xFFF5F8FA),
    onBackground = DarkGray,
    surface = Color.White,
    onSurface = DarkGray,
    surfaceVariant = Color(0xFFEAEFF3),
    onSurfaceVariant = Color(0xFF536471),
    error = Color(0xFFE0245E),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun NewXDownloaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
