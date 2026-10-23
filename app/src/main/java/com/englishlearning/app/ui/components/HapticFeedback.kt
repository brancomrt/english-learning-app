package com.englishlearning.app.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * Tipos de feedback háptico disponíveis no app.
 */
enum class HapticType {
    /** Feedback leve para cliques normais */
    LIGHT,
    /** Feedback médio para confirmações */
    MEDIUM,
    /** Feedback forte para erros */
    HEAVY,
    /** Feedback de sucesso (padrão específico) */
    SUCCESS,
    /** Feedback de erro (padrão específico) */
    ERROR,
    /** Feedback de seleção */
    SELECTION
}

/**
 * Controller para feedback háptico.
 *
 * Fornece vibração tátil para diferentes ações no app,
 * melhorando a experiência do usuário com feedback físico.
 */
class HapticFeedbackController(
    private val context: Context,
    private val view: View
) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * Executa feedback háptico do tipo especificado.
     */
    fun perform(type: HapticType) {
        when (type) {
            HapticType.LIGHT -> performLight()
            HapticType.MEDIUM -> performMedium()
            HapticType.HEAVY -> performHeavy()
            HapticType.SUCCESS -> performSuccess()
            HapticType.ERROR -> performError()
            HapticType.SELECTION -> performSelection()
        }
    }

    private fun performLight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } else {
            vibrate(10)
        }
    }

    private fun performMedium() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            vibrate(30)
        }
    }

    private fun performHeavy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            vibrate(50)
        }
    }

    private fun performSuccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            // Padrão de sucesso: curto-pausa-curto
            vibratePattern(longArrayOf(0, 20, 50, 20))
        }
    }

    private fun performError() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            // Padrão de erro: longo
            vibrate(100)
        }
    }

    private fun performSelection() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun vibrate(durationMs: Long) {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(
                    VibrationEffect.createOneShot(
                        durationMs,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(durationMs)
            }
        }
    }

    private fun vibratePattern(pattern: LongArray) {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(
                    VibrationEffect.createWaveform(pattern, -1)
                )
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(pattern, -1)
            }
        }
    }
}

/**
 * Composable que fornece acesso ao controller de feedback háptico.
 *
 * Uso:
 * ```kotlin
 * val haptic = rememberHapticFeedback()
 *
 * Button(onClick = {
 *     haptic.perform(HapticType.SUCCESS)
 *     // ... ação
 * }) { ... }
 * ```
 */
@Composable
fun rememberHapticFeedback(): HapticFeedbackController {
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view) {
        HapticFeedbackController(context, view)
    }
}
