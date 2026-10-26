package com.englishlearning.app.domain.repository

/**
 * Repository para Text-to-Speech com suporte a áudio em cache.
 */
interface TtsRepository {
    
    /**
     * Fala o texto fornecido.
     * Usa TTS do Android como padrão.
     */
    suspend fun speak(text: String, speechRate: Float = 1.0f)
    
    /**
     * Fala uma palavra usando áudio de pronúncia nativa se disponível.
     * Fluxo:
     * 1. Verifica cache local
     * 2. Se não tem, tenta baixar da Free Dictionary API
     * 3. Se não conseguir, usa TTS do Android como fallback
     * 
     * @param word Palavra em inglês
     * @param preferNative Se true, prioriza áudio nativo. Se false, usa TTS direto.
     * @return true se usou áudio nativo, false se usou TTS
     */
    suspend fun speakWord(word: String, preferNative: Boolean = true): Boolean
    
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
    
    /**
     * Verifica se uma palavra tem áudio nativo disponível offline.
     */
    fun hasNativeAudio(word: String): Boolean
}
