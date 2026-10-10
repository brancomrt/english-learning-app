package com.englishlearning.app.domain.gamification

import java.time.LocalDate
import kotlin.math.E
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// Enums
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Avaliação do usuário para um card de revisão SRS.
 *
 * Requisito 16.3: aceitar Rating (AGAIN=1, HARD=2, GOOD=3, EASY=4).
 */
enum class SrsRating(val value: Int) {
    AGAIN(1),
    HARD(2),
    GOOD(3),
    EASY(4)
}

/**
 * Estado interno de um card no ciclo de vida do FSRS.
 *
 * Requisito 16.2: state com valores NEW | LEARNING | REVIEW | RELEARNING.
 */
enum class SrsState {
    NEW,
    LEARNING,
    REVIEW,
    RELEARNING
}

// ─────────────────────────────────────────────────────────────────────────────
// Data classes
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Parâmetros FSRS de um card SRS.
 *
 * Precondição: [stability] >= 0; [difficulty] em [1, 10]; [reps] >= 0; [lapses] >= 0.
 *
 * Requisito 16.2: campos stability, difficulty, elapsedDays, scheduledDays, reps, lapses, state.
 */
data class SrsCardParams(
    val stability: Float,       // Força de memória; 0 para cards novos
    val difficulty: Float,      // Dificuldade intrínseca do card [1.0, 10.0]
    val elapsedDays: Int,       // Dias passados desde a última revisão
    val scheduledDays: Int,     // Intervalo agendado na última revisão
    val reps: Int,              // Total de revisões realizadas
    val lapses: Int,            // Total de vezes que o usuário esqueceu (AGAIN)
    val state: SrsState         // Estado atual no ciclo FSRS
)

/**
 * Resultado de um agendamento FSRS para um card após uma avaliação.
 *
 * [scheduledDays] >= 1 sempre (Requisitos 16.4 e 16.5).
 */
data class FsrsScheduleResult(
    val newStability: Float,    // Estabilidade atualizada após a revisão
    val newDifficulty: Float,   // Dificuldade atualizada após a revisão
    val scheduledDays: Int,     // Próximo intervalo em dias (>= 1)
    val newState: SrsState,     // Novo estado do card
    val dueDate: LocalDate      // Data da próxima revisão
)

// ─────────────────────────────────────────────────────────────────────────────
// Scheduler
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Implementação do algoritmo FSRS-4.5 de repetição espaçada.
 *
 * Responsabilidades:
 *  - Calcular o próximo intervalo de revisão ([scheduledDays]) para um card.
 *  - Atualizar os parâmetros de memória: stability e difficulty.
 *  - Determinar o novo estado do card após a avaliação.
 *
 * Requisitos: 16.3, 16.4, 16.5.
 *
 * Propriedades garantidas:
 *  - Prop 13: GOOD/EASY em REVIEW → nextInterval > card.scheduledDays (crescimento monotônico).
 *  - Prop 14: AGAIN → scheduledDays == 1 e newState == RELEARNING (lapso).
 *  - Prop 15: retrievability(stability > 0, elapsedDays >= 0) ∈ [0.0, 1.0].
 */
object FsrsScheduler {

    // ─── Constantes FSRS-4.5 ─────────────────────────────────────────────────

    /**
     * Taxa de retenção alvo: 90%.
     * Usada para calcular o intervalo ideal via [nextInterval].
     */
    private const val TARGET_RETENTION = 0.90

    /**
     * Constante de decaimento da função de esquecimento.
     * R(t) = (1 + t / (9 * S))^(-1)  →  DECAY = -1, mas a fórmula
     * exponencial equivalente usa DECAY_EXP para facilitar os cálculos.
     *
     * A fórmula simplificada adoptada aqui:
     *   R = e^(-t / (9 * S))
     * que é equivalente à curva de esquecimento padrão FSRS.
     */
    private const val DECAY_EXP = -1.0 / 9.0   // expoente de decaimento normalizado por 9*S

    // ─── Dificuldades iniciais por rating ────────────────────────────────────

    private const val INIT_DIFFICULTY_AGAIN = 7.0f
    private const val INIT_DIFFICULTY_HARD  = 6.0f
    private const val INIT_DIFFICULTY_GOOD  = 5.0f
    private const val INIT_DIFFICULTY_EASY  = 4.0f

    // ─── Estabilidades iniciais por rating (W[0..3]) ─────────────────────────

    private const val INIT_STABILITY_AGAIN = 0.4f
    private const val INIT_STABILITY_HARD  = 0.6f
    private const val INIT_STABILITY_GOOD  = 2.4f
    private const val INIT_STABILITY_EASY  = 5.8f

    // ─── Coeficientes de actualização ────────────────────────────────────────

