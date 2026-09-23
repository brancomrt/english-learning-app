package com.englishlearning.app.ui.vocabulary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.data.database.dao.VocabularyDao
import com.englishlearning.app.data.database.entity.VocabularyCardEntity
import com.englishlearning.app.domain.gamification.SrsRating
import com.englishlearning.app.domain.model.Category
import com.englishlearning.app.domain.model.Level
import com.englishlearning.app.domain.model.SrsCardType
import com.englishlearning.app.domain.repository.ContentRepository
import com.englishlearning.app.domain.repository.SrsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// ─── UI State ────────────────────────────────────────────────────────────────

sealed class VocabularyUiState {
    object Loading : VocabularyUiState()

    /** Seleção de nível e categoria antes de iniciar. */
    data class Selecting(
        val levels: List<Level> = emptyList(),
        val categories: List<Category> = emptyList(),
        val filteredCategories: List<Category> = emptyList(),
        val selectedLevelId: Int = -1,
        val selectedCategoryId: Int = -1,
        val dueCount: Int = 0,       // cards devidos hoje
        val newCount: Int = 0        // cards novos disponíveis
    ) : VocabularyUiState()

    /** Revisão ativa — um card por vez. */
    data class Reviewing(
        val card: VocabularyCardEntity,
        val isRevealed: Boolean = false,
        val remaining: Int = 0,       // cards restantes na sessão
        val reviewed: Int = 0,        // cards já avaliados
        val total: Int = 0
    ) : VocabularyUiState()

    /** Sessão concluída. */
    data class Done(
        val reviewed: Int,
        val newCards: Int,
        val reviewedCards: Int
    ) : VocabularyUiState()
}

// ─── ViewModel ───────────────────────────────────────────────────────────────

