package com.englishlearning.app.ui.components

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Estado do TTS exposto para a UI.
 */
enum class TtsState { IDLE, LOADING, SPEAKING, ERROR }

/**
 * Gerencia o TextToSpeech do Android para reprodução de áudio em inglês.
 *
 * Usa a engine de TTS nativa do dispositivo (Google TTS), sem custo,
 * sem internet e sem API key.
 *
 * @param onStateChange Callback chamado quando o estado muda.
 */
class TtsManager(
    context: Context,
    private val onStateChange: (TtsState) -> Unit = {}
) {
    private var tts: TextToSpeech? = null
    var isReady = false
        private set

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                isReady = result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED
                onStateChange(if (isReady) TtsState.IDLE else TtsState.ERROR)
            } else {
                onStateChange(TtsState.ERROR)
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onStateChange(TtsState.SPEAKING)
            }
            override fun onDone(utteranceId: String?) {
                onStateChange(TtsState.IDLE)
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onStateChange(TtsState.ERROR)
            }
        })
    }

    /**
     * Fala o texto em inglês americano.
     * @param text Texto a ser falado.
     * @param slow Se true, usa velocidade reduzida (0.7x) para facilitar o aprendizado.
     */
    fun speak(text: String, slow: Boolean = false) {
        if (!isReady || text.isBlank()) return
        tts?.stop()
        tts?.setSpeechRate(if (slow) 0.7f else 1.0f)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
        onStateChange(TtsState.IDLE)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}

/**
 * Composable que cria e gerencia o ciclo de vida do TtsManager.
 * Faz shutdown automático quando o composable sai da composição.
 */
@Composable
fun rememberTtsManager(): Pair<TtsManager, TtsState> {
    val context = LocalContext.current
    var ttsState by remember { mutableStateOf(TtsState.LOADING) }

    val ttsManager = remember {
        TtsManager(context) { state -> ttsState = state }
    }

    DisposableEffect(Unit) {
        onDispose { ttsManager.shutdown() }
    }

    return Pair(ttsManager, ttsState)
}