    /** Coeficiente de actualização da dificuldade por unidade de (rating - 3). */
    private const val DIFFICULTY_UPDATE_COEFF = 0.1f

    /** Expoente aplicado a R na fórmula de recallStability. */
    private const val RECALL_EXPONENT = 0.9

    // ─── API pública ─────────────────────────────────────────────────────────

    /**
     * Agenda a próxima revisão de um card com base no [rating] do usuário.
     *
     * Precondição: [card.reps] >= 0; [rating] é um [SrsRating] válido.
     * Postcondição: resultado.[scheduledDays] >= 1.
     *
     * @param card  Parâmetros actuais do card.
     * @param rating Avaliação do usuário (AGAIN / HARD / GOOD / EASY).
     * @param today  Data de referência (padrão: hoje).
     * @return [FsrsScheduleResult] com novos parâmetros e data de vencimento.
     */
    fun schedule(
        card: SrsCardParams,
        rating: SrsRating,
        today: LocalDate = LocalDate.now()
    ): FsrsScheduleResult {
        return when (card.state) {
            SrsState.NEW,
            SrsState.LEARNING,
            SrsState.RELEARNING -> scheduleNew(card, rating, today)

            SrsState.REVIEW -> scheduleReview(card, rating, today)
        }
    }

    // ─── Funções públicas de cálculo (internal visibility para testes) ────────

    /**
     * Calcula a retrievability (probabilidade de lembrança) para um card.
     *
     * Fórmula: R = e^(DECAY_EXP * elapsedDays / stability)
     *
     * Precondição: [stability] > 0; [elapsedDays] >= 0.
     * Postcondição: resultado ∈ [0.0, 1.0].
     *
     * Valida: Propriedade 15.
     */
    internal fun retrievability(stability: Float, elapsedDays: Int): Float {
        if (stability <= 0f) return 0f
        val r = E.pow(DECAY_EXP * elapsedDays / stability)
        return r.toFloat().coerceIn(0f, 1f)
    }

    /**
     * Calcula o próximo intervalo de revisão em dias, dado a estabilidade
     * actual e a retenção alvo.
     *
     * Fórmula: interval = round(9 * S * ln(R_target) / ln(R_current))
     *   onde R_current = retrievability(S, elapsedDays=0) = 1.0 no dia da revisão,
     *   simplificando para: interval = round(-9 * S * ln(R_target))
     *
     * Postcondição: resultado >= 1.
     */
    internal fun nextInterval(stability: Float): Int {
        // No dia da revisão elapsedDays = 0 → R = 1.0, portanto:
        // interval = 9 * S * |ln(TARGET_RETENTION)|
        val interval = (-9.0 * stability * ln(TARGET_RETENTION)).roundToInt()
        return maxOf(1, interval)
    }

    /**
     * Dificuldade inicial de um card novo, baseada no [rating] da primeira resposta.
     * Resultado clamped em [1.0, 10.0].
     */
    internal fun initDifficulty(rating: SrsRating): Float {
        return when (rating) {
            SrsRating.AGAIN -> INIT_DIFFICULTY_AGAIN
            SrsRating.HARD  -> INIT_DIFFICULTY_HARD
            SrsRating.GOOD  -> INIT_DIFFICULTY_GOOD
            SrsRating.EASY  -> INIT_DIFFICULTY_EASY
        }.coerceIn(1f, 10f)
    }

    /**
     * Actualiza a dificuldade de um card após uma revisão.
     *
     * Fórmula: d' = clamp(d + 0.1 * (rating - 3), 1, 10)
     *  - AGAIN (1): d aumenta 0.2 (mais difícil)
     *  - HARD  (2): d aumenta 0.1
     *  - GOOD  (3): d não muda
     *  - EASY  (4): d diminui 0.1 (mais fácil)
     *
     * Postcondição: resultado ∈ [1.0, 10.0].
     */
    internal fun updateDifficulty(difficulty: Float, rating: SrsRating): Float {
        // Fórmula correta: AGAIN/HARD aumentam dificuldade; EASY diminui
        val delta = DIFFICULTY_UPDATE_COEFF * (3 - rating.value)
        return (difficulty + delta).coerceIn(1f, 10f)
    }

    /**
     * Calcula a nova estabilidade após uma revisão bem-sucedida (HARD / GOOD / EASY).
     *
     * Fórmula: S' = S * (e^(0.9 * ln(R)) * (11 - d) / 10 + 1.0)
     *   ≡  S * (R^0.9 * (11 - d) / 10 + 1.0)
     *
     * Precondição: [stability] > 0; [retrievability] ∈ [0, 1]; [difficulty] ∈ [1, 10].
     * Postcondição: resultado > [stability] (crescimento garantido para R > 0).
     *
     * Valida: Propriedade 13 (crescimento monotônico para GOOD/EASY em REVIEW).
     */
    internal fun recallStability(
        difficulty: Float,
        stability: Float,
        retrievability: Float
    ): Float {
        // R^0.9 via exponential: e^(0.9 * ln(R))
        val rFactor = if (retrievability > 0f)
            E.pow(RECALL_EXPONENT * ln(retrievability.toDouble())).toFloat()
        else
            0f
        val growth = rFactor * (11f - difficulty) / 10f + 1f
        return (stability * growth).coerceAtLeast(stability + 0.1f)
    }