@HiltViewModel
class VocabularyViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val srsRepository: SrsRepository,
    private val vocabularyDao: VocabularyDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<VocabularyUiState>(VocabularyUiState.Loading)
    val uiState: StateFlow<VocabularyUiState> = _uiState.asStateFlow()

    private val userId = 1L
    private val today = LocalDate.now()

    // Fila de cards da sessão atual
    private val sessionQueue = ArrayDeque<VocabularyCardEntity>()
    private var sessionTotal = 0
    private var sessionReviewed = 0
    private var sessionNewCount = 0
    private var sessionReviewCount = 0

    init {
        loadSelectionScreen()
    }

    // ── Seleção ────────────────────────────────────────────────────────────────

    private fun loadSelectionScreen(
        levelId: Int = -1,
        categoryId: Int = -1
    ) {
        viewModelScope.launch {
            val levels = contentRepository.getLevels()
            val allCategories = contentRepository.getCategories()
            val effectiveLevelId = if (levelId == -1) levels.firstOrNull()?.id ?: -1 else levelId
            val filtered = allCategories.filter { it.levelId == effectiveLevelId }
            val effectiveCatId = if (categoryId == -1) filtered.firstOrNull()?.id ?: -1 else categoryId

            val (due, new) = countCards(effectiveLevelId, effectiveCatId)

            _uiState.value = VocabularyUiState.Selecting(
                levels = levels,
                categories = allCategories,
                filteredCategories = filtered,
                selectedLevelId = effectiveLevelId,
                selectedCategoryId = effectiveCatId,
                dueCount = due,
                newCount = new
            )
        }
    }

    fun selectLevel(levelId: Int) {
        val s = _uiState.value as? VocabularyUiState.Selecting ?: return
        val filtered = s.categories.filter { it.levelId == levelId }
        val catId = filtered.firstOrNull()?.id ?: -1
        loadSelectionScreen(levelId, catId)
    }

    fun selectCategory(categoryId: Int) {
        val s = _uiState.value as? VocabularyUiState.Selecting ?: return
        loadSelectionScreen(s.selectedLevelId, categoryId)
    }

    /** Conta cards devidos e novos para o nível/categoria selecionados. */
    private suspend fun countCards(levelId: Int, categoryId: Int): Pair<Int, Int> {
        if (levelId == -1 || categoryId == -1) return Pair(0, 0)
        val allCards = vocabularyDao.getByLevelAndCategory(levelId, categoryId)
        val srsCards = srsRepository.getCardsForReview(userId, today)
            .filter { it.cardType == SrsCardType.VOCABULARY }
            .map { it.refId }
            .toSet()
        val dueIds = allCards.filter { it.id in srsCards }.size
        val newIds = allCards.filter { it.id !in getAllSrsRefIds() }.size
        return Pair(dueIds, newIds)
    }

    private suspend fun getAllSrsRefIds(): Set<Long> {
        return srsRepository.getCardsForReview(userId, today.plusDays(365 * 10))
            .filter { it.cardType == SrsCardType.VOCABULARY }
            .map { it.refId }
            .toSet()
    }

    // ── Início da sessão ───────────────────────────────────────────────────────

    /** Inicia sessão com cards devidos + novos da categoria selecionada. */
    fun startSession() {
        val s = _uiState.value as? VocabularyUiState.Selecting ?: return
        if (s.selectedLevelId == -1 || s.selectedCategoryId == -1) return
        viewModelScope.launch {
            _uiState.value = VocabularyUiState.Loading
            val allCards = vocabularyDao.getByLevelAndCategory(s.selectedLevelId, s.selectedCategoryId)
            val allSrsIds = getAllSrsRefIds()
            val dueIds = srsRepository.getCardsForReview(userId, today)
                .filter { it.cardType == SrsCardType.VOCABULARY }
                .map { it.refId }
                .toSet()

            // Ordem: cards devidos primeiro, depois novos (máx 20 por sessão)
            val due = allCards.filter { it.id in dueIds }
            val new = allCards.filter { it.id !in allSrsIds }.take((20 - due.size).coerceAtLeast(0))

            sessionQueue.clear()
            sessionQueue.addAll(due.shuffled() + new)
            sessionTotal = sessionQueue.size
            sessionReviewed = 0
            sessionNewCount = new.size
            sessionReviewCount = due.size

            if (sessionQueue.isEmpty()) {
                _uiState.value = VocabularyUiState.Done(0, 0, 0)
            } else {
                showNextCard()
            }
        }
    }

    // ── Revisão ────────────────────────────────────────────────────────────────

    /** Revela o verso do card (palavra + definição + áudio). */
    fun revealCard() {
        val s = _uiState.value as? VocabularyUiState.Reviewing ?: return
        _uiState.value = s.copy(isRevealed = true)
    }

    /**
     * Avalia o card atual e avança para o próximo.
     * Persiste o agendamento FSRS e move para o próximo card.
     */
    fun rateCard(rating: SrsRating) {
        val s = _uiState.value as? VocabularyUiState.Reviewing ?: return
        viewModelScope.launch {
            scheduleCard(s.card, rating)
            sessionReviewed++
            showNextCard()
        }
    }

    private fun showNextCard() {
        if (sessionQueue.isEmpty()) {
            _uiState.value = VocabularyUiState.Done(
                reviewed = sessionReviewed,
                newCards = sessionNewCount,
                reviewedCards = sessionReviewCount
            )
        } else {
            val next = sessionQueue.removeFirst()
            _uiState.value = VocabularyUiState.Reviewing(
                card = next,
                isRevealed = false,
                remaining = sessionQueue.size,
                reviewed = sessionReviewed,
                total = sessionTotal
            )
        }
    }

    private suspend fun scheduleCard(card: VocabularyCardEntity, rating: SrsRating) {
        val existing = srsRepository.getOrCreateCard(
            userId = userId,
            cardType = SrsCardType.VOCABULARY,
            refId = card.id
        )
        // Usa updateCardAfterReview que já encapsula o scheduling FSRS
        srsRepository.updateCardAfterReview(existing, rating)
    }

    /** Volta para a tela de seleção. */
    fun backToSelection() {
        val s = _uiState.value
        val levelId = if (s is VocabularyUiState.Done) -1 else -1
        loadSelectionScreen()
    }
}
