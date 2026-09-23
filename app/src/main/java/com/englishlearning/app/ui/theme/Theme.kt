package com.englishlearning.app.ui.theme

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

// ---------------------------------------------------------------------------
// Color palette
// ---------------------------------------------------------------------------

private val PrimaryBlue = Color(0xFF1565C0)
private val PrimaryBlueDark = Color(0xFF0D47A1)
private val SecondaryGreen = Color(0xFF2E7D32)
private val TertiaryOrange = Color(0xFFE65100)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E4FF),
    onPrimaryContainer = PrimaryBlueDark,
    secondary = SecondaryGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8E6C9),
    onSecondaryContainer = Color(0xFF1B5E20),
    tertiary = TertiaryOrange,
    onTertiary = Color.White,
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF003065),
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = Color(0xFFD3E4FF),
    secondary = Color(0xFFA5D6A7),
    onSecondary = Color(0xFF1B5E20),
    tertiary = Color(0xFFFFCC80),
    onTertiary = Color(0xFF7A2800),
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5)
)

// ---------------------------------------------------------------------------
// Theme composable
// ---------------------------------------------------------------------------

/**
 * Tema principal do aplicativo baseado em Material Design 3.
 *
 * Suporta:
 * - Dynamic Color no Android 12+ (API 31+).
 * - Tema escuro automático baseado nas preferências do sistema.
 * - Fallback para o esquema de cores estático nos demais dispositivos.
 *
 * Requisito 12.6: seguir diretrizes do Material Design 3 com Jetpack Compose.
 *
 * @param darkTheme      Usa tema escuro se verdadeiro (padrão: preferência do sistema).
 * @param dynamicColor   Habilita dynamic color no Android 12+ (padrão: true).
 * @param content        Conteúdo Compose a ser exibido dentro do tema.
 */
@Composable
fun EnglishLearningTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
        typography = MaterialTheme.typography,
        content = content
    )
}
