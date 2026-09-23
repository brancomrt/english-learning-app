package com.englishlearning.app.domain.gamification

import io.kotest.core.spec.style.FreeSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.filter
import io.kotest.property.forAll
import java.time.LocalDate

/**
 * Property-based tests for [FsrsScheduler].
 *
 * **Validates: Requisitos 16.4, 16.5**
 *
 * ## Propriedades testadas
 *
 * - **Propriedade 13**: Intervalo SRS Cresce Monotonicamente para GOOD/EASY —
 *   para qualquer card em estado REVIEW, aplicar GOOD ou EASY produz
 *   `result.scheduledDays > card.scheduledDays`.
 *
 * - **Propriedade 14**: AGAIN Sempre Reseta para Intervalo Mínimo (== 1) —
 *   para qualquer card em qualquer estado, aplicar AGAIN produz
 *   `result.scheduledDays == 1`.
 *
 * - **Propriedade 15**: Retrievability em [0, 1] —
 *   para qualquer `stability > 0` e `elapsedDays >= 0`,
 *   `FsrsScheduler.retrievability` retorna valor em `[0.0, 1.0]`.
 */
class FsrsSchedulerPropertyTest : FreeSpec({

    // ─── Geradores compartilhados ─────────────────────────────────────────────

    /** Stability válida: float positivo em (0, 200]. */
    val arbStability: Arb<Float> = Arb.float(0.1f, 200f)

    /** Difficulty válida: float em [1.0, 10.0]. */
    val arbDifficulty: Arb<Float> = Arb.float(1f, 10f)

    /** Dias decorridos: inteiro em [0, 3650] (0 a 10 anos). */
    val arbElapsedDays: Arb<Int> = Arb.int(0, 3650)

    /** Intervalo agendado anterior: inteiro em [1, 365]. */
    val arbScheduledDays: Arb<Int> = Arb.int(1, 365)

    /** Card em estado REVIEW com todos os campos válidos. */
    val arbReviewCard: Arb<SrsCardParams> = Arb.bind(
        arbStability,
        arbDifficulty,
        arbElapsedDays,
        arbScheduledDays,
        Arb.int(1, 100),   // reps >= 1 (já revisado ao menos uma vez)
        Arb.int(0, 10)     // lapses
    ) { stability, difficulty, elapsedDays, scheduledDays, reps, lapses ->
        SrsCardParams(
            stability = stability,
            difficulty = difficulty,
            elapsedDays = elapsedDays,
            scheduledDays = scheduledDays,
            reps = reps,
            lapses = lapses,
            state = SrsState.REVIEW
        )
    }

    /** Card em qualquer estado válido do FSRS. */
    val arbAnyCard: Arb<SrsCardParams> = Arb.bind(
        arbStability,
        arbDifficulty,
        arbElapsedDays,
        arbScheduledDays,
        Arb.int(0, 100),   // reps
        Arb.int(0, 10),    // lapses
        Arb.enum<SrsState>()
    ) { stability, difficulty, elapsedDays, scheduledDays, reps, lapses, state ->
        SrsCardParams(
            stability = stability,
            difficulty = difficulty,
            elapsedDays = elapsedDays,
            scheduledDays = scheduledDays,
            reps = reps,
            lapses = lapses,
            state = state
        )
    }

    val fixedToday: LocalDate = LocalDate.of(2024, 6, 1)

    // ─── Propriedade 13 ───────────────────────────────────────────────────────

    /**
     * Propriedade 13: Intervalo SRS Cresce Monotonicamente para GOOD/EASY
     *
     * Para qualquer SrsCard em estado REVIEW com reps >= 1, aplicar
     * [FsrsScheduler.schedule] com rating GOOD ou EASY produz
     * `result.scheduledDays > card.scheduledDays`.
     *
     * **Validates: Requisitos 16.4**
     */
    "Propriedade 13 - Intervalo SRS Cresce Monotonicamente para GOOD/EASY em REVIEW" - {

        "GOOD em REVIEW deve produzir scheduledDays > card.scheduledDays" {
            forAll(iterations = 1000, arbReviewCard) { card ->
                val result = FsrsScheduler.schedule(card, SrsRating.GOOD, fixedToday)
                result.scheduledDays > card.scheduledDays
            }
        }

        "EASY em REVIEW deve produzir scheduledDays > card.scheduledDays" {
            forAll(iterations = 1000, arbReviewCard) { card ->
                val result = FsrsScheduler.schedule(card, SrsRating.EASY, fixedToday)
                result.scheduledDays > card.scheduledDays
            }
        }
    }

    // ─── Propriedade 14 ───────────────────────────────────────────────────────

    /**
     * Propriedade 14: AGAIN Sempre Reseta para Intervalo Mínimo (== 1)
     *
     * Para qualquer SrsCard em qualquer estado, aplicar
     * [FsrsScheduler.schedule] com rating AGAIN produz `scheduledDays == 1`.
     *
     * **Validates: Requisitos 16.5**
     */
    "Propriedade 14 - AGAIN Sempre Reseta para Intervalo Mínimo (scheduledDays == 1)" {
        forAll(iterations = 1000, arbAnyCard) { card ->
            val result = FsrsScheduler.schedule(card, SrsRating.AGAIN, fixedToday)
            result.scheduledDays == 1
        }
    }

    // ─── Propriedade 15 ───────────────────────────────────────────────────────

    /**
     * Propriedade 15: Retrievability em [0, 1]
     *
     * Para qualquer `stability > 0` e `elapsedDays >= 0`,
     * [FsrsScheduler.retrievability] retorna um valor no intervalo fechado `[0.0, 1.0]`.
     *
     * **Validates: design interno do FSRS**
     */
    "Propriedade 15 - Retrievability em [0, 1] para qualquer stability > 0 e elapsedDays >= 0" {
        forAll(
            iterations = 1000,
            arbStability,
            arbElapsedDays
        ) { stability, elapsedDays ->
            val r = FsrsScheduler.retrievability(stability, elapsedDays)
            r >= 0f && r <= 1f
        }
    }
})
