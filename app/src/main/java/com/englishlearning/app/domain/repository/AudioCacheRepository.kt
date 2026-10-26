package com.englishlearning.app.domain.repository

import java.io.File

/**
 * Repository para cache de áudios de pronúncia.
 * 
 * Gerencia o download e armazenamento local de arquivos de áudio
 * para reprodução offline. Usa Free Dictionary API como fonte.
 */
interface AudioCacheRepository {
    
    /**
     * Obtém o arquivo de áudio para uma palavra.
     * Se o áudio já estiver em cache, retorna imediatamente.
     * Caso contrário, tenta baixar da API.
     * 
     * @param word Palavra em inglês
     * @return Arquivo de áudio local ou null se não disponível
     */
    suspend fun getAudioFile(word: String): File?
    
    /**
     * Verifica se o áudio de uma palavra está em cache.
     * 
     * @param word Palavra em inglês
     * @return true se o áudio está disponível offline
     */
    fun isAudioCached(word: String): Boolean
    
    /**
     * Pré-carrega áudios para uma lista de palavras em background.
     * Útil para download em lote quando há Wi-Fi.
     * 
     * @param words Lista de palavras
     * @param onProgress Callback de progresso (downloaded, total)
     */
    suspend fun prefetchAudios(
        words: List<String>,
        onProgress: ((downloaded: Int, total: Int) -> Unit)? = null
    )
    
    /**
     * Limpa o cache de áudios.
     * 
     * @return Espaço liberado em bytes
     */
    suspend fun clearCache(): Long
    
    /**
     * Obtém o tamanho total do cache de áudios.
     * 
     * @return Tamanho em bytes
     */
    fun getCacheSize(): Long
    
    /**
     * Obtém estatísticas do cache.
     */
    fun getCacheStats(): AudioCacheStats
}

/**
 * Estatísticas do cache de áudio.
 */
data class AudioCacheStats(
    val totalFiles: Int,
    val totalSizeBytes: Long,
    val oldestFileDate: Long?,
    val newestFileDate: Long?
)
