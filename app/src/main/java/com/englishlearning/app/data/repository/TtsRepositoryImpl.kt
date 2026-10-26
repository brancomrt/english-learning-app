package com.englishlearning.app.data.repository

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Log
import com.englishlearning.app.domain.repository.AudioCacheRepository
import com.englishlearning.app.domain.repository.TtsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementação do TtsRepository com suporte a cache de áudio nativo.
 * 
 * Prioridade de reprodução:
 * 1. Áudio nativo em cache (MP3 de falantes nativos)
 * 2. Download de áudio nativo da Free Dictionary API
 * 3. Fallback para Android TTS
 * 
 * Isso garante a melhor qualidade de pronúncia possível enquanto
 * mantém funcionamento offline após primeiro uso.
 */
@Singleton
class TtsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioCacheRepository: AudioCacheRepository
) : TtsRepository {
    
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var defaultSpeed = 1.0f
    private var mediaPlayer: MediaPlayer? = null
    
    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isInitialized = true
            }
        }
    }
    
    override suspend fun speak(text: String, speechRate: Float) {
        // Para qualquer reprodução anterior
        stop()
        
        // Aguarda TTS estar pronto
        waitForTtsInit()
        
        tts?.setSpeechRate(speechRate)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }
    
    override suspend fun speakWord(word: String, preferNative: Boolean): Boolean {
        // Para qualquer reprodução anterior
        stop()
        
        if (!preferNative) {
            speak(word)
            return false
        }
        
        // Tenta usar áudio nativo
        return try {
            val audioFile = audioCacheRepository.getAudioFile(word)
            
            if (audioFile != null && audioFile.exists()) {
                // Reproduz áudio nativo
                playAudioFile(audioFile.absolutePath)
                Log.d(TAG, "Playing native audio for '$word'")
                true
            } else {
                // Fallback para TTS
                Log.d(TAG, "No native audio for '$word', using TTS fallback")
                speak(word)
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing native audio for '$word': ${e.message}")
            // Fallback para TTS em caso de erro
            speak(word)
            false
        }
    }
    
    override fun stop() {
        tts?.stop()
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }
    
    override fun isSpeaking(): Boolean {
        return tts?.isSpeaking == true || mediaPlayer?.isPlaying == true
    }
    
    override fun setDefaultSpeed(speed: Float) {
        defaultSpeed = speed.coerceIn(0.5f, 2.0f)
    }
    
    override fun getDefaultSpeed(): Float {
        return defaultSpeed
    }
    
    override fun hasNativeAudio(word: String): Boolean {
        return audioCacheRepository.isAudioCached(word)
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Aguarda TTS estar inicializado.
     */
    private suspend fun waitForTtsInit() {
        if (isInitialized) return
        
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
    
    /**
     * Reproduz arquivo de áudio local.
     */
    private suspend fun playAudioFile(filePath: String) = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<Unit> { cont ->
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                setOnPreparedListener { mp ->
                    mp.start()
                }
                setOnCompletionListener { mp ->
                    mp.release()
                    mediaPlayer = null
                    cont.resume(Unit)
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    mp.release()
                    mediaPlayer = null
                    cont.resumeWithException(Exception("MediaPlayer error: $what"))
                    true
                }
                prepareAsync()
            }
            
            cont.invokeOnCancellation {
                mediaPlayer?.release()
                mediaPlayer = null
            }
        }
    }
    
    companion object {
        private const val TAG = "TtsRepository"
    }
}
