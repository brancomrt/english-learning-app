package com.englishlearning.app.domain.repository

import com.englishlearning.app.domain.model.GrammarLesson
import com.englishlearning.app.domain.model.GrammarProgress
import com.englishlearning.app.domain.model.GrammarStatistics
import kotlinx.coroutines.flow.Flow

/**
 * Repository para lições de gramática.
 */
interface GrammarRepository {
    
    /**
     * Obtém todas as lições de um nível.
     */
    suspend fun getLessonsByLevel(levelId: Int): List<GrammarLesson>
    
    /**
     * Obtém lições com progresso do usuário.
     */
    suspend fun getLessonsWithProgress(userId: Long): List<GrammarLesson>
    
    /**
     * Obtém lição completa com exemplos.
     */
    suspend fun getLessonWithExamples(lessonId: Long): GrammarLesson
    
    /**
     * Obtém lição por tópico gramatical.
     */
    suspend fun getLessonByTopic(topic: String): GrammarLesson?
    
    /**
     * Marca lição como visualizada.
     */
    suspend fun markAsViewed(userId: Long, lessonId: Long)
    
    /**
     * Marca lição como completa.
     */
    suspend fun markAsCompleted(userId: Long, lessonId: Long)
    
    /**
     * Obtém próxima lição recomendada.
     */
    suspend fun getNextRecommendedLesson(userId: Long, levelId: Int): GrammarLesson?
    
    /**
     * Obtém estatísticas de gramática.
     */
    suspend fun getStatistics(userId: Long): GrammarStatistics
    
    /**
     * Busca lições por palavra-chave.
     */
    suspend fun searchLessons(keyword: String): List<GrammarLesson>
    
    /**
     * Flow de progresso do usuário.
     */
    fun getProgressFlow(userId: Long): Flow<List<GrammarProgress>>
    
    /**
     * Sugere lições baseado em erros frequentes do usuário.
     */
    suspend fun getSuggestionsBasedOnErrors(userId: Long): List<GrammarLesson>
}
