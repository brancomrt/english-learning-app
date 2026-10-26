package com.englishlearning.app.domain.repository

import com.englishlearning.app.domain.model.Flashcard
import com.englishlearning.app.domain.model.FlashcardRating
import com.englishlearning.app.domain.model.FlashcardStatistics
import kotlinx.coroutines.flow.Flow

/**
 * Repository para flashcards de vocabulário.
 */
interface FlashcardRepository {
    
    /**
     * Obtém flashcards devidos para revisão hoje.
     */
    suspend fun getDueCards(userId: Long, today: String, limit: Int = 50): List<Flashcard>
    
    /**
     * Obtém contagem de cards devidos.
     */
    suspend fun getDueCount(userId: Long, today: String): Int
    
    /**
     * Obtém novos flashcards (nunca revisados).
     */
    suspend fun getNewCards(userId: Long, limit: Int = 20): List<Flashcard>
    
    /**
     * Obtém contagem de novos cards.
     */
    suspend fun getNewCount(userId: Long): Int
    
    /**
     * Revisa um flashcard e atualiza SRS.
     */
    suspend fun reviewCard(cardId: Long, rating: FlashcardRating)
    
    /**
     * Obtém flashcard por ID.
     */
    suspend fun getById(id: Long): Flashcard?
    
    /**
     * Obtém flashcards por nível.
     */
    suspend fun getByLevel(levelId: Int): List<Flashcard>
    
    /**
     * Busca flashcards por palavra ou definição.
     */
    suspend fun search(userId: Long, query: String, limit: Int = 50): List<Flashcard>
    
    /**
     * Obtém estatísticas de flashcards.
     */
    suspend fun getStatistics(userId: Long): FlashcardStatistics
    
    /**
     * Reseta progresso de todos os flashcards.
     */
    suspend fun resetProgress(userId: Long, today: String)
    
    /**
     * Extrai vocabulário dos drills e cria flashcards.
     */
    suspend fun extractVocabularyFromDrills()
    
    /**
     * Flow de contagem de cards devidos.
     */
    fun getDueCountFlow(userId: Long, today: String): Flow<Int>
}
