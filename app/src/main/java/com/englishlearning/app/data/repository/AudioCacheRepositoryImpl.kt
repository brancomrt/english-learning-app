package com.englishlearning.app.data.repository

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.englishlearning.app.data.api.FreeDictionaryApi
import com.englishlearning.app.domain.repository.AudioCacheRepository
import com.englishlearning.app.domain.repository.AudioCacheStats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementação do AudioCacheRepository.
 * 
 * Gerencia cache de áudios em disco para reprodução offline.
 * Usa Free Dictionary API para obter URLs de áudio.
 * 
 * Estrutura do cache:
 * /data/data/com.englishlearning.app/cache/audio/
 * ├── hello.mp3
 * ├── world.mp3
 * └── ...
 */
@Singleton
class AudioCacheRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dictionaryApi: FreeDictionaryApi
) : AudioCacheRepository {
    
    private val cacheDir: File by lazy {
        File(context.cacheDir, AUDIO_CACHE_DIR).also { 
            if (!it.exists()) it.mkdirs() 
        }
    }
    
    override suspend fun getAudioFile(word: String): File? {
        val cleanWord = sanitizeFileName(word)
        if (cleanWord.isBlank()) return null
        
        val cacheFile = File(cacheDir, "$cleanWord.mp3")
        
        // 1. Verifica se já está em cache
        if (cacheFile.exists() && cacheFile.length() > 0) {
            Log.d(TAG, "Audio cache HIT for '$word'")
            return cacheFile
        }
        
        // 2. Busca URL de áudio na API
        Log.d(TAG, "Audio cache MISS for '$word', fetching from API...")
        val wordInfo = dictionaryApi.getWordInfo(word)
        val audioUrl = wordInfo?.audioUrl
        
        if (audioUrl.isNullOrBlank()) {
            Log.w(TAG, "No audio URL found for '$word'")
            return null
        }
        
        // 3. Baixa o arquivo de áudio
        return try {
            downloadAudioFile(audioUrl, cacheFile)
            if (cacheFile.exists() && cacheFile.length() > 0) {
                Log.d(TAG, "Audio downloaded and cached for '$word' (${cacheFile.length()} bytes)")
                cacheFile
            } else {
                Log.w(TAG, "Download failed or empty file for '$word'")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading audio for '$word': ${e.message}")
            cacheFile.delete() // Remove arquivo incompleto
            null
        }
    }
    
    override fun isAudioCached(word: String): Boolean {
        val cleanWord = sanitizeFileName(word)
        val cacheFile = File(cacheDir, "$cleanWord.mp3")
        return cacheFile.exists() && cacheFile.length() > 0
    }
    
    override suspend fun prefetchAudios(
        words: List<String>,
        onProgress: ((downloaded: Int, total: Int) -> Unit)?
    ) = withContext(Dispatchers.IO) {
        val uniqueWords = words.distinct()
        var downloaded = 0
        
        for (word in uniqueWords) {
            try {
                if (!isAudioCached(word)) {
                    getAudioFile(word)
                }
                downloaded++
                onProgress?.invoke(downloaded, uniqueWords.size)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to prefetch audio for '$word': ${e.message}")
                downloaded++
                onProgress?.invoke(downloaded, uniqueWords.size)
            }
        }
    }
    
    override suspend fun clearCache(): Long = withContext(Dispatchers.IO) {
        var freedSpace = 0L
        cacheDir.listFiles()?.forEach { file ->
            freedSpace += file.length()
            file.delete()
        }
        Log.i(TAG, "Cache cleared, freed ${freedSpace / 1024} KB")
        freedSpace
    }
    
    override fun getCacheSize(): Long {
        return cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }
    
    override fun getCacheStats(): AudioCacheStats {
        val files = cacheDir.listFiles() ?: emptyArray()
        return AudioCacheStats(
            totalFiles = files.size,
            totalSizeBytes = files.sumOf { it.length() },
            oldestFileDate = files.minOfOrNull { it.lastModified() },
            newestFileDate = files.maxOfOrNull { it.lastModified() }
        )
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Baixa arquivo de áudio da URL para o arquivo local.
     */
    private suspend fun downloadAudioFile(
        audioUrl: String, 
        targetFile: File
    ) = withContext(Dispatchers.IO) {
        val url = URL(audioUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.instanceFollowRedirects = true
        
        try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                throw Exception("HTTP ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }
    
    /**
     * Sanitiza nome de arquivo removendo caracteres inválidos.
     */
    private fun sanitizeFileName(word: String): String {
        return word.lowercase()
            .trim()
            .replace(Regex("[^a-z0-9]"), "_")
            .take(50) // Limita tamanho do nome
    }
    
    companion object {
        private const val TAG = "AudioCacheRepository"
        private const val AUDIO_CACHE_DIR = "audio"
    }
}
