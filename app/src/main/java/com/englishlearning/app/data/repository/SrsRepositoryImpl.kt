package com.englishlearning.app.data.repository

import com.englishlearning.app.data.database.dao.SrsDao
import com.englishlearning.app.data.database.dao.VocabularyDao
import com.englishlearning.app.data.database.entity.SrsCardEntity
import com.englishlearning.app.data.database.entity.VocabularyCardEntity
import com.englishlearning.app.domain.gamification.FsrsScheduler
import com.englishlearning.app.domain.gamification.SrsCardParams
import com.englishlearning.app.domain.gamification.SrsRating
import com.englishlearning.app.domain.gamification.SrsState
import com.englishlearning.app.domain.model.SrsCard
import com.englishlearning.app.domain.model.SrsCardType
import com.englishlearning.app.domain.model.VocabularyCard
import com.englishlearning.app.domain.repository.SrsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Implementação de [SrsRepository] que persiste SRS cards e vocabulary cards
 * no Room Database via [SrsDao] e [VocabularyDao], e delega o cálculo de
 * agendamento ao [FsrsScheduler].
 *
 * Requisito 16.1: criação automática de SrsCard após primeira resposta CORRECT/TOO_CLOSE.
 * Requisito 16.3–16.5: agendamento FSRS via FsrsScheduler.
 * Requisito 16.6: contagem reativa de cards devidos para badge na HomeScreen.
 * Requisito 16.7: apenas cards com dueDate <= hoje exibidos na SrsReviewScreen.
 * Requisito 16.8: suporte a tipos GRAMMAR e VOCABULARY.
 * Requisito 16.9: vocabulary cards com word, definition (PT-BR), exampleSentence (inglês).
 * Requisito 17.1, 17.2: active recall na SrsReviewScreen.
 * Requisito 18.6: persistência offline completa.
 */
