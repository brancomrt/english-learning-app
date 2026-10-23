package com.englishlearning.app.domain.usecase

import com.englishlearning.app.domain.model.ChallengeType
import com.englishlearning.app.domain.model.DailyChallenge
import com.englishlearning.app.domain.model.DailyChallengeSet
import com.englishlearning.app.domain.repository.DailyChallengeRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random

/**
 * Use case para gerar desafios diários personalizados.
 * 
 * Gera 3-5 desafios baseados no nível e histórico do usuário.
 */
class GenerateDailyChallengesUseCase @Inject constructor(
    private val challengeRepository: DailyChallengeRepository
) {
    
    companion object {
        private const val MIN_CHALLENGES = 3
        private const val MAX_CHALLENGES = 5
    }
    
    /**
     * Obtém ou gera os desafios do dia.
     * Se já existirem desafios para hoje, retorna os existentes.
     * Caso contrário, gera novos desafios.
     */
    suspend operator fun invoke(userId: Long = 1L): DailyChallengeSet {
        val today = LocalDate.now()
        
        // Verifica se já existem desafios para hoje
        val existingSet = challengeRepository.getChallengesForDate(userId, today)
        if (existingSet != null && existingSet.challenges.isNotEmpty()) {
            return existingSet
        }
        
        // Gera novos desafios
        val challenges = generateChallenges(userId, today)
        
        // Salva os desafios gerados
        challengeRepository.saveChallenges(userId, challenges)
        
        return DailyChallengeSet(
            date = today,
            challenges = challenges,
            totalXpAvailable = challenges.sumOf { it.xpReward + it.bonusXp },
            completedCount = 0
        )
    }
    
    /**
     * Gera lista de desafios personalizados para o usuário.
     */
    private suspend fun generateChallenges(userId: Long, date: LocalDate): List<DailyChallenge> {
        // Usa nível padrão já que não temos acesso direto ao progresso aqui
        val userLevel = 1
        
        // Pool de desafios possíveis
        val challengePool = mutableListOf<DailyChallenge>()
        var id = System.currentTimeMillis()
        
        // 1. Desafio de exercícios (sempre presente)
        val exerciseTarget = when (userLevel) {
            1 -> Random.nextInt(5, 10)
            2 -> Random.nextInt(8, 15)
            3 -> Random.nextInt(10, 20)
            else -> Random.nextInt(15, 25)
        }
        challengePool.add(
            DailyChallenge(
                id = id++,
                type = ChallengeType.EXERCISE_COUNT,
                title = "Pratique $exerciseTarget exercícios",
                description = "Complete $exerciseTarget exercícios em qualquer modo",
                targetValue = exerciseTarget,
                xpReward = exerciseTarget * 10,
                bonusXp = 25,
                date = date,
                iconEmoji = "📝"
            )
        )
        
        // 2. Desafio de streak (acertos seguidos)
        val streakTarget = when (userLevel) {
            1 -> Random.nextInt(3, 5)
            2 -> Random.nextInt(4, 7)
            else -> Random.nextInt(5, 10)
        }
        challengePool.add(
            DailyChallenge(
                id = id++,
                type = ChallengeType.STREAK_COUNT,
                title = "Sequência de $streakTarget",
                description = "Acerte $streakTarget exercícios seguidos sem errar",
                targetValue = streakTarget,
                xpReward = streakTarget * 15,
                bonusXp = 30,
                date = date,
                iconEmoji = "🔥"
            )
        )
        
        // 3. Desafio de variedade (categorias diferentes)
        val categoryTarget = Random.nextInt(2, 4)
        challengePool.add(
            DailyChallenge(
                id = id++,
                type = ChallengeType.CATEGORY_VARIETY,
                title = "Explorador",
                description = "Pratique em $categoryTarget categorias diferentes",
                targetValue = categoryTarget,
                xpReward = categoryTarget * 20,
                bonusXp = 20,
                date = date,
                iconEmoji = "🗺️"
            )
        )
        
        // 4. Desafio de Shadowing (para usuários intermediários+)
        if (userLevel >= 2) {
            challengePool.add(
                DailyChallenge(
                    id = id++,
                    type = ChallengeType.SHADOWING_SESSION,
                    title = "Treine Pronúncia",
                    description = "Complete uma sessão de Shadowing",
                    targetValue = 1,
                    xpReward = 50,
                    bonusXp = 25,
                    date = date,
                    iconEmoji = "🎤"
                )
            )
        }
        
        // 5. Desafio de Listening First
        challengePool.add(
            DailyChallenge(
                id = id++,
                type = ChallengeType.LISTENING_SESSION,
                title = "Ouça Primeiro",
                description = "Complete 5 exercícios no modo Listening First",
                targetValue = 5,
                xpReward = 40,
                bonusXp = 20,
                date = date,
                iconEmoji = "👂"
            )
        )
        
        // 6. Desafio de revisão SRS
        challengePool.add(
            DailyChallenge(
                id = id++,
                type = ChallengeType.SRS_REVIEW,
                title = "Revisão Inteligente",
                description = "Revise 10 cards no sistema SRS",
                targetValue = 10,
                xpReward = 30,
                bonusXp = 15,
                date = date,
                iconEmoji = "🧠"
            )
        )
        
        // 7. Desafio de Conversation Chain
        if (userLevel >= 2) {
            challengePool.add(
                DailyChallenge(
                    id = id++,
                    type = ChallengeType.CONVERSATION_CHAIN,
                    title = "Mini-Diálogo",
                    description = "Complete uma cadeia de conversação",
                    targetValue = 1,
                    xpReward = 60,
                    bonusXp = 30,
                    date = date,
                    iconEmoji = "💬"
                )
            )
        }
        
        // 8. Desafio baseado em tempo
        val minutesTarget = Random.nextInt(10, 20)
        challengePool.add(
            DailyChallenge(
                id = id++,
                type = ChallengeType.TIME_BASED,
                title = "Estude $minutesTarget min",
                description = "Estude por pelo menos $minutesTarget minutos hoje",
                targetValue = minutesTarget,
                xpReward = minutesTarget * 3,
                bonusXp = 20,
                date = date,
                iconEmoji = "⏱️"
            )
        )
        
        // Seleciona aleatoriamente MIN_CHALLENGES a MAX_CHALLENGES desafios
        val numChallenges = Random.nextInt(MIN_CHALLENGES, MAX_CHALLENGES + 1)
        return challengePool.shuffled().take(numChallenges)
    }
}
