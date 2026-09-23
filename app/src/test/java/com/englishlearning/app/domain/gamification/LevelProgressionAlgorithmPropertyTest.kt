package com.englishlearning.app.domain.gamification

import com.englishlearning.app.domain.model.ValidationStatus
import io.kotest.core.spec.style.FreeSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.forAll

/**
 * Property-based tests for [LevelProgressionAlgorithm.calculatePoints].
 *
 * Validates: Requisitos 4.2, 4.3, 4.4
 */
class LevelProgressionAlgorithmPropertyTest : FreeSpec({

    // ---------------------------------------------------------------------------
    // Propriedade 4: Pontuação Nunca Negativa
    // calculatePoints() >= 0 para qualquer combinação válida de entradas
    // Valida: Requisitos 4.4
    // ---------------------------------------------------------------------------
    "Propriedade 4 - Pontuação Nunca Negativa: calculatePoints() >= 0 para qualquer entrada" {
        forAll(
            iterations = 1000,
            Arb.enum<ValidationStatus>(),
            Arb.int(1..10),
            Arb.int(1..20),
            Arb.boolean()
        ) { status, difficultyScore, attemptNumber, hasActiveStreak ->
            val points = LevelProgressionAlgorithm.calculatePoints(
                status = status,
                difficultyScore = difficultyScore,
                attemptNumber = attemptNumber,
                hasActiveStreak = hasActiveStreak
            )
            points >= 0
        }
    }

    // ---------------------------------------------------------------------------
    // Propriedade 11: Penalidade de Tentativa Reduz Pontuação Monotonicamente
    // calculatePoints(CORRECT, d, n+1, s) <= calculatePoints(CORRECT, d, n, s)
    // adicionar tentativas nunca aumenta a pontuação
    // Valida: Requisitos 4.3
    // ---------------------------------------------------------------------------
    "Propriedade 11 - Penalidade de Tentativa Reduz Pontuação Monotonicamente: pontosN+1 <= pontosN" {
        forAll(
            iterations = 1000,
            Arb.int(1..10),
            Arb.int(1..19),  // n in [1..19] so n+1 is still <= 20
            Arb.boolean()
        ) { difficultyScore, attemptNumber, hasActiveStreak ->
            val pointsN = LevelProgressionAlgorithm.calculatePoints(
                status = ValidationStatus.CORRECT,
                difficultyScore = difficultyScore,
                attemptNumber = attemptNumber,
                hasActiveStreak = hasActiveStreak
            )
            val pointsNplus1 = LevelProgressionAlgorithm.calculatePoints(
                status = ValidationStatus.CORRECT,
                difficultyScore = difficultyScore,
                attemptNumber = attemptNumber + 1,
                hasActiveStreak = hasActiveStreak
            )
            pointsNplus1 <= pointsN
        }
    }

    // ---------------------------------------------------------------------------
    // Propriedade 12: Multiplicador de Streak Aumenta Pontuação
    // calculatePoints(CORRECT, d, n, hasStreak=true) >= calculatePoints(CORRECT, d, n, hasStreak=false)
    // ter streak ativo nunca reduz a pontuação
    // Valida: Requisitos 4.2
    // ---------------------------------------------------------------------------
    "Propriedade 12 - Multiplicador de Streak Aumenta Pontuação: pontosComStreak >= pontosSemStreak" {
        forAll(
            iterations = 1000,
            Arb.int(1..10),
            Arb.int(1..20)
        ) { difficultyScore, attemptNumber ->
            val pointsWithoutStreak = LevelProgressionAlgorithm.calculatePoints(
                status = ValidationStatus.CORRECT,
                difficultyScore = difficultyScore,
                attemptNumber = attemptNumber,
                hasActiveStreak = false
            )
            val pointsWithStreak = LevelProgressionAlgorithm.calculatePoints(
                status = ValidationStatus.CORRECT,
                difficultyScore = difficultyScore,
                attemptNumber = attemptNumber,
                hasActiveStreak = true
            )
            pointsWithStreak >= pointsWithoutStreak
        }
    }
})
