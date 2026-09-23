package com.englishlearning.app.ui.srs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.domain.gamification.SrsRating
import com.englishlearning.app.domain.model.Exercise
import com.englishlearning.app.domain.model.SrsCard
import com.englishlearning.app.domain.model.SrsCardType
import com.englishlearning.app.domain.model.VocabularyCard
import com.englishlearning.app.domain.repository.ContentRepository
import com.englishlearning.app.domain.repository.SrsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// UI State models
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Progresso da sessão de revisão SRS ativa.
 *
 * Requisito 17.6: SrsReviewScreen exibe progresso (cards revisados / cards devidos total).
 *
 * @property currentIndex  Índice do card exibido atualmente (começa em 0).
 * @property totalCards    Total de cards devidos que compõem a sessão.
 */
data class ReviewProgress(
    val currentIndex: Int,
    val totalCards: Int
)

/**
 * Estatísticas da sessão de revisão SRS ao final.
 *
 * Requisito 17.7: exibir tela de conclusão com total revisado e distribuição de ratings.
 *
 * @property totalReviewed  Total de cards revisados nesta sessão.
 * @property againCount     Quantidade de avaliações AGAIN.
 * @property hardCount      Quantidade de avaliações HARD.
 * @property goodCount      Quantidade de avaliações GOOD.
 * @property easyCount      Quantidade de avaliações EASY.
 */
data class ReviewSessionStats(
    val totalReviewed: Int,
    val againCount: Int,
    val hardCount: Int,
    val goodCount: Int,
    val easyCount: Int
)

/**
 * Conteúdo rico de um card para ser exibido na tela de revisão.
 *
 * Separa estímulo (visível antes da revelação) de resposta (visível somente após revelar),
 * independente do tipo do card (GRAMMAR ou VOCABULARY).
 *
 * Requisito 17.1: exibir apenas o estímulo sem revelar a resposta esperada.
 * Requisito 17.2: ao pressionar "Revelar", mostrar a resposta esperada.
 *
 * @property stimulus   Texto do estímulo exibido antes da revelação.
 * @property answer     Texto da resposta revelada ao pressionar "Revelar".
 * @property detail     Informação adicional opcional (ex: sentença de exemplo para vocab).
 */
data class ReviewCardContent(
    val stimulus: String,
    val answer: String,
    val detail: String? = null
)

/**
 * Estado da tela de revisão SRS.
 *
 * Requisito 17.1–17.7: fluxo completo da SrsReviewScreen.
 */
sealed class SrsReviewUiState {

    /** Carregando os cards devidos do repositório. */
    object Loading : SrsReviewUiState()

    /**
     * Exibindo um card para revisão ativa.
     *
     * Requisito 17.1: [isRevealed] = false → apenas o estímulo é exibido.
     * Requisito 17.2: [isRevealed] = true → estímulo + resposta + botões de avaliação.
     * Requisito 17.6: [progress] para exibir o progresso da sessão.
     */
    data class ShowingCard(
        val card: SrsCard,
        val content: ReviewCardContent,
        val isRevealed: Boolean,
        val progress: ReviewProgress
    ) : SrsReviewUiState()

    /**
     * Todos os cards devidos foram revisados — exibir estatísticas finais.
     *
     * Requisito 17.7: tela de conclusão com [stats].
     */
    data class SessionComplete(
        val stats: ReviewSessionStats
    ) : SrsReviewUiState()

    /**
     * Nenhum card devido hoje — sessão vazia.
     *
     * Requisito 16.6: badge exibido apenas quando dueCardCount > 0; ao abrir a tela
     *                 sem cards devidos, este estado é emitido.
     */
    object Empty : SrsReviewUiState()

    /**
     * Erro ao carregar os cards devidos.
     *
     * Requisito 12.7: exibir mensagem de erro amigável com opção de retry.
     */
    data class Error(val message: String) : SrsReviewUiState()
}

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

/** ID do único usuário do app (modo single-user). */
private const val DEFAULT_USER_ID = 1L

/**
 * ViewModel da SrsReviewScreen — gerencia a sessão de revisão por repetição espaçada (SRS).
 *
 * Responsabilidades:
 * - [loadDueCards]: carrega cards com `dueDate <= hoje`, embaralha e inicia a sessão (Req. 16.7).
 * - [revealAnswer]: transiciona [SrsReviewUiState.ShowingCard.isRevealed] para true (Req. 17.2).
 * - [submitRating]: aplica o agendamento FSRS via [SrsRepository.updateCardAfterReview] e
 *   avança para o próximo card (Req. 17.3, 17.4, 17.5).
 * - [completeSession]: calcula estatísticas finais da sessão (Req. 17.7).
 * - Estado exposto como [StateFlow] de [SrsReviewUiState] (Req. 17.6).
 *
 * @param srsRepository       Repositório SRS para carga e atualização de cards.
 * @param contentRepository   Repositório de conteúdo para buscar exercícios GRAMMAR.
 */
