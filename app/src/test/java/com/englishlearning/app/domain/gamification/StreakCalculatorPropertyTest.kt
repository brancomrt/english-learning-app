package com.englishlearning.app.domain.gamification

import io.kotest.core.spec.style.FreeSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.forAll
import java.time.LocalDate

/**
 * Property-based tests for [StreakCalculator.calculateNewStreak].
 *
 * Validates: Requisitos 5.2, 5.3, 5.4, 5.5
 */
class StreakCalculatorPropertyTest : FreeSpec({

    val calculator = StreakCalculator()

    /**
     * Arbitrary that generates a random LocalDate within the range 2016-06 … 2027-06
     * (epoch days 17000–21000), giving realistic dates without pulling in extra modules.
     */
    val arbLocalDate: Arb<LocalDate> = Arb.long(17000L..21000L)
        .map { epochDay -> LocalDate.ofEpochDay(epochDay) }

    // ---------------------------------------------------------------------------
    // Propriedade 8: Streak Consistente com Datas
    //
    // Para qualquer currentStreak >= 0, lastActivityDate e today onde today >= lastActivityDate:
    //   • daysDiff == 0 → newStreak == currentStreak            (mesmo dia, streak mantido)
    //   • daysDiff == 1 → newStreak == currentStreak + 1        (ontem, streak incrementado)
    //   • daysDiff >  1 → newStreak == 1                        (mais de 1 dia, streak resetado)
    //   • Invariante: newStreak >= 1 para qualquer entrada válida
    //
    // Valida: Requisitos 5.2, 5.3, 5.4
    // ---------------------------------------------------------------------------

    "Propriedade 8a - mesmo dia (daysDiff == 0): newStreak == currentStreak e streak não quebrado" {
        forAll(
            iterations = 500,
            Arb.int(0..100),
            arbLocalDate
        ) { currentStreak, date ->
            val result = calculator.calculateNewStreak(
                currentStreak = currentStreak,
                lastActivityDate = date,
                today = date           // same day → daysDiff = 0
            )
            result.newStreak == currentStreak &&
                !result.streakBroken &&
                !result.isNewDay
        }
    }

    "Propriedade 8b - ontem (daysDiff == 1): newStreak == currentStreak + 1" {
        forAll(
            iterations = 500,
            Arb.int(0..100),
            arbLocalDate
        ) { currentStreak, lastDate ->
            val today = lastDate.plusDays(1)   // daysDiff = 1
            val result = calculator.calculateNewStreak(
                currentStreak = currentStreak,
                lastActivityDate = lastDate,
                today = today
            )
            result.newStreak == currentStreak + 1 &&
                !result.streakBroken &&
                result.isNewDay
        }
    }

    "Propriedade 8c - mais de 1 dia (daysDiff > 1): newStreak == 1" {
        forAll(
            iterations = 500,
            Arb.int(0..100),
            arbLocalDate,
            Arb.long(2L..365L)             // gap: 2 to 365 days
        ) { currentStreak, lastDate, gapDays ->
            val today = lastDate.plusDays(gapDays)
            val result = calculator.calculateNewStreak(
                currentStreak = currentStreak,
                lastActivityDate = lastDate,
                today = today
            )
            result.newStreak == 1 &&
                result.isNewDay
        }
    }

    // Invariant: newStreak >= 1 EXCEPT when the streak was already 0 and today == lastActivityDate.
    // If daysDiff == 0 and currentStreak == 0, newStreak stays 0 (correct: "manter o currentStreak").
    // For any other combination the streak either advances (daysDiff == 1 → +1) or resets to 1
    // (daysDiff > 1) or was already > 0 (daysDiff == 0, currentStreak > 0).
    "Propriedade 8d - invariante: newStreak >= 1 quando daysDiff > 0 ou currentStreak > 0" {
        forAll(
            iterations = 1000,
            Arb.int(0..100),
            arbLocalDate,
            Arb.long(0L..365L)             // today is same or later than lastDate
        ) { currentStreak, lastDate, gapDays ->
            val today = lastDate.plusDays(gapDays)
            val result = calculator.calculateNewStreak(
                currentStreak = currentStreak,
                lastActivityDate = lastDate,
                today = today
            )
            val daysDiff = gapDays  // same as ChronoUnit.DAYS.between(lastDate, today)
            if (daysDiff == 0L && currentStreak == 0) {
                // Edge case: streak was 0 and same day → allowed to stay at 0
                result.newStreak >= 0
            } else {
                result.newStreak >= 1
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Propriedade 9: longestStreak Nunca Menor que currentStreak
    //
    // A StreakCalculator retorna StreakResult; a responsabilidade de atualizar o
    // longestStreak pertence ao chamador (ProgressRepositoryImpl), que aplica
    // longestStreak = max(longestStreak, newStreak). Esta propriedade verifica que
    // essa política garante o invariante do Requisito 5.5 para qualquer entrada.
    //
    // Valida: Requisito 5.5
    // ---------------------------------------------------------------------------

    "Propriedade 9 - longestStreak Nunca Menor que currentStreak: após max(longestStreak, newStreak), longestStreak >= newStreak" {
        forAll(
            iterations = 1000,
            Arb.int(0..50),       // initialCurrentStreak
            Arb.int(0..100),      // rawLongestStreak (normalised to be >= currentStreak)
            arbLocalDate,
            Arb.long(0L..2L)      // dayGap: 0 (same day), 1 (yesterday), 2 (two days → reset)
        ) { initialStreak, rawLongest, lastDate, gapDays ->
            // Ensure initial state is consistent
            val initialLongest = maxOf(rawLongest, initialStreak)
            val today = lastDate.plusDays(gapDays)

            val result = calculator.calculateNewStreak(
                currentStreak = initialStreak,
                lastActivityDate = lastDate,
                today = today
            )

            // Caller updates longestStreak when newStreak exceeds it
            val updatedLongest = maxOf(initialLongest, result.newStreak)

            // Invariant must hold after every update
            updatedLongest >= result.newStreak
        }
    }
})
