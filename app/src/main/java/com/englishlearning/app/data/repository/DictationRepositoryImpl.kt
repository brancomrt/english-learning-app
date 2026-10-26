package com.englishlearning.app.data.repository

import com.englishlearning.app.data.database.dao.DictationDao
import com.englishlearning.app.data.database.entity.DictationExerciseEntity
import com.englishlearning.app.data.database.entity.DictationProgressEntity
import com.englishlearning.app.domain.model.DictationExercise
import com.englishlearning.app.domain.model.DictationProgress
import com.englishlearning.app.domain.model.DictationResult
import com.englishlearning.app.domain.model.DictationStatistics
import com.englishlearning.app.domain.model.SrsState
import com.englishlearning.app.domain.repository.DictationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementação do DictationRepository para ditados estilo Callan Method.
 * Organizado por Stages (1-12) com textos completos.
 */
@Singleton
class DictationRepositoryImpl @Inject constructor(
    private val dictationDao: DictationDao
) : DictationRepository {
    
    // ═══════════════════════════════════════════════════════════════════════
    // Exercises by Stage (Callan Style)
    // ═══════════════════════════════════════════════════════════════════════
    
    override suspend fun getByStage(stage: Int): List<DictationExercise> {
        return dictationDao.getExercisesByStage(stage).map { it.toDomain() }
    }
    
    override fun getByStageFlow(stage: Int): Flow<List<DictationExercise>> {
        return dictationDao.getExercisesByStageFlow(stage).map { list ->
            list.map { it.toDomain() }
        }
    }
    
    override suspend fun getAllExercises(): List<DictationExercise> {
        return dictationDao.getAllExercises().map { it.toDomain() }
    }
    
    override fun getAllExercisesFlow(): Flow<List<DictationExercise>> {
        return dictationDao.getAllExercisesFlow().map { list ->
            list.map { it.toDomain() }
        }
    }
    
    override suspend fun getAvailableStages(): List<Int> {
        return dictationDao.getAvailableStages()
    }
    
    override suspend fun getById(id: Long): DictationExercise? {
        return dictationDao.getExerciseById(id)?.toDomain()
    }
    
    override suspend fun getIncompleteByStage(stage: Int): List<DictationExercise> {
        return dictationDao.getIncompleteByStage(stage).map { it.toDomain() }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // Mark Complete (100% success)
    // ═══════════════════════════════════════════════════════════════════════
    
    override suspend fun markCompleted(exerciseId: Long, completed: Boolean) {
        dictationDao.markCompleted(exerciseId, completed)
    }
    
    override suspend fun getCompletedCountByStage(stage: Int): Int {
        return dictationDao.getCompletedCountByStage(stage)
    }
    
    override suspend fun getTotalCompletedCount(): Int {
        return dictationDao.getTotalCompletedCount()
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // Save Result and Progress
    // ═══════════════════════════════════════════════════════════════════════
    
    override suspend fun saveResult(userId: Long, exerciseId: Long, result: DictationResult) {
        val today = LocalDate.now().toString()
        val existingProgress = dictationDao.getProgress(userId, exerciseId)
        
        // Se score = 100, marcar como completo
        if (result.score == 100) {
            dictationDao.markCompleted(exerciseId, true)
        }
        
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
    
    // ═══════════════════════════════════════════════════════════════════════
    // Statistics
    // ═══════════════════════════════════════════════════════════════════════
    
    override suspend fun getStatistics(userId: Long): DictationStatistics {
        val avgScore = dictationDao.getAverageScore(userId) ?: 0f
        val masteredCount = dictationDao.getMasteredCount(userId)
        val totalAttempts = dictationDao.getTotalAttempts(userId) ?: 0
        val today = LocalDate.now().toString()
        val dueCount = dictationDao.getDueCount(userId, today)
        val totalExercises = dictationDao.getTotalExerciseCount()
        val completedCount = dictationDao.getTotalCompletedCount()
        
        return DictationStatistics(
            totalExercises = totalExercises,
            completedExercises = completedCount,
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
    
    // ═══════════════════════════════════════════════════════════════════════
    // Legacy methods (kept for compatibility but can be removed later)
    // ═══════════════════════════════════════════════════════════════════════
    
    override suspend fun getDueExercises(userId: Long, today: String, limit: Int): List<DictationExercise> {
        return dictationDao.getDueExercises(userId, today, limit).map { it.toDomain() }
    }
    
    override suspend fun getNewExercisesByStage(userId: Long, stage: Int, limit: Int): List<DictationExercise> {
        return dictationDao.getNewExercisesByStage(userId, stage, limit).map { it.toDomain() }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // Mapping
    // ═══════════════════════════════════════════════════════════════════════
    
    private fun DictationExerciseEntity.toDomain(): DictationExercise {
        return DictationExercise(
            id = id,
            stage = stage,
            title = title,
            text = text,
            translation = translation,
            wordCount = wordCount,
            grammarFocus = grammarFocus,
            completed = completed
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
