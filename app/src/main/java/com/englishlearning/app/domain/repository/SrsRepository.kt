package com.englishlearning.app.domain.repository

import com.englishlearning.app.domain.gamification.SrsRating
import com.englishlearning.app.domain.model.SrsCard
import com.englishlearning.app.domain.model.SrsCardType
import com.englishlearning.app.domain.model.VocabularyCard
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Abstração de acesso aos dados do sistema de Repetição Espaçada (SRS).
 *
 * Gerencia [SrsCard]s associados a exercícios gramaticais (GRAMMAR) e vocabulary cards
 * (VOCABULARY), calculando e persistindo os agendamentos FSRS-4.5 após cada revisão.
 *
 * Esta interface é Pure Kotlin — não importa nenhuma classe do Android SDK,
 * mantendo a camada de domínio independente de infraestrutura.
 *
 * Requisito 16.1: criação de SrsCard após primeira resposta a um exercício.
 * Requisito 16.6: badge com contagem de cards devidos na HomeScreen.
 * Requisito 16.7: apresentar apenas cards devidos (dueDate <= hoje) na SrsReviewScreen.
 * Requisito 16.8: suporte aos tipos GRAMMAR e VOCABULARY.
 * Requisito 17.1: exibir card sem revelar resposta na SrsReviewScreen.
 * Requisito 17.2: revelar resposta e exibir botões de avaliação.
 * Requisito 18.6: persistência completa offline no Room Database.
 */
interface SrsRepository {

    /**
     * Retorna todos os SRS cards devidos do usuário (dueDate <= [today]),
     * em ordem aleatória conforme Requisito 16.7.
     *
     * @param userId  ID do usuário.
     * @param today   Data de referência (padrão: hoje).
     * @return Lista de cards devidos, possivelmente vazia.
     */
    suspend fun getCardsForReview(
        userId: Long,
        today: LocalDate = LocalDate.now()
    ): List<SrsCard>

    /**
     * Retorna a contagem atual de SRS cards devidos do usuário (dueDate <= [today]).
     *
     * Requisito 16.6: badge na HomeScreen exibe esta contagem.
     *
     * @param userId  ID do usuário.
     * @param today   Data de referência (padrão: hoje).
     * @return Número de cards devidos.
     */
    suspend fun getDueCardCount(
        userId: Long,
        today: LocalDate = LocalDate.now()
    ): Int

    /**
     * Retorna o [SrsCard] existente para a combinação (userId, cardType, refId),
     * ou cria um novo card no estado NEW se ainda não existir.
     *
     * Quando um novo card é criado, [dueDate] é definido como hoje e todos os
     * parâmetros FSRS são inicializados com seus valores padrão (stability=0,
     * difficulty=5, reps=0, lapses=0, state=NEW).
     *
     * Requisito 16.1: criação automática de SrsCard após resposta CORRECT/TOO_CLOSE.
     * Requisito 18.1: promoção do estado NEW para LEARNING após primeira resposta.
     *
     * @param userId    ID do usuário.
     * @param cardType  Tipo do card (GRAMMAR ou VOCABULARY).
     * @param refId     ID do exercício (GRAMMAR) ou do vocabulary card (VOCABULARY).
     * @return O card existente ou o novo card recém-criado.
     */
    suspend fun getOrCreateCard(
        userId: Long,
        cardType: SrsCardType,
        refId: Long
    ): SrsCard

    /**
     * Aplica o agendamento FSRS ao [card] com o [rating] informado e persiste
     * o resultado no banco de dados.
     *
     * Delega o cálculo ao [com.englishlearning.app.domain.gamification.FsrsScheduler]
     * e persiste os novos parâmetros (stability, difficulty, scheduledDays, dueDate,
     * lastReviewDate, state) via [com.englishlearning.app.data.database.dao.SrsDao].
     *
     * Requisito 16.3: aceitar Rating e calcular novos parâmetros FSRS.
     * Requisito 16.4: intervalo cresce monotonicamente para GOOD/EASY em REVIEW.
     * Requisito 16.5: AGAIN reseta o intervalo para 1 dia.
     * Requisito 18.6: persistência offline após cada revisão.
     *
     * @param card    O card que foi revisado.
     * @param rating  A avaliação do usuário (AGAIN / HARD / GOOD / EASY).
     * @return O [SrsCard] atualizado com os novos parâmetros FSRS.
     */
    suspend fun updateCardAfterReview(card: SrsCard, rating: SrsRating): SrsCard

    /**
     * Observa reativamente a contagem de SRS cards devidos do usuário.
     *
     * Emite um novo valor sempre que um card for inserido ou atualizado no banco.
     * O [today] fixo (data de hoje ao iniciar a observação) é suficiente para a
     * maioria dos casos de uso; o ViewModel deve recarregar ao retornar ao foreground
     * caso o dia tenha mudado.
     *
     * Requisito 16.6: observable para badge de revisão na HomeScreen.
     *
     * @param userId  ID do usuário.
     * @return Flow que emite a contagem de cards devidos a cada mudança no banco.
     */
    fun observeDueCardCount(userId: Long): Flow<Int>

    /**
     * Retorna vocabulary cards de um nível e categoria específicos.
     *
     * Requisito 16.8: vocabulary cards acessíveis para o fluxo SRS.
     * Requisito 16.9: campos word, definition (PT-BR), exampleSentence (inglês).
     *
     * @param levelId     ID do nível desejado.
     * @param categoryId  ID da categoria desejada.
     * @return Lista de vocabulary cards da combinação, possivelmente vazia.
     */
    suspend fun getVocabularyCards(levelId: Int, categoryId: Int): List<VocabularyCard>

    /**
     * Retorna um vocabulary card pelo seu ID, ou null se não encontrado.
     *
     * Usado pela SrsReviewViewModel para recuperar o conteúdo completo de cards
     * do tipo VOCABULARY durante uma sessão de revisão.
     *
     * @param id ID único do vocabulary card.
     * @return O [VocabularyCard] correspondente, ou null.
     */
    suspend fun getVocabularyCardById(id: Long): VocabularyCard?
}