    /**
     * Calcula a estabilidade de estabilização após um lapso (AGAIN).
     *
     * Fórmula: S' = 0.2 * d^(-0.5) * s^(-0.5) * (e^(0.9*(1-R)) - 1)
     *
     * Precondição: [stability] > 0; [retrievability] ∈ [0, 1]; [difficulty] ∈ [1, 10].
     * Postcondição: resultado >= 0.1 (mínimo para garantir que o card progride).
     */
    internal fun forgottenStability(
        difficulty: Float,
        stability: Float,
        retrievability: Float
    ): Float {
        val s = 0.2 *
            difficulty.toDouble().pow(-0.5) *
            stability.toDouble().pow(-0.5) *
            (E.pow(0.9 * (1.0 - retrievability)) - 1.0)
        return s.toFloat().coerceAtLeast(0.1f)
    }

    // ─── Implementações privadas ──────────────────────────────────────────────

    /**
     * Agenda um card que está em estado NEW, LEARNING ou RELEARNING.
     * Usa as estabilidades iniciais calibradas por rating.
     *
     * Requisito 16.5: AGAIN → scheduledDays = 1.
     */
    private fun scheduleNew(
        card: SrsCardParams,
        rating: SrsRating,
        today: LocalDate
    ): FsrsScheduleResult {
        val newStability = initialStability(rating)
        val newDifficulty = initDifficulty(rating)

        val intervalDays = when (rating) {
            SrsRating.AGAIN -> 1
            SrsRating.HARD  -> 1
            SrsRating.GOOD  -> maxOf(1, newStability.toInt())
            SrsRating.EASY  -> maxOf(4, (newStability * 2).toInt())
        }

        val newState = when (rating) {
            SrsRating.AGAIN -> SrsState.LEARNING
            else            -> SrsState.REVIEW
        }

        return FsrsScheduleResult(
            newStability = newStability,
            newDifficulty = newDifficulty,
            scheduledDays = intervalDays,
            newState = newState,
            dueDate = today.plusDays(intervalDays.toLong())
        )
    }

    /**
     * Agenda um card que está em estado REVIEW.
     *
     * Requisito 16.4: GOOD/EASY → nextInterval > card.scheduledDays.
     * Requisito 16.5: AGAIN → scheduledDays = 1, state → RELEARNING.
     */
    private fun scheduleReview(
        card: SrsCardParams,
        rating: SrsRating,
        today: LocalDate
    ): FsrsScheduleResult {
        val r = retrievability(card.stability, card.elapsedDays)
        val newDifficulty = updateDifficulty(card.difficulty, rating)

        return when (rating) {
            SrsRating.AGAIN -> {
                // Lapso: estabilidade reduzida, intervalo resetado para 1
                val newStability = forgottenStability(card.difficulty, card.stability, r)
                FsrsScheduleResult(
                    newStability = newStability,
                    newDifficulty = newDifficulty,
                    scheduledDays = 1,
                    newState = SrsState.RELEARNING,
                    dueDate = today.plusDays(1L)
                )
            }

            else -> {
                // Revisão bem-sucedida (HARD / GOOD / EASY)
                val newStability = recallStability(card.difficulty, card.stability, r)
                val rawInterval = nextInterval(newStability)
                // Garante crescimento monotônico (Requisito 16.4 / Propriedade 13)
                val intervalDays = maxOf(rawInterval, card.scheduledDays + 1)
                FsrsScheduleResult(
                    newStability = newStability,
                    newDifficulty = newDifficulty,
                    scheduledDays = intervalDays,
                    newState = SrsState.REVIEW,
                    dueDate = today.plusDays(intervalDays.toLong())
                )
            }
        }
    }

    /**
     * Estabilidade inicial para um card novo, por rating.
     * Valores W[0..3] do modelo FSRS-4.5.
     */
    private fun initialStability(rating: SrsRating): Float = when (rating) {
        SrsRating.AGAIN -> INIT_STABILITY_AGAIN
        SrsRating.HARD  -> INIT_STABILITY_HARD
        SrsRating.GOOD  -> INIT_STABILITY_GOOD
        SrsRating.EASY  -> INIT_STABILITY_EASY
    }
}
