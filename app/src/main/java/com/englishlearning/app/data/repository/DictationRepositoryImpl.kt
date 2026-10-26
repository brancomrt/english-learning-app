package com.englishlearning.app.data.repository

import com.englishlearning.app.data.database.dao.DictationDao
import com.englishlearning.app.data.database.entity.DictationExerciseEntity
import com.englishlearning.app.data.database.entity.DictationProgressEntity
import com.englishlearning.app.domain.model.DictationDifficulty
import com.englishlearning.app.domain.model.DictationExercise
import com.englishlearning.app.domain.model.DictationProgress
import com.englishlearning.app.domain.model.DictationResult
import com.englishlearning.app.domain.model.DictationStatistics
import com.englishlearning.app.domain.model.SrsState
import com.englishlearning.app.domain.repository.DictationRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementação do DictationRepository.
 */
@Singleton
class DictationRepositoryImpl @Inject constructor(
    private val dictationDao: DictationDao
) : DictationRepository {
    
    override suspend fun getDueExercises(userId: Long, today: String, limit: Int): List<DictationExercise> {
        return dictationDao.getDueExercises(userId, today, limit).map { it.toDomain() }
    }
    
    override suspend fun getNewExercises(userId: Long, maxLevel: Int, limit: Int): List<DictationExercise> {
        return dictationDao.getNewExercises(userId, maxLevel, limit).map { it.toDomain() }
    }
    
    override suspend fun getById(id: Long): DictationExercise? {
        return dictationDao.getExerciseById(id)?.toDomain()
    }
    
    override suspend fun getByLevel(levelId: Int): List<DictationExercise> {
        return dictationDao.getExercisesByLevel(levelId).map { it.toDomain() }
    }
    
    override suspend fun saveResult(userId: Long, exerciseId: Long, result: DictationResult) {
        val today = LocalDate.now().toString()
        val existingProgress = dictationDao.getProgress(userId, exerciseId)
        
        if (existingProgress != null) {
            // Update existing progress with FSRS
            val isCorrect = result.score >= 80
            val (stability, difficulty, scheduledDays, state, lapses) = calculateSrs(
                existingProgress, isCorrect
            )
            val dueDate = LocalDate.now().plusDays(scheduledDays.toLong()).toString()
            
            dictationDao.updateAfterReview(
                userId = userId,
                exerciseId = exerciseId,
                score = result.score,
                stability = stability,
                difficulty = difficulty,
                elapsedDays = existingProgress.scheduledDays,
                scheduledDays = scheduledDays,
                reps = existingProgress.reps + 1,
                lapses = lapses,
                state = state,
                dueDate = dueDate,
                lastReviewDate = today
            )
        } else {
            // Create new progress
            val scheduledDays = if (result.score >= 80) 1 else 0
            val dueDate = LocalDate.now().plusDays(scheduledDays.toLong()).toString()
            
            dictationDao.saveProgress(
                DictationProgressEntity(
                    userId = userId,
                    exerciseId = exerciseId,
                    attempts = 1,
                    correctCount = if (result.score >= 80) 1 else 0,
                    bestScore = result.score,
                    lastScore = result.score,
                    stability = if (result.score >= 80) 1.0 else 0.5,
                    difficulty = 5.0,
                    elapsedDays = 0,
                    scheduledDays = scheduledDays,
                    reps = 1,
                    lapses = if (result.score < 60) 1 else 0,
                    state = if (result.score >= 80) "LEARNING" else "NEW",
                    dueDate = dueDate,
                    lastReviewDate = today
                )
            )
        }
    }
    
    private fun calculateSrs(
        progress: DictationProgressEntity,
        isCorrect: Boolean
    ): SrsUpdateResult {
        return if (isCorrect) {
            val stability = if (progress.stability == 0.0) 1.0 else progress.stability * 2.5
            val scheduledDays = stability.toInt().coerceAtLeast(1)
            SrsUpdateResult(
                stability = stability,
                difficulty = progress.difficulty,
                scheduledDays = scheduledDays,
                state = "REVIEW",
                lapses = progress.lapses
            )
        } else {
            val stability = (progress.stability * 0.5).coerceAtLeast(0.1)
            SrsUpdateResult(
                stability = stability,
                difficulty = (progress.difficulty + 0.3).coerceAtMost(10.0),
                scheduledDays = 1,
                state = "RELEARNING",
                lapses = progress.lapses + 1
            )
        }
    }
    
    private data class SrsUpdateResult(
        val stability: Double,
        val difficulty: Double,
        val scheduledDays: Int,
        val state: String,
        val lapses: Int
    )
    
    override suspend fun getProgress(userId: Long, exerciseId: Long): DictationProgress? {
        return dictationDao.getProgress(userId, exerciseId)?.toDomain()
    }
    
    override suspend fun getStatistics(userId: Long): DictationStatistics {
        val avgScore = dictationDao.getAverageScore(userId) ?: 0f
        val masteredCount = dictationDao.getMasteredCount(userId)
        val totalAttempts = dictationDao.getTotalAttempts(userId) ?: 0
        val today = LocalDate.now().toString()
        val dueCount = dictationDao.getDueCount(userId, today)
        val totalExercises = dictationDao.getTotalExerciseCount()
        
        return DictationStatistics(
            totalExercises = totalExercises,
            completedExercises = masteredCount,
            averageScore = avgScore,
            bestScore = 0, // TODO
            totalAttempts = totalAttempts,
            masteredCount = masteredCount,
            dueToday = dueCount,
            commonMistakes = emptyMap()
        )
    }
    
    override suspend fun getDueCount(userId: Long, today: String): Int {
        return dictationDao.getDueCount(userId, today)
    }
    
    override fun getDueCountFlow(userId: Long, today: String): Flow<Int> {
        return dictationDao.getDueCountFlow(userId, today)
    }
    
    override suspend fun generateFromDrills() {
        // TODO: Generate dictation exercises from drills
    }
    
    private fun DictationExerciseEntity.toDomain(): DictationExercise {
        return DictationExercise(
            id = id,
            sentence = sentence,
            translation = translation,
            levelId = levelId,
            difficulty = DictationDifficulty.entries.getOrNull(difficulty - 1) ?: DictationDifficulty.MEDIUM,
            grammarTopic = grammarTopic,
            keywords = keywords?.split(",")?.map { it.trim() } ?: emptyList(),
            sourceDrillId = sourceDrillId
        )
    }
    
    private fun DictationProgressEntity.toDomain(): DictationProgress {
        return DictationProgress(
            exerciseId = exerciseId,
            attempts = attempts,
            correctCount = correctCount,
            bestScore = bestScore,
            lastScore = lastScore,
            srsState = try { SrsState.valueOf(state) } catch (e: Exception) { SrsState.NEW },
            dueDate = dueDate,
            lastReviewDate = lastReviewDate
        )
    }
}
