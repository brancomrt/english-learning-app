package com.englishlearning.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade Room para SRS cards (Spaced Repetition System).
 *
 * Cada card associa um exercício gramatical (GRAMMAR) ou card de vocabulário
 * (VOCABULARY) aos parâmetros do algoritmo FSRS-4.5. Os parâmetros FSRS
 * controlam o agendamento da próxima revisão com base na estabilidade da
 * memória e na dificuldade percebida do conteúdo.
 *
 * [dueDate] e [lastReviewDate] são armazenados no formato ISO "yyyy-MM-dd"
 * e convertidos para [java.time.LocalDate] pelos TypeConverters da classe Converters.
 *
 * Requisito 16.2: armazenar todos os parâmetros FSRS por card.
 * Requisito 18.6: persistência completa offline no Room Database.
 */
@Entity(
    tableName = "srs_cards",
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["ref_id"]),
        Index(value = ["due_date"])
    ]
)
data class SrsCardEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    /** Identificador do usuário proprietário deste card. */
    @ColumnInfo(name = "user_id")
    val userId: Long,

    /**
     * Tipo do card: "GRAMMAR" para exercícios gramaticais, "VOCABULARY" para
     * vocabulary cards. Mapeado para o enum [SrsCardType] via TypeConverter.
     */
    @ColumnInfo(name = "card_type")
    val cardType: String,

    /**
     * Referência ao ID da entidade-fonte: exerciseId quando cardType = GRAMMAR,
     * ou vocabularyCardId quando cardType = VOCABULARY.
     */
    @ColumnInfo(name = "ref_id")
    val refId: Long,

    /**
     * Parâmetro FSRS: força de memória atual. Valores maiores indicam
     * maior intervalo de retenção antes do esquecimento.
     * Inicializado em 0.0 para cards novos.
     */
    @ColumnInfo(name = "stability")
    val stability: Float = 0f,

    /**
     * Parâmetro FSRS: dificuldade intrínseca do card no intervalo [1, 10].
     * Valores maiores indicam material mais difícil de reter.
     * Inicializado em 5.0 (dificuldade média) para cards novos.
     */
    @ColumnInfo(name = "difficulty")
    val difficulty: Float = 5f,

    /**
     * Parâmetro FSRS: dias decorridos desde a última revisão.
     * Recalculado a cada revisão com base nas datas.
     */
    @ColumnInfo(name = "elapsed_days")
    val elapsedDays: Int = 0,

    /**
     * Parâmetro FSRS: intervalo agendado em dias até a próxima revisão.
     * Calculado pelo FsrsScheduler em cada revisão.
     */
    @ColumnInfo(name = "scheduled_days")
    val scheduledDays: Int = 0,

    /**
     * Número total de revisões realizadas para este card.
     * Incrementado em 1 a cada revisão.
     */
    @ColumnInfo(name = "reps")
    val reps: Int = 0,

    /**
     * Número de lapsos (esquecimentos): vezes em que o usuário avaliou AGAIN
     * após o card estar no estado REVIEW.
     */
    @ColumnInfo(name = "lapses")
    val lapses: Int = 0,

    /**
     * Estado atual do card no ciclo de aprendizado FSRS:
     * "NEW" | "LEARNING" | "REVIEW" | "RELEARNING".
     * Armazenado como String; mapeado via TypeConverter para SrsState.
     */
    @ColumnInfo(name = "state")
    val state: String = "NEW",

    /**
     * Data em que este card fica disponível para revisão.
     * Armazenada como String no formato "yyyy-MM-dd" (ISO 8601).
     * Requisito 16.6: exibir badge na HomeScreen com cards devidos (dueDate <= hoje).
     */
    @ColumnInfo(name = "due_date")
    val dueDate: String,

    /**
     * Data da última revisão realizada, ou null se o card nunca foi revisado.
     * Armazenada como String no formato "yyyy-MM-dd" (ISO 8601), ou null.
     */
    @ColumnInfo(name = "last_review_date")
    val lastReviewDate: String? = null
)
