package com.englishlearning.app.domain.model

import com.englishlearning.app.domain.gamification.SrsState
import java.time.LocalDate

/**
 * Tipo do SRS card: gramatical (exercício) ou de vocabulário.
 *
 * Requisito 16.8: suporte a SrsCards dos tipos GRAMMAR e VOCABULARY.
 */
enum class SrsCardType {
    GRAMMAR,
    VOCABULARY
}

/**
 * Domain model para um SRS card com todos os parâmetros FSRS-4.5.
 *
 * Representa um exercício gramatical (GRAMMAR) ou um vocabulary card (VOCABULARY)
 * sob o ciclo de repetição espaçada. Os campos [dueDate] e [lastReviewDate] utilizam
 * [LocalDate] em vez de String para facilitar a lógica de agendamento na camada
 * de domínio, sem dependência do Android SDK.
 *
 * Requisito 16.2: armazenar stability, difficulty, elapsedDays, scheduledDays, reps,
 *                 lapses, state (NEW|LEARNING|REVIEW|RELEARNING), dueDate, lastReviewDate.
 * Requisito 16.8: suporte aos tipos GRAMMAR e VOCABULARY.
 * Requisito 18.6: persistência completa offline no Room Database.
 */
data class SrsCard(
    val id: Long = 0,

    /** Identificador do usuário proprietário deste card. */
    val userId: Long,

    /** Tipo do card: GRAMMAR para exercícios gramaticais, VOCABULARY para palavras. */
    val cardType: SrsCardType,

    /**
     * Referência ao ID da entidade-fonte.
     * exerciseId quando cardType = GRAMMAR; vocabularyCardId quando VOCABULARY.
     */
    val refId: Long,

    /** Parâmetro FSRS: força de memória atual. 0f para cards novos. */
    val stability: Float = 0f,

    /** Parâmetro FSRS: dificuldade intrínseca do card em [1.0, 10.0]. */
    val difficulty: Float = 5f,

    /** Parâmetro FSRS: dias decorridos desde a última revisão. */
    val elapsedDays: Int = 0,

    /** Parâmetro FSRS: intervalo agendado em dias até a próxima revisão. */
    val scheduledDays: Int = 0,

    /** Número total de revisões realizadas para este card. */
    val reps: Int = 0,

    /** Número de lapsos (avaliações AGAIN em estado REVIEW). */
    val lapses: Int = 0,

    /** Estado atual do card no ciclo FSRS. */
    val state: SrsState = SrsState.NEW,

    /**
     * Data em que este card fica disponível para revisão.
     * Requisito 16.6: exibir badge na HomeScreen com cards devidos (dueDate <= hoje).
     */
    val dueDate: LocalDate,

    /** Data da última revisão realizada, ou null se nunca revisado. */
    val lastReviewDate: LocalDate? = null
)

/**
 * Domain model para um vocabulary card do sistema SRS.
 *
 * Representa uma palavra em inglês associada à sua definição em português (PT-BR)
 * e a uma sentença de exemplo em inglês, para estudo por repetição espaçada.
 *
 * Requisito 16.9: campos word (inglês), definition (PT-BR), exampleSentence (inglês),
 *                 levelId e categoryId.
 */
data class VocabularyCard(
    val id: Long = 0,

    /** A palavra em inglês a ser estudada. */
    val word: String,

    /** Definição/tradução da palavra em português brasileiro (PT-BR). */
    val definition: String,

    /** Sentença de exemplo em inglês demonstrando o uso contextual da palavra. */
    val exampleSentence: String,

    /** ID do nível ao qual este vocabulary card pertence. */
    val levelId: Int,

    /** ID da categoria à qual este vocabulary card pertence. */
    val categoryId: Int,

    /**
     * Caminho para o arquivo de áudio da pronúncia, relativo ao diretório de assets.
     * Nulo quando o áudio não está disponível.
     */
    val audioPath: String? = null
)
