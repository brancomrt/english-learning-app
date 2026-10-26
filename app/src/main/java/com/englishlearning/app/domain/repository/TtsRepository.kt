package com.englishlearning.app.domain.repository

/**
 * Repository para Text-to-Speech.
 */
interface TtsRepository {
    
    /**
     * Fala o texto fornecido.
     */
    suspend fun speak(text: String, speechRate: Float = 1.0f)
    
    /**
     * Para a fala atual.
     */
    fun stop()
    
    /**
     * Verifica se está falando.
     */
    fun isSpeaking(): Boolean
    
    /**
     * Define velocidade padrão de fala.
     */
    fun setDefaultSpeed(speed: Float)
    
    /**
     * Obtém velocidade padrão de fala.
     */
    fun getDefaultSpeed(): Float
}
