package com.englishlearning.app.data.repository

import android.content.Context
import android.speech.tts.TextToSpeech
import com.englishlearning.app.domain.repository.TtsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Implementação do TtsRepository usando Android TextToSpeech.
 */
@Singleton
class TtsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TtsRepository {
    
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var defaultSpeed = 1.0f
    
    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isInitialized = true
            }
        }
    }
    
    override suspend fun speak(text: String, speechRate: Float) {
        if (!isInitialized) {
            // Aguarda inicialização
            suspendCancellableCoroutine<Unit> { cont ->
                val checkInit = object : Runnable {
                    override fun run() {
                        if (isInitialized) {
                            cont.resume(Unit)
                        } else {
                            android.os.Handler(context.mainLooper).postDelayed(this, 100)
                        }
                    }
                }
                android.os.Handler(context.mainLooper).post(checkInit)
            }
        }
        
        tts?.setSpeechRate(speechRate)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }
    
    override fun stop() {
        tts?.stop()
    }
    
    override fun isSpeaking(): Boolean {
        return tts?.isSpeaking == true
    }
    
    override fun setDefaultSpeed(speed: Float) {
        defaultSpeed = speed.coerceIn(0.5f, 2.0f)
    }
    
    override fun getDefaultSpeed(): Float {
        return defaultSpeed
    }
}
