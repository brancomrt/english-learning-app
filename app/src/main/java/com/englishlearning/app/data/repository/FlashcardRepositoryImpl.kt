package com.englishlearning.app.data.repository

import com.englishlearning.app.data.database.dao.FlashcardDao
import com.englishlearning.app.data.database.entity.FlashcardEntity
import com.englishlearning.app.domain.model.Flashcard
import com.englishlearning.app.domain.model.FlashcardRating
import com.englishlearning.app.domain.model.FlashcardStatistics
import com.englishlearning.app.domain.model.FlashcardStats
import com.englishlearning.app.domain.model.PartOfSpeech
import com.englishlearning.app.domain.model.SrsState
import com.englishlearning.app.domain.repository.FlashcardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementação do FlashcardRepository.
 */
@Singleton
class FlashcardRepositoryImpl @Inject constructor(
    private val flashcardDao: FlashcardDao
) : FlashcardRepository {
    
    override suspend fun getDueCards(userId: Long, today: String, limit: Int): List<Flashcard> {
        return flashcardDao.getDueCards(userId, today, limit).map { it.toDomain() }
    }
    
    override suspend fun getDueCount(userId: Long, today: String): Int {
        return flashcardDao.getDueCount(userId, today)
    }
    
    override suspend fun getNewCards(userId: Long, limit: Int): List<Flashcard> {
        return flashcardDao.getNewCards(userId, limit).map { it.toDomain() }
    }
    
    override suspend fun getNewCount(userId: Long): Int {
        return flashcardDao.getNewCount(userId)
    }
    
    override suspend fun reviewCard(cardId: Long, rating: FlashcardRating) {
        val card = flashcardDao.getById(cardId) ?: return
        val today = LocalDate.now().toString()
        
        // Simplified FSRS update
        val result: SrsUpdateResult = when (rating) {
            FlashcardRating.AGAIN -> {
                val stability = (card.stability * 0.5).coerceAtLeast(0.1)
                val difficulty = (card.difficulty + 0.5).coerceAtMost(10.0)
                SrsUpdateResult(stability, difficulty, 1, "RELEARNING")
            }
            FlashcardRating.HARD -> {
                val stability = card.stability * 1.2
                val difficulty = (card.difficulty + 0.1).coerceAtMost(10.0)
                SrsUpdateResult(stability, difficulty, maxOf(1, (stability * 0.8).toInt()), "REVIEW")
            }
            FlashcardRating.GOOD -> {
                val stability = if (card.stability == 0.0) 1.0 else card.stability * 2.5
                val difficulty = card.difficulty
                SrsUpdateResult(stability, difficulty, stability.toInt().coerceAtLeast(1), "REVIEW")
            }
            FlashcardRating.EASY -> {
                val stability = if (card.stability == 0.0) 4.0 else card.stability * 4.0
                val difficulty = (card.difficulty - 0.1).coerceAtLeast(1.0)
                SrsUpdateResult(stability, difficulty, stability.toInt().coerceAtLeast(4), "REVIEW")
            }
        }
        
        val dueDate = LocalDate.now().plusDays(result.scheduledDays.toLong()).toString()
        val lapses = if (rating == FlashcardRating.AGAIN) card.lapses + 1 else card.lapses
        
        flashcardDao.updateSrsParams(
            id = cardId,
            stability = result.stability,
            difficulty = result.difficulty,
            elapsedDays = card.scheduledDays,
            scheduledDays = result.scheduledDays,
            reps = card.reps + 1,
            lapses = lapses,
            state = result.state,
            dueDate = dueDate,
            lastReviewDate = today
        )
    }
    
    private data class SrsUpdateResult(
        val stability: Double,
        val difficulty: Double,
        val scheduledDays: Int,
        val state: String
    )
    
    override suspend fun getById(id: Long): Flashcard? {
        return flashcardDao.getById(id)?.toDomain()
    }
    
    override suspend fun getByLevel(levelId: Int): List<Flashcard> {
        return flashcardDao.getByLevel(levelId).map { it.toDomain() }
    }
    
    override suspend fun search(userId: Long, query: String, limit: Int): List<Flashcard> {
        return flashcardDao.search(userId, query, limit).map { it.toDomain() }
    }
    
    override suspend fun getStatistics(userId: Long): FlashcardStatistics {
        val total = flashcardDao.getTotalCount(userId)
        val newCount = flashcardDao.getNewCount(userId)
        val today = LocalDate.now().toString()
        val dueCount = flashcardDao.getDueCount(userId, today)
        val masteredCount = flashcardDao.getMasteredCount(userId)
        val learningCount = flashcardDao.getLearningCount(userId)
        val avgDifficulty = flashcardDao.getAverageDifficulty(userId) ?: 5.0
        
        return FlashcardStatistics(
            totalCards = total,
            newCards = newCount,
            learningCards = learningCount,
            reviewCards = dueCount,
            masteredCards = masteredCount,
            dueToday = dueCount,
            averageDifficulty = avgDifficulty,
            totalReviews = 0, // TODO: track this
            streakDays = 0
        )
    }
    
    override suspend fun resetProgress(userId: Long, today: String) {
        flashcardDao.resetAllProgress(userId, today)
    }
    
    override suspend fun extractVocabularyFromDrills() {
        // TODO: Implement vocabulary extraction from drills
    }
    
    override fun getDueCountFlow(userId: Long, today: String): Flow<Int> {
        return flashcardDao.getDueCountFlow(userId, today)
    }
    
    private fun FlashcardEntity.toDomain(): Flashcard {
        return Flashcard(
            id = id,
            word = word,
            definition = definition,
            exampleSentence = exampleSentence,
            context = context,
            sourceDrillId = sourceDrillId,
            levelId = levelId,
            partOfSpeech = partOfSpeech?.let { 
                try { PartOfSpeech.valueOf(it) } catch (e: Exception) { null }
            },
            pronunciation = pronunciation,
            audioUrl = audioUrl,
            imageUrl = imageUrl,
            srsState = try { SrsState.valueOf(state) } catch (e: Exception) { SrsState.NEW },
            dueDate = dueDate,
            stats = FlashcardStats(
                stability = stability,
                difficulty = difficulty,
                elapsedDays = elapsedDays,
                scheduledDays = scheduledDays,
                reps = reps,
                lapses = lapses,
                lastReviewDate = lastReviewDate
            )
        )
    }
}