@HiltViewModel
class SrsReviewViewModel @Inject constructor(
    private val srsRepository: SrsRepository,
    private val contentRepository: ContentRepository
) : ViewModel() {

    // ─────────────────────────────────────────────────────────────────────────
    // State
    // ─────────────────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow<SrsReviewUiState>(SrsReviewUiState.Loading)

    /**
     * Estado observável da SrsReviewScreen.
     *
     * Requisito 17.1–17.7: gerenciado via StateFlow.
     */
    val uiState: StateFlow<SrsReviewUiState> = _uiState.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // Internal session state
    // ─────────────────────────────────────────────────────────────────────────

    /** Lista de cards devidos embaralhados que compõem a sessão atual. */
    private var sessionCards: List<SrsCard> = emptyList()

    /** Índice do card exibido atualmente. */
    private var currentIndex: Int = 0

    /** Contagem de avaliações por rating para o resumo final. */
    private var againCount = 0
    private var hardCount = 0
    private var goodCount = 0
    private var easyCount = 0

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Carrega os cards devidos para o usuário (dueDate <= hoje), embaralha-os
     * aleatoriamente e inicia a sessão de revisão.
     *
     * Transições de estado:
     * - [SrsReviewUiState.Loading]     → durante o carregamento.
     * - [SrsReviewUiState.Empty]       → quando não há cards devidos hoje.
     * - [SrsReviewUiState.ShowingCard] → quando há cards devidos.
     * - [SrsReviewUiState.Error]       → em caso de falha.
     *
     * Requisito 16.7: SrsReviewScreen apresenta apenas cards com dueDate <= hoje,
     *                 em ordem aleatória.
     * Requisito 17.1: ao mostrar o primeiro card, isRevealed = false.
     *
     * @param userId ID do usuário (padrão: [DEFAULT_USER_ID]).
     */
    fun loadDueCards(userId: Long = DEFAULT_USER_ID) {
        viewModelScope.launch {
            _uiState.value = SrsReviewUiState.Loading
            try {
                val dueCards = srsRepository.getCardsForReview(userId, LocalDate.now())
                if (dueCards.isEmpty()) {
                    _uiState.value = SrsReviewUiState.Empty
                    return@launch
                }

                // Embaralhar conforme Requisito 16.7
                sessionCards = dueCards.shuffled()
                currentIndex = 0
                resetRatingCounts()

                showCurrentCard()
            } catch (e: Exception) {
                _uiState.value = SrsReviewUiState.Error(
                    e.message ?: "Erro ao carregar revisões. Tente novamente."
                )
            }
        }
    }

    /**
     * Revela a resposta do card atual, exibindo os botões de avaliação.
     *
     * Ignorado silenciosamente se o estado atual não for [SrsReviewUiState.ShowingCard]
     * ou se a resposta já estiver revelada.
     *
     * Requisito 17.2: ao pressionar "Revelar", mostrar a resposta esperada e os
     *                 4 botões de avaliação (AGAIN / HARD / GOOD / EASY).
     */
    fun revealAnswer() {
        val current = _uiState.value as? SrsReviewUiState.ShowingCard ?: return
        if (current.isRevealed) return
        _uiState.value = current.copy(isRevealed = true)
    }

    /**
     * Registra a avaliação do usuário para o card atual, atualiza os parâmetros
     * FSRS via [SrsRepository.updateCardAfterReview] e avança para o próximo card.
     *
     * Fluxo:
     * 1. Persiste a avaliação no repositório SRS (novo agendamento FSRS).
     * 2. Incrementa o contador de ratings para o resumo final.
     * 3. Avança para o próximo card ou transiciona para [SrsReviewUiState.SessionComplete].
     *
     * Ignorado silenciosamente se o estado atual não for [SrsReviewUiState.ShowingCard].
     *
     * Requisito 17.3: exibir botões AGAIN / HARD / GOOD / EASY após revelação.
     * Requisito 17.4: AGAIN → feedback de intervalo curto (card volta em breve).
     * Requisito 17.5: GOOD/EASY → exibir próximo intervalo calculado.
     * Requisito 17.6: avançar o índice de progresso da sessão.
     *
     * @param rating Avaliação do usuário: AGAIN, HARD, GOOD ou EASY.
     */
    fun submitRating(rating: SrsRating) {
        val current = _uiState.value as? SrsReviewUiState.ShowingCard ?: return
        val card = current.card

        viewModelScope.launch {
            try {
                // 1. Atualizar o card no repositório com o agendamento FSRS
                srsRepository.updateCardAfterReview(card, rating)

                // 2. Incrementar contador de ratings para estatísticas finais
                incrementRatingCount(rating)

                // 3. Avançar para o próximo card ou concluir a sessão
                if (currentIndex < sessionCards.size - 1) {
                    currentIndex++
                    showCurrentCard()
                } else {
                    completeSession()
                }
            } catch (e: Exception) {
                _uiState.value = SrsReviewUiState.Error(
                    e.message ?: "Erro ao salvar avaliação. Tente novamente."
                )
            }
        }
    }

    /**
     * Calcula as estatísticas finais da sessão e transiciona para
     * [SrsReviewUiState.SessionComplete].
     *
     * Pode ser chamado explicitamente pela UI (ex: botão "Encerrar sessão"),
     * mas é invocado automaticamente ao revisar o último card via [submitRating].
     *
     * Requisito 17.7: exibir tela de conclusão com total revisado e distribuição
     *                 de avaliações (again/hard/good/easy).
     */
    fun completeSession() {
        val totalReviewed = againCount + hardCount + goodCount + easyCount
        _uiState.value = SrsReviewUiState.SessionComplete(
            stats = ReviewSessionStats(
                totalReviewed = totalReviewed,
                againCount = againCount,
                hardCount = hardCount,
                goodCount = goodCount,
                easyCount = easyCount
            )
        )
    }

    /**
     * Reinicia o carregamento após uma falha.
     *
     * Requisito 12.7: opção de retry quando uiState transiciona para Error.
     */
    fun retry() {
        loadDueCards()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Carrega o conteúdo do card no índice [currentIndex] e transiciona
     * o estado para [SrsReviewUiState.ShowingCard] com [isRevealed] = false.
     *
     * O conteúdo varia por tipo:
     * - GRAMMAR: stimulus = answerText do exercício; answer = questionText da resposta primária.
     * - VOCABULARY: stimulus = word; answer = definition; detail = exampleSentence.
     *
     * Requisito 17.1: exibir apenas o estímulo (sem a resposta esperada) ao iniciar o card.
     */
    private suspend fun showCurrentCard() {
        val card = sessionCards.getOrNull(currentIndex) ?: return
        val content = loadCardContent(card) ?: return

        _uiState.value = SrsReviewUiState.ShowingCard(
            card = card,
            content = content,
            isRevealed = false,
            progress = ReviewProgress(
                currentIndex = currentIndex,
                totalCards = sessionCards.size
            )
        )
    }

    /**
     * Carrega o conteúdo rico ([ReviewCardContent]) para o [card] fornecido.
     *
     * Para cards GRAMMAR busca o exercício via [ContentRepository.getExerciseById];
     * para cards VOCABULARY busca o vocabulary card via [SrsRepository.getVocabularyCardById].
     *
     * Retorna null se o conteúdo não for encontrado (exercício ou vocab card removido).
     * Nesses casos o card é pulado silenciosamente — o ViewModel avança para o próximo.
     *
     * Requisito 17.1: estímulo sem resposta para active recall.
     * Requisito 17.2: resposta revelada ao pressionar "Revelar".
     */
    private suspend fun loadCardContent(card: SrsCard): ReviewCardContent? {
        return when (card.cardType) {
            SrsCardType.GRAMMAR -> loadGrammarContent(card.refId)
            SrsCardType.VOCABULARY -> loadVocabularyContent(card.refId)
        }
    }

    /**
     * Carrega o conteúdo de um card GRAMMAR a partir do exercício referenciado.
     *
     * Stimulus: [Exercise.answerText] (a resposta que o usuário deve ver).
     * Answer: texto da resposta aceita principal ([AcceptedAnswer.isPrimary] == true),
     *         ou a primeira resposta aceita caso não haja uma principal.
     */
    private suspend fun loadGrammarContent(exerciseId: Long): ReviewCardContent? {
        val exercise: Exercise = contentRepository.getExerciseById(exerciseId) ?: return null
        val primaryAnswer = exercise.acceptedAnswers
            .firstOrNull { it.isPrimary }
            ?: exercise.acceptedAnswers.firstOrNull()
        val answerText = primaryAnswer?.questionText ?: return null
        return ReviewCardContent(
            stimulus = exercise.answerText,
            answer = answerText
        )
    }

    /**
     * Carrega o conteúdo de um card VOCABULARY a partir do vocabulary card referenciado.
     *
     * Stimulus: [VocabularyCard.word] (a palavra em inglês).
     * Answer: [VocabularyCard.definition] (definição em PT-BR).
     * Detail: [VocabularyCard.exampleSentence] (sentença de exemplo em inglês).
     */
    private suspend fun loadVocabularyContent(vocabularyCardId: Long): ReviewCardContent? {
        val vocab: VocabularyCard = srsRepository.getVocabularyCardById(vocabularyCardId)
            ?: return null
        return ReviewCardContent(
            stimulus = vocab.word,
            answer = vocab.definition,
            detail = vocab.exampleSentence
        )
    }

    /**
     * Incrementa o contador do rating recebido para o cálculo das estatísticas finais.
     */
    private fun incrementRatingCount(rating: SrsRating) {
        when (rating) {
            SrsRating.AGAIN -> againCount++
            SrsRating.HARD  -> hardCount++
            SrsRating.GOOD  -> goodCount++
            SrsRating.EASY  -> easyCount++
        }
    }

    /** Zera os contadores de rating ao iniciar uma nova sessão. */
    private fun resetRatingCounts() {
        againCount = 0
        hardCount = 0
        goodCount = 0
        easyCount = 0
    }
}
