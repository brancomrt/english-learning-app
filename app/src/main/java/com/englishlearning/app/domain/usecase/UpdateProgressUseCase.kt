package com.englishlearning.app.domain.usecase

import com.englishlearning.app.domain.model.AttemptResult
import com.englishlearning.app.domain.model.SrsCardType
import com.englishlearning.app.domain.model.ValidationStatus
import com.englishlearning.app.domain.repository.ProgressRepository
import com.englishlearning.app.domain.repository.SrsRepository
import javax.inject.Inject

/**
 * Persiste o resultado de uma tentativa, atualiza o streak diário do usuário
 * e cria/obtém um SRS card para exercícios respondidos corretamente.
 *
 * Requisito 13.1: LearningViewModel atualiza o progresso após cada validação.
 * Requisito 9.3: registra a tentativa com userAnswer, validationStatus, score,
 *               xpEarned e timestamp no banco de dados local.
 * Requisito 5.1: registra atividade do dia ao completar um exercício.
 * Requisito 16.1: criação automática de SrsCard após resposta CORRECT ou TOO_CLOSE.
 * Requisito 18.1: promoção automática do estado NEW para LEARNING.
 *
 * @param progressRepository Repositório de progresso do usuário.
 * @param srsRepository      Repositório SRS para criar/obter cards de revisão espaçada.
 */
class UpdateProgressUseCase @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val srsRepository: SrsRepository
) {

    /**
     * Persiste [attemptResult], atualiza o streak do usuário identificado por [userId],
     * e — quando a resposta é CORRECT ou TOO_CLOSE — garante que um [SrsCard] de tipo
     * GRAMMAR exista para o exercício, criando-o na primeira vez.
     *
     * As operações de persistência de tentativa e streak são executadas sempre.
     * A criação/obtenção do SrsCard ocorre somente para respostas bem-sucedidas,
     * evitando criar cards para tentativas incorretas.
     *
     * Precondição: [userId] é um ID de usuário válido no banco de dados.
     * Precondição: [attemptResult] contém exerciseId, userAnswer e validationResult preenchidos.
     *
     * @param userId        ID do usuário que realizou a tentativa.
     * @param levelId       ID do nível do exercício tentado (reservado para uso futuro).
     * @param categoryId    ID da categoria do exercício tentado (reservado para uso futuro).
     * @param attemptResult Resultado completo da tentativa a ser persistida.
     */
    suspend operator fun invoke(
        userId: Long,
        levelId: Int,
        categoryId: Int,
        attemptResult: AttemptResult
    ) {
        progressRepository.updateProgress(userId, attemptResult)
        progressRepository.updateStreak(userId)

        // Requisitos 16.1, 18.1: criar/obter SrsCard do tipo GRAMMAR para o exercício
        // apenas quando o usuário acertou (CORRECT) ou chegou muito perto (TOO_CLOSE).
        val status = attemptResult.validationResult.status
        if (status == ValidationStatus.CORRECT || status == ValidationStatus.TOO_CLOSE) {
            srsRepository.getOrCreateCard(
                userId = userId,
                cardType = SrsCardType.GRAMMAR,
                refId = attemptResult.exerciseId
            )
        }
    }
}
