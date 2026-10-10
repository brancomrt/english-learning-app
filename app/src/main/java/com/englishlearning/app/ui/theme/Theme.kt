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
// Color palette — Modern Educational (Indigo · Emerald · Amber)
// ---------------------------------------------------------------------------

// Primary: Indigo vibrante
private val Indigo600   = Color(0xFF4F46E5)
private val Indigo800   = Color(0xFF3730A3)
private val Indigo50    = Color(0xFFEEF2FF)
// Secondary: Esmeralda (vocabulário, progresso)
private val Emerald500  = Color(0xFF10B981)
private val Emerald800  = Color(0xFF065F46)
private val Emerald50   = Color(0xFFD1FAE5)
// Tertiary: Âmbar (conquistas, revisões)
private val Amber500    = Color(0xFFF59E0B)
private val Amber900    = Color(0xFF78350F)
private val Amber50     = Color(0xFFFEF3C7)

private val LightColorScheme = lightColorScheme(
    primary              = Indigo600,
    onPrimary            = Color.White,
    primaryContainer     = Indigo50,
    onPrimaryContainer   = Indigo800,
    secondary            = Emerald500,
    onSecondary          = Color.White,
    secondaryContainer   = Emerald50,
    onSecondaryContainer = Emerald800,
    tertiary             = Amber500,
    onTertiary           = Color.White,
    tertiaryContainer    = Amber50,
    onTertiaryContainer  = Amber900,
    background           = Color(0xFFF8FAFC),
    surface              = Color.White,
    surfaceVariant       = Color(0xFFF1F5F9),
    onSurface            = Color(0xFF0F172A),
    onSurfaceVariant     = Color(0xFF64748B),
    outline              = Color(0xFFCBD5E1)
)

private val DarkColorScheme = darkColorScheme(
    primary              = Color(0xFFA5B4FC),   // indigo-300
    onPrimary            = Color(0xFF1E1B4B),
    primaryContainer     = Indigo800,
    onPrimaryContainer   = Color(0xFFE0E7FF),
    secondary            = Color(0xFF6EE7B7),   // emerald-300
    onSecondary          = Color(0xFF064E3B),
    tertiary             = Color(0xFFFCD34D),   // amber-300
    onTertiary           = Color(0xFF451A03),
    background           = Color(0xFF0F172A),   // slate-900
    surface              = Color(0xFF1E293B),   // slate-800
    surfaceVariant       = Color(0xFF334155),   // slate-700
    onSurface            = Color(0xFFF1F5F9),
    onSurfaceVariant     = Color(0xFF94A3B8),
    outline              = Color(0xFF475569)
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
    // false = sempre usa a paleta customizada (não depende do wallpaper do Android 12+)
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
        typography = MaterialTheme.typography,
        content = content
    )
}
