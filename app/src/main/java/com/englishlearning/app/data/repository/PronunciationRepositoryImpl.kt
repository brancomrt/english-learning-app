package com.englishlearning.app.data.repository

import com.englishlearning.app.data.database.dao.ExerciseDao
import com.englishlearning.app.data.database.dao.PronunciationDao
import com.englishlearning.app.data.database.entity.PronunciationResultEntity
import com.englishlearning.app.data.database.entity.PronunciationStatsEntity
import com.englishlearning.app.domain.model.ImprovementTrend
import com.englishlearning.app.domain.model.PronunciationBreakdown
import com.englishlearning.app.domain.model.PronunciationExerciseType
import com.englishlearning.app.domain.model.PronunciationResult
import com.englishlearning.app.domain.model.PronunciationStatistics
import com.englishlearning.app.domain.model.ScoreDistribution
import com.englishlearning.app.domain.repository.PronunciationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementação do PronunciationRepository.
 */
@Singleton
class PronunciationRepositoryImpl @Inject constructor(
    private val pronunciationDao: PronunciationDao,
    private val exerciseDao: ExerciseDao
) : PronunciationRepository {
    
    override suspend fun getSentencesForPractice(levelId: Int, count: Int): List<String> {
        // Get exercises from specified level for pronunciation practice
        val exercises = exerciseDao.getByLevel(levelId).take(count)
        return exercises.map { it.answerText }
    }
    
    override suspend fun saveResult(userId: Long, result: PronunciationResult) {
        val entity = PronunciationResultEntity(
            userId = userId,
            exerciseId = result.exerciseId,
            exerciseType = result.exerciseType.name,
            expectedText = result.expectedText,
            recognizedText = result.recognizedText,
            overallScore = result.overallScore,
            accuracyScore = result.breakdown.accuracyScore,
            fluencyScore = result.breakdown.fluencyScore,
            completenessScore = result.breakdown.completenessScore,
            errorWords = result.wordAnalysis
                .filter { it.status.name == "MISPRONOUNCED" }
                .joinToString(",") { it.word },
            omittedWords = result.wordAnalysis
                .filter { it.status.name == "OMITTED" }
                .joinToString(",") { it.word },
            insertedWords = result.wordAnalysis
                .filter { it.status.name == "INSERTED" }
                .joinToString(",") { it.word },
            audioDurationMs = result.audioDurationMs,
            confidence = result.confidence,
            timestamp = result.timestamp
        )
        pronunciationDao.insertResult(entity)
        
        // Update stats
        val existingStats = pronunciationDao.getStats(userId)
        if (existingStats != null) {
            pronunciationDao.incrementStats(userId, result.overallScore)
        } else {
            pronunciationDao.saveStats(
                PronunciationStatsEntity(
                    userId = userId,
                    totalAttempts = 1,
                    averageScore = result.overallScore.toFloat(),
                    bestScore = result.overallScore,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }
    
    override suspend fun getRecentResults(userId: Long, limit: Int): List<PronunciationResult> {
        return pronunciationDao.getRecentResults(userId, limit).map { it.toDomain() }
    }
    
    override suspend fun getStatistics(userId: Long): PronunciationStatistics {
        val stats = pronunciationDao.getStats(userId)
        val distribution = pronunciationDao.getScoreDistribution(userId)
        val recentScores = pronunciationDao.getRecentScores(userId, 10)
        
        val trend = if (recentScores.size >= 3) {
            val firstHalf = recentScores.take(recentScores.size / 2).average()
            val secondHalf = recentScores.drop(recentScores.size / 2).average()
            when {
                secondHalf > firstHalf + 5 -> ImprovementTrend.IMPROVING
                secondHalf < firstHalf - 5 -> ImprovementTrend.DECLINING
                else -> ImprovementTrend.STABLE
            }
        } else {
            ImprovementTrend.NEW
        }
        
        return PronunciationStatistics(
            totalAttempts = stats?.totalAttempts ?: 0,
            averageScore = stats?.averageScore ?: 0f,
            bestScore = stats?.bestScore ?: 0,
            scoreDistribution = ScoreDistribution(
                excellent = distribution.find { it.category == "excellent" }?.count ?: 0,
                good = distribution.find { it.category == "good" }?.count ?: 0,
                fair = distribution.find { it.category == "fair" }?.count ?: 0,
                needsPractice = distribution.find { it.category == "needs_practice" }?.count ?: 0
            ),
            commonErrors = emptyList(),
            improvementTrend = trend,
            lastAttemptAt = stats?.lastUpdated
        )
    }
    
    override suspend fun getCommonErrors(userId: Long): List<String> {
        val errorStrings = pronunciationDao.getRecentErrorWords(userId, 100)
        return errorStrings
            .flatMap { it.split(",") }
            .filter { it.isNotBlank() }
            .groupBy { it.lowercase() }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key }
    }
    
    override fun getStatisticsFlow(userId: Long): Flow<PronunciationStatistics?> {
        return pronunciationDao.getStatsFlow(userId).map { stats ->
            if (stats == null) null else {
                PronunciationStatistics(
                    totalAttempts = stats.totalAttempts,
                    averageScore = stats.averageScore,
                    bestScore = stats.bestScore,
                    lastAttemptAt = stats.lastUpdated
                )
            }
        }
    }
    
    private fun PronunciationResultEntity.toDomain(): PronunciationResult {
        return PronunciationResult(
            id = id,
            exerciseId = exerciseId,
            exerciseType = try { 
                PronunciationExerciseType.valueOf(exerciseType) 
            } catch (e: Exception) { 
                PronunciationExerciseType.DRILL 
            },
            expectedText = expectedText,
            recognizedText = recognizedText,
            overallScore = overallScore,
            breakdown = PronunciationBreakdown(
                accuracyScore = accuracyScore,
                fluencyScore = fluencyScore,
                completenessScore = completenessScore
            ),
            wordAnalysis = emptyList(), // Would need to reconstruct from stored data
            audioDurationMs = audioDurationMs,
            confidence = confidence,
            timestamp = timestamp
        )
    }
}
