package com.englishlearning.app.ui.flashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.domain.model.Flashcard
import com.englishlearning.app.domain.model.FlashcardRating
import com.englishlearning.app.domain.repository.FlashcardRepository
import com.englishlearning.app.domain.repository.TtsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel para a tela de Flashcards.
 */
@HiltViewModel
class FlashcardViewModel @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
    private val ttsRepository: TtsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FlashcardUiState())
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()
    
    private var cards = listOf<Flashcard>()
    private var currentIndex = 0
    
    init {
        // Não carrega automaticamente - espera seleção de nível
        _uiState.update { it.copy(isLoading = false, isSelectingLevel = true) }
    }
    
    fun selectLevel(level: Int) {
        _uiState.update { it.copy(selectedLevel = level, isSelectingLevel = false) }
        loadCards(level)
    }
    
    private fun loadCards(levelId: Int = _uiState.value.selectedLevel) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val today = LocalDate.now().toString()
                val userId = 1L
                
                // Carrega cards devidos + novos filtrados por nível
                val dueCards = flashcardRepository.getDueCards(userId, today, 30)
                    .filter { it.levelId == levelId }
                val newCards = flashcardRepository.getNewCards(userId, 10)
                    .filter { it.levelId == levelId }
                
                cards = (dueCards + newCards).shuffled()
                
                val dueCount = dueCards.size
                val newCount = newCards.size
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentCard = cards.firstOrNull(),
                        totalCards = cards.size,
                        dueToday = dueCount,
                        newCards = newCount
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isLoading = false, error = e.message) 
                }
            }
        }
    }
    
    fun flipCard() {
        _uiState.update { it.copy(isShowingAnswer = !it.isShowingAnswer) }
    }
    
    fun rateCard(rating: FlashcardRating) {
        val card = _uiState.value.currentCard ?: return
        
        viewModelScope.launch {
            try {
                // Atualiza SRS baseado no rating
                flashcardRepository.reviewCard(card.id, rating)
                
                // Avança para próximo card
                currentIndex++
                val isCorrect = rating in listOf(FlashcardRating.GOOD, FlashcardRating.EASY)
                
                _uiState.update {
                    it.copy(
                        currentCard = cards.getOrNull(currentIndex),
                        isShowingAnswer = false,
                        reviewedCount = it.reviewedCount + 1,
                        correctCount = if (isCorrect) it.correctCount + 1 else it.correctCount,
                        progress = if (cards.isNotEmpty()) (currentIndex.toFloat() / cards.size) else 0f,
                        isComplete = currentIndex >= cards.size
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    fun playAudio() {
        val word = _uiState.value.currentCard?.word ?: return
        viewModelScope.launch {
            // Usa áudio nativo se disponível, senão TTS
            val usedNative = ttsRepository.speakWord(word, preferNative = true)
            _uiState.update { it.copy(usedNativeAudio = usedNative) }
        }
    }
}

/**
 * Estado da UI de Flashcards.
 */
data class FlashcardUiState(
    val isLoading: Boolean = true,
    val currentCard: Flashcard? = null,
    val isShowingAnswer: Boolean = false,
    val totalCards: Int = 0,
    val reviewedCount: Int = 0,
    val correctCount: Int = 0,
    val dueToday: Int = 0,
    val newCards: Int = 0,
    val progress: Float = 0f,
    val isComplete: Boolean = false,
    val error: String? = null,
    // Filtro por nível
    val availableLevels: List<Int> = (1..12).toList(),
    val selectedLevel: Int = 1,
    val isSelectingLevel: Boolean = true,
    // Áudio
    val usedNativeAudio: Boolean = false
)
