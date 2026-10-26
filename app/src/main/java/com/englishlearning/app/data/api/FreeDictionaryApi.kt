package com.englishlearning.app.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cliente para a Free Dictionary API.
 * https://dictionaryapi.dev/
 * 
 * API 100% gratuita, sem necessidade de chave.
 * Retorna definições, fonética e URLs de áudio para palavras em inglês.
 */
@Singleton
class FreeDictionaryApi @Inject constructor() {
    
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    
    /**
     * Busca informações de uma palavra, incluindo URL de áudio.
     * 
     * @param word Palavra em inglês
     * @return WordInfo com dados da palavra ou null se não encontrada
     */
    suspend fun getWordInfo(word: String): WordInfo? = withContext(Dispatchers.IO) {
        try {
            val cleanWord = word.lowercase().trim().replace(Regex("[^a-z]"), "")
            if (cleanWord.isBlank()) return@withContext null
            
            val url = URL("$BASE_URL/$cleanWord")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val entries = json.decodeFromString<List<DictionaryEntry>>(response)
                
                // Extrai informações relevantes
                val entry = entries.firstOrNull() ?: return@withContext null
                
                // Procura por áudio (prioriza UK, depois US)
                val audioUrl = entry.phonetics
                    .mapNotNull { it.audio }
                    .filter { it.isNotBlank() }
                    .firstOrNull { it.contains("uk") || it.contains("gb") }
                    ?: entry.phonetics.mapNotNull { it.audio }.filter { it.isNotBlank() }.firstOrNull()
                
                // Extrai fonética IPA
                val phonetic = entry.phonetic 
                    ?: entry.phonetics.mapNotNull { it.text }.firstOrNull()
                
                // Extrai definição principal
                val definition = entry.meanings.firstOrNull()?.definitions?.firstOrNull()?.definition
                
                // Extrai part of speech
                val partOfSpeech = entry.meanings.firstOrNull()?.partOfSpeech
                
                WordInfo(
                    word = entry.word,
                    phonetic = phonetic,
                    audioUrl = audioUrl?.let { normalizeUrl(it) },
                    definition = definition,
                    partOfSpeech = partOfSpeech
                )
            } else {
                Log.w(TAG, "Word not found: $word (HTTP ${connection.responseCode})")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching word info for '$word': ${e.message}")
            null
        }
    }
    
    /**
     * Normaliza URL de áudio (adiciona https: se necessário).
     */
    private fun normalizeUrl(url: String): String {
        return when {
            url.startsWith("//") -> "https:$url"
            url.startsWith("http") -> url
            else -> "https://$url"
        }
    }
    
    companion object {
        private const val TAG = "FreeDictionaryApi"
        private const val BASE_URL = "https://api.dictionaryapi.dev/api/v2/entries/en"
    }
}

/**
 * Informações simplificadas de uma palavra.
 */
data class WordInfo(
    val word: String,
    val phonetic: String?,
    val audioUrl: String?,
    val definition: String?,
    val partOfSpeech: String?
)

// ═══════════════════════════════════════════════════════════════════════════
// Modelos de resposta da API
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
data class DictionaryEntry(
    val word: String,
    val phonetic: String? = null,
    val phonetics: List<Phonetic> = emptyList(),
    val meanings: List<Meaning> = emptyList(),
    val origin: String? = null
)

@Serializable
data class Phonetic(
    val text: String? = null,
    val audio: String? = null,
    @SerialName("sourceUrl")
    val sourceUrl: String? = null
)

@Serializable
data class Meaning(
    val partOfSpeech: String,
    val definitions: List<Definition> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList()
)

@Serializable
data class Definition(
    val definition: String,
    val example: String? = null,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList()
)
