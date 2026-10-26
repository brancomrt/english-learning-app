package com.englishlearning.app.data.repository

import com.englishlearning.app.data.database.dao.GrammarLessonDao
import com.englishlearning.app.data.database.entity.GrammarExampleEntity
import com.englishlearning.app.data.database.entity.GrammarLessonEntity
import com.englishlearning.app.data.database.entity.GrammarProgressEntity
import com.englishlearning.app.domain.model.GrammarExample
import com.englishlearning.app.domain.model.GrammarLesson
import com.englishlearning.app.domain.model.GrammarProgress
import com.englishlearning.app.domain.model.GrammarStatistics
import com.englishlearning.app.domain.repository.GrammarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementação do GrammarRepository.
 */
@Singleton
class GrammarRepositoryImpl @Inject constructor(
    private val grammarLessonDao: GrammarLessonDao
) : GrammarRepository {
    
    override suspend fun getLessonsByLevel(levelId: Int): List<GrammarLesson> {
        return grammarLessonDao.getLessonsByLevel(levelId).map { it.toDomain() }
    }
    
    override suspend fun getLessonsWithProgress(userId: Long): List<GrammarLesson> {
        // Get all lessons with their progress
        val allLessons = mutableListOf<GrammarLesson>()
        for (levelId in 1..12) {
            val lessonsWithProgress = grammarLessonDao.getLessonsWithProgress(userId, levelId)
            allLessons.addAll(lessonsWithProgress.map { lwp ->
                GrammarLesson(
                    id = lwp.id,
                    levelId = lwp.levelId,
                    grammarTopic = lwp.grammarTopic,
                    title = lwp.title,
                    explanation = lwp.explanation,
                    formula = lwp.formula,
                    iconEmoji = lwp.iconEmoji,
                    isViewed = lwp.isViewed,
                    isCompleted = lwp.isCompleted
                )
            })
        }
        return allLessons
    }
    
    override suspend fun getLessonWithExamples(lessonId: Long): GrammarLesson {
        val lesson = grammarLessonDao.getLessonById(lessonId) 
            ?: throw IllegalArgumentException("Lesson not found: $lessonId")
        val examples = grammarLessonDao.getExamplesByLesson(lessonId)
        return lesson.toDomain().copy(examples = examples.map { it.toDomain() })
    }
    
    override suspend fun getLessonByTopic(topic: String): GrammarLesson? {
        return grammarLessonDao.getLessonByTopic(topic)?.toDomain()
    }
    
    override suspend fun markAsViewed(userId: Long, lessonId: Long) {
        val existing = grammarLessonDao.getProgress(userId, lessonId)
        if (existing == null) {
            grammarLessonDao.saveProgress(
                GrammarProgressEntity(
                    userId = userId,
                    lessonId = lessonId,
                    isViewed = true,
                    viewedAt = System.currentTimeMillis()
                )
            )
        } else {
            grammarLessonDao.markAsViewed(userId, lessonId)
        }
    }
    
    override suspend fun markAsCompleted(userId: Long, lessonId: Long) {
        val existing = grammarLessonDao.getProgress(userId, lessonId)
        if (existing == null) {
            grammarLessonDao.saveProgress(
                GrammarProgressEntity(
                    userId = userId,
                    lessonId = lessonId,
                    isViewed = true,
                    isCompleted = true,
                    viewedAt = System.currentTimeMillis(),
                    completedAt = System.currentTimeMillis()
                )
            )
        } else {
            grammarLessonDao.markAsCompleted(userId, lessonId)
        }
    }
    
    override suspend fun getNextRecommendedLesson(userId: Long, levelId: Int): GrammarLesson? {
        return grammarLessonDao.getNextLesson(userId, levelId)?.toDomain()
    }
    
    override suspend fun getStatistics(userId: Long): GrammarStatistics {
        val allProgress = grammarLessonDao.getAllProgressByUser(userId)
        val viewedCount = allProgress.count { it.isViewed }
        val completedCount = allProgress.count { it.isCompleted }
        
        return GrammarStatistics(
            totalLessons = 0, // TODO: count total
            viewedLessons = viewedCount,
            completedLessons = completedCount,
            averageQuizScore = 0f,
            strongTopics = emptyList(),
            weakTopics = emptyList()
        )
    }
    
    override suspend fun searchLessons(keyword: String): List<GrammarLesson> {
        return grammarLessonDao.searchLessons(keyword).map { it.toDomain() }
    }
    
    override fun getProgressFlow(userId: Long): Flow<List<GrammarProgress>> {
        return grammarLessonDao.getProgressFlow(userId).map { list ->
            list.map { it.toDomain() }
        }
    }
    
    override suspend fun getSuggestionsBasedOnErrors(userId: Long): List<GrammarLesson> {
        // TODO: Implement error-based suggestions
        return emptyList()
    }
    
    private fun GrammarLessonEntity.toDomain(): GrammarLesson {
        return GrammarLesson(
            id = id,
            levelId = levelId,
            grammarTopic = grammarTopic,
            title = title,
            explanation = explanation,
            formula = formula,
            iconEmoji = iconEmoji
        )
    }
    
    private fun GrammarExampleEntity.toDomain(): GrammarExample {
        return GrammarExample(
            id = id,
            sentence = sentence,
            translation = translation,
            highlight = highlight,
            note = note,
            isCorrect = isCorrect
        )
    }
    
    private fun GrammarProgressEntity.toDomain(): GrammarProgress {
        return GrammarProgress(
            lessonId = lessonId,
            isViewed = isViewed,
            isCompleted = isCompleted,
            viewedAt = viewedAt,
            completedAt = completedAt
        )
    }
}
