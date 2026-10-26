package com.englishlearning.app.data.repository

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.englishlearning.app.domain.repository.SpeechRecognitionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementação do SpeechRecognitionRepository usando Android SpeechRecognizer.
 */
@Singleton
class SpeechRecognitionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SpeechRecognitionRepository {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningState = false
    private val partialResults = MutableStateFlow("")
    private var resultCallback: ((String) -> Unit)? = null
    private var errorCallback: ((Exception) -> Unit)? = null
    
    override suspend fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            throw IllegalStateException("Speech recognition not available")
        }
        
        isListeningState = true
        partialResults.value = ""
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        speechRecognizer?.startListening(intent)
    }
    
    override suspend fun stopListening(): String = suspendCancellableCoroutine { cont ->
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListeningState = false
            }
            
            override fun onError(error: Int) {
                isListeningState = false
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    else -> "Recognition error: $error"
                }
                if (cont.isActive) {
                    cont.resumeWithException(Exception(errorMessage))
                }
            }
            
            override fun onResults(results: Bundle?) {
                isListeningState = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull() ?: ""
                if (cont.isActive) {
                    cont.resume(recognizedText)
                }
            }
            
            override fun onPartialResults(partialResultsBundle: Bundle?) {
                val matches = partialResultsBundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                partialResults.value = matches?.firstOrNull() ?: ""
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        
        speechRecognizer?.stopListening()
        
        cont.invokeOnCancellation {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            isListeningState = false
        }
    }
    
    override suspend fun cancel() {
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListeningState = false
    }
    
    override fun isListening(): Boolean = isListeningState
    
    override fun getPartialResultsFlow(): Flow<String> = partialResults
    
    override suspend fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
}
