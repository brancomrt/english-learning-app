package com.englishlearning.app.data.repository

import com.englishlearning.app.data.database.dao.DailyChallengeDao
import com.englishlearning.app.data.database.entity.DailyChallengeEntity
import com.englishlearning.app.domain.model.ChallengeType
import com.englishlearning.app.domain.model.DailyChallenge
import com.englishlearning.app.domain.model.DailyChallengeSet
import com.englishlearning.app.domain.repository.ChallengeStats
import com.englishlearning.app.domain.repository.DailyChallengeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyChallengeRepositoryImpl @Inject constructor(
    private val dao: DailyChallengeDao
) : DailyChallengeRepository {
    
    override suspend fun getChallengesForDate(userId: Long, date: LocalDate): DailyChallengeSet? {
        val entities = dao.getChallengesForDate(userId, date.toString())
        if (entities.isEmpty()) return null
        
        val challenges = entities.map { it.toDomain() }
        return DailyChallengeSet(
            date = date,
            challenges = challenges,
            totalXpAvailable = challenges.sumOf { it.xpReward + it.bonusXp },
            completedCount = challenges.count { it.isCompleted }
        )
    }
    
    override fun observeTodayChallenges(userId: Long): Flow<DailyChallengeSet?> {
        val today = LocalDate.now().toString()
        return dao.observeChallengesForDate(userId, today).map { entities ->
            if (entities.isEmpty()) null
            else {
                val challenges = entities.map { it.toDomain() }
                DailyChallengeSet(
                    date = LocalDate.now(),
                    challenges = challenges,
                    totalXpAvailable = challenges.sumOf { it.xpReward + it.bonusXp },
                    completedCount = challenges.count { it.isCompleted }
                )
            }
        }
    }
    
    override suspend fun saveChallenges(userId: Long, challenges: List<DailyChallenge>) {
        val entities = challenges.map { it.toEntity(userId) }
        dao.insertAll(entities)
    }
    
    override suspend fun updateChallengeProgress(userId: Long, challengeId: Long, newProgress: Int) {
        dao.updateProgress(userId, challengeId, newProgress)
        
        // Verifica se completou
        val challenge = dao.getById(challengeId)
        if (challenge != null && newProgress >= challenge.targetValue && !challenge.isCompleted) {
            dao.markCompleted(userId, challengeId, System.currentTimeMillis())
        }
    }
    
    override suspend fun markChallengeCompleted(userId: Long, challengeId: Long, completedAt: Long) {
        dao.markCompleted(userId, challengeId, completedAt)
    }
    
    override suspend fun incrementProgressForType(
        userId: Long,
        challengeType: ChallengeType,
        incrementBy: Int
    ) {
        val today = LocalDate.now().toString()
        dao.incrementProgressForType(userId, challengeType.name, today, incrementBy)
        
        // Verifica se algum desafio desse tipo foi completado
        val todayChallenges = dao.getChallengesForDate(userId, today)
        todayChallenges
            .filter { it.type == challengeType.name && !it.isCompleted }
            .forEach { challenge ->
                if (challenge.currentProgress >= challenge.targetValue) {
                    dao.markCompleted(userId, challenge.id, System.currentTimeMillis())
                }
            }
    }
    
    override suspend fun getChallengeStats(userId: Long): ChallengeStats {
        val totalCompleted = dao.getTotalCompletedCount(userId)
        val totalXp = dao.getTotalXpEarned(userId) ?: 0
        val daysCompleted = dao.getDaysWithCompletedChallenges(userId)
        
        return ChallengeStats(
            totalCompleted = totalCompleted,
            totalXpEarned = totalXp,
            currentStreak = daysCompleted, // Simplificado
            bestStreak = daysCompleted,
            completionRate = if (totalCompleted > 0) 0.7f else 0f // Placeholder
        )
    }
    
    override suspend fun cleanupOldChallenges(userId: Long) {
        val cutoffDate = LocalDate.now().minusDays(30).toString()
        dao.deleteOldChallenges(cutoffDate)
    }
    
    // ────────────────────────────────────────────────────────────────
    // Mappers
    // ────────────────────────────────────────────────────────────────
    
    private fun DailyChallengeEntity.toDomain(): DailyChallenge = DailyChallenge(
        id = id,
        type = ChallengeType.valueOf(type),
        title = title,
        description = description,
        targetValue = targetValue,
        currentProgress = currentProgress,
        xpReward = xpReward,
        bonusXp = bonusXp,
        date = LocalDate.parse(date),
        isCompleted = isCompleted,
        completedAt = completedAt,
        iconEmoji = iconEmoji
    )
    
    private fun DailyChallenge.toEntity(userId: Long): DailyChallengeEntity = DailyChallengeEntity(
        id = if (id == 0L) 0 else id,
        userId = userId,
        type = type.name,
        title = title,
        description = description,
        targetValue = targetValue,
        currentProgress = currentProgress,
        xpReward = xpReward,
        bonusXp = bonusXp,
        date = date.toString(),
        isCompleted = isCompleted,
        completedAt = completedAt,
        iconEmoji = iconEmoji
    )
}