class SrsRepositoryImpl @Inject constructor(
    private val srsDao: SrsDao,
    private val vocabularyDao: VocabularyDao
) : SrsRepository {

    /**
     * Retorna todos os SRS cards devidos do usuário (dueDate <= [today]),
     * mapeados para domain models.
     *
     * Requisito 16.7: SrsReviewScreen apresenta apenas cards com dueDate <= hoje.
     */
    override suspend fun getCardsForReview(userId: Long, today: LocalDate): List<SrsCard> {
        return srsDao.getDueCards(userId, today.toString())
            .map { it.toDomain() }
    }

    /**
     * Retorna a contagem de SRS cards devidos do usuário para a data fornecida.
     *
     * Requisito 16.6: badge na HomeScreen exibe esta contagem.
     */
    override suspend fun getDueCardCount(userId: Long, today: LocalDate): Int {
        return srsDao.getDueCardCount(userId, today.toString())
    }

    /**
     * Retorna o [SrsCard] existente para (userId, cardType, refId), ou cria um
     * novo card no estado NEW com dueDate = hoje e parâmetros FSRS padrão.
     *
     * Requisito 16.1: criação automática de SrsCard após primeira resposta.
     * Requisito 18.1: promoção para LEARNING ocorrerá na primeira chamada a
     *                 [updateCardAfterReview] após a criação.
     */
    override suspend fun getOrCreateCard(
        userId: Long,
        cardType: SrsCardType,
        refId: Long
    ): SrsCard {
        val existing = srsDao.getCardByRef(userId, cardType.name, refId)
        if (existing != null) {
            return existing.toDomain()
        }

        // Criar novo card com estado NEW e dueDate = hoje
        val newEntity = SrsCardEntity(
            userId = userId,
            cardType = cardType.name,
            refId = refId,
            stability = 0f,
            difficulty = 5f,
            elapsedDays = 0,
            scheduledDays = 0,
            reps = 0,
            lapses = 0,
            state = SrsState.NEW.name,
            dueDate = LocalDate.now().toString(),
            lastReviewDate = null
        )
        srsDao.upsertCard(newEntity)

        // Recarregar para obter o id gerado pelo Room
        val inserted = srsDao.getCardByRef(userId, cardType.name, refId)
            ?: newEntity  // Fallback improvável: retornar a entidade sem id persistido
        return inserted.toDomain()
    }

    /**
     * Aplica o agendamento FSRS ao card com o rating informado, persiste os novos
     * parâmetros e retorna o [SrsCard] atualizado.
     *
     * O [elapsedDays] é calculado como a diferença entre hoje e [card.lastReviewDate]
     * (ou 0 se nunca revisado), para alimentar corretamente a função de retrievability
     * do [FsrsScheduler].
     *
     * Requisito 16.3: aceitar Rating e calcular novos parâmetros FSRS.
     * Requisito 16.4: intervalo crescente para GOOD/EASY em REVIEW.
     * Requisito 16.5: AGAIN reseta o intervalo para 1 dia.
     * Requisito 18.6: persistência offline.
     */
    override suspend fun updateCardAfterReview(card: SrsCard, rating: SrsRating): SrsCard {
        val today = LocalDate.now()

        // Calcular elapsedDays com base na última revisão
        val elapsedDays = card.lastReviewDate?.let { lastDate ->
            java.time.temporal.ChronoUnit.DAYS.between(lastDate, today).toInt().coerceAtLeast(0)
        } ?: card.elapsedDays

        val params = SrsCardParams(
            stability = card.stability,
            difficulty = card.difficulty,
            elapsedDays = elapsedDays,
            scheduledDays = card.scheduledDays,
            reps = card.reps,
            lapses = card.lapses,
            state = card.state
        )

        val scheduleResult = FsrsScheduler.schedule(params, rating, today)

        // Construir entidade atualizada preservando o id e os campos de identidade
        val updatedEntity = SrsCardEntity(
            id = card.id,
            userId = card.userId,
            cardType = card.cardType.name,
            refId = card.refId,
            stability = scheduleResult.newStability,
            difficulty = scheduleResult.newDifficulty,
            elapsedDays = elapsedDays,
            scheduledDays = scheduleResult.scheduledDays,
            reps = params.reps + 1,
            lapses = if (rating == SrsRating.AGAIN) params.lapses + 1 else params.lapses,
            state = scheduleResult.newState.name,
            dueDate = scheduleResult.dueDate.toString(),
            lastReviewDate = today.toString()
        )

        srsDao.upsertCard(updatedEntity)

        return updatedEntity.toDomain()
    }

    /**
     * Observa reativamente a contagem de SRS cards devidos do usuário,
     * usando a data de hoje no momento da primeira emissão.
     *
     * Requisito 16.6: observable para badge de revisão na HomeScreen.
     */
    override fun observeDueCardCount(userId: Long): Flow<Int> {
        val today = LocalDate.now().toString()
        return srsDao.observeDueCount(userId, today)
    }

    /**
     * Retorna vocabulary cards de um nível e categoria específicos, mapeados
     * para domain models.
     *
     * Requisito 16.8: vocabulary cards acessíveis para o fluxo SRS.
     */
    override suspend fun getVocabularyCards(levelId: Int, categoryId: Int): List<VocabularyCard> {
        return vocabularyDao.getByLevelAndCategory(levelId, categoryId)
            .map { it.toDomain() }
    }

    /**
     * Retorna um vocabulary card pelo seu ID, ou null se não encontrado.
     *
     * Usado pela SrsReviewViewModel para carregar o conteúdo de cards VOCABULARY
     * durante a sessão de revisão ativa.
     */
    override suspend fun getVocabularyCardById(id: Long): VocabularyCard? {
        return vocabularyDao.getById(id)?.toDomain()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Mapper extensions
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Converte [SrsCardEntity] (Room) para o domain model [SrsCard].
 *
 * [dueDate] é parseado de String "yyyy-MM-dd" para [LocalDate].
 * [lastReviewDate] é parseado da mesma forma, ou null se ausente.
 * [state] é convertido para [SrsState] com fallback para NEW.
 * [cardType] é convertido para [SrsCardType] com fallback para GRAMMAR.
 */
internal fun SrsCardEntity.toDomain(): SrsCard {
    return SrsCard(
        id = id,
        userId = userId,
        cardType = cardType.toSrsCardType(),
        refId = refId,
        stability = stability,
        difficulty = difficulty,
        elapsedDays = elapsedDays,
        scheduledDays = scheduledDays,
        reps = reps,
        lapses = lapses,
        state = state.toSrsState(),
        dueDate = LocalDate.parse(dueDate),
        lastReviewDate = lastReviewDate?.let { LocalDate.parse(it) }
    )
}

/**
 * Converte [VocabularyCardEntity] (Room) para o domain model [VocabularyCard].
 */
internal fun VocabularyCardEntity.toDomain(): VocabularyCard {
    return VocabularyCard(
        id = id,
        word = word,
        definition = definition,
        exampleSentence = exampleSentence,
        levelId = levelId,
        categoryId = categoryId,
        audioPath = audioPath
    )
}

/**
 * Converte String para [SrsCardType] com fallback para [SrsCardType.GRAMMAR]
 * caso o valor armazenado não seja reconhecido.
 */
private fun String.toSrsCardType(): SrsCardType {
    return try {
        SrsCardType.valueOf(this)
    } catch (e: IllegalArgumentException) {
        SrsCardType.GRAMMAR
    }
}

/**
 * Converte String para [SrsState] com fallback para [SrsState.NEW]
 * caso o valor armazenado não seja reconhecido.
 */
private fun String.toSrsState(): SrsState {
    return try {
        SrsState.valueOf(this)
    } catch (e: IllegalArgumentException) {
        SrsState.NEW
    }
}
