package com.englishlearning.app.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository para reconhecimento de fala.
 */
interface SpeechRecognitionRepository {
    
    /**
     * Inicia escuta de áudio para reconhecimento.
     */
    suspend fun startListening()
    
    /**
     * Para escuta e retorna texto reconhecido.
     */
    suspend fun stopListening(): String
    
    /**
     * Cancela escuta sem processar.
     */
    suspend fun cancel()
    
    /**
     * Verifica se está escutando.
     */
    fun isListening(): Boolean
    
    /**
     * Flow de texto parcial enquanto escuta.
     */
    fun getPartialResultsFlow(): Flow<String>
    
    /**
     * Verifica se o reconhecimento de fala está disponível.
     */
    suspend fun isAvailable(): Boolean
}
