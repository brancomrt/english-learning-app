package com.englishlearning.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Botão de reprodução de áudio TTS.
 *
 * - Quando IDLE: ícone 🔊 estático
 * - Quando SPEAKING: ícone pulsa para indicar reprodução
 * - Quando ERROR/LOADING: ícone desabilitado
 *
 * @param text    Texto a ser falado ao clicar.
 * @param manager TtsManager obtido via rememberTtsManager().
 * @param state   Estado atual do TTS.
 * @param slow    Se true, fala em velocidade reduzida.
 * @param size    Tamanho do ícone.
 */
@Composable
fun TtsButton(
    text: String,
    manager: TtsManager,
    state: TtsState,
    slow: Boolean = false,
    size: Dp = 24.dp
) {
    val isSpeaking = state == TtsState.SPEAKING
    val isEnabled = state == TtsState.IDLE || state == TtsState.SPEAKING

    // Animação de pulso durante a reprodução
    val infiniteTransition = rememberInfiniteTransition(label = "tts_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSpeaking) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    IconButton(
        onClick = {
            if (isSpeaking) manager.stop()
            else manager.speak(text, slow)
        },
        enabled = isEnabled
    ) {
        Icon(
            imageVector = if (isSpeaking) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
            contentDescription = if (isSpeaking) "Parar áudio" else "Ouvir em inglês",
            tint = when (state) {
                TtsState.SPEAKING -> MaterialTheme.colorScheme.primary
                TtsState.IDLE     -> MaterialTheme.colorScheme.onSurface
                else              -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
            modifier = Modifier
                .size(size)
                .scale(if (isSpeaking) scale else 1f)
        )
    }
}
