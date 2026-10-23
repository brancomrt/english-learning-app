package com.englishlearning.app.ui.listeningfirst

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.domain.model.ChallengeType
import com.englishlearning.app.domain.model.Exercise
import com.englishlearning.app.domain.model.ListeningFirstState
import com.englishlearning.app.domain.model.ValidationResult
import com.englishlearning.app.domain.model.ValidationStatus
import com.englishlearning.app.domain.repository.DailyChallengeRepository
import com.englishlearning.app.domain.usecase.LoadExercisesUseCase
import com.englishlearning.app.domain.usecase.UpdateProgressUseCase
import com.englishlearning.app.domain.usecase.ValidateAnswerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────────────────────────────────────

sealed class ListeningFirstUiState {
    object Loading : ListeningFirstUiState()
    
    data class Active(
        val exercise: Exercise,
        val currentIndex: Int,
        val totalExercises: Int,
        val listeningState: ListeningFirstState,
        val lastResult: ValidationResult? = null,
        val sessionXp: Int = 0,
        val correctCount: Int = 0
    ) : ListeningFirstUiState()
    
    data class Complete(
        val totalExercises: Int,
        val correctCount: Int,
        val totalXp: Int,
        val averagePlayCount: Float
    ) : ListeningFirstUiState()
    
    data class Error(val message: String) : ListeningFirstUiState()
}

sealed class ListeningFirstEvent {
    object PlayAudio : ListeningFirstEvent()
    data class ShowFeedback(val isCorrect: Boolean) : ListeningFirstEvent()
}

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

/**
 * ViewModel para o modo Listening First.
 * 
 * Neste modo, o usuário ouve primeiro (sem ver o texto) e tenta responder.
 * O texto só é revelado após N tentativas ou quando o usuário acerta.
 */
@HiltViewModel
class ListeningFirstViewModel @Inject constructor(
    private val loadExercisesUseCase: LoadExercisesUseCase,
    private val validateAnswerUseCase: ValidateAnswerUseCase,
    private val updateProgressUseCase: UpdateProgressUseCase,
    private val dailyChallengeRepository: DailyChallengeRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ListeningFirstUiState>(ListeningFirstUiState.Loading)
    val uiState: StateFlow<ListeningFirstUiState> = _uiState.asStateFlow()
    
    private val _events = Channel<ListeningFirstEvent>(Channel.BUFFERED)
    val events: Flow<ListeningFirstEvent> = _events.receiveAsFlow()
    
    private var exercises: List<Exercise> = emptyList()
    private var currentIndex = 0
    private var sessionXp = 0
    private var correctCount = 0
    private var totalPlayCount = 0
    private var levelId = 0
    private var categoryId = 0
    
    /**
     * Inicia uma sessão de Listening First.
     */
    fun startSession(levelId: Int, categoryId: Int, exerciseLimit: Int = 10) {
        this.levelId = levelId
        this.categoryId = categoryId
        
        viewModelScope.launch {
            _uiState.value = ListeningFirstUiState.Loading
            
            loadExercisesUseCase(levelId, categoryId).fold(
                onSuccess = { list ->
                    exercises = list.take(exerciseLimit)
                    currentIndex = 0
                    sessionXp = 0
                    correctCount = 0
                    totalPlayCount = 0
                    showCurrentExercise()
                },
                onFailure = { e ->
                    _uiState.value = ListeningFirstUiState.Error(
                        e.message ?: "Erro ao carregar exercícios"
                    )
                }
            )
        }
    }
    
    /**
     * Registra que o áudio foi reproduzido.
     */
    fun onAudioPlayed() {
        val state = _uiState.value as? ListeningFirstUiState.Active ?: return
        totalPlayCount++
        
        val newListeningState = state.listeningState.copy(
            audioPlayed = true,
            playCount = state.listeningState.playCount + 1
        )
        
        _uiState.value = state.copy(listeningState = newListeningState)
    }
    
    /**
     * Revela o texto (após atingir o limite de plays ou por escolha do usuário).
     */
    fun revealText() {
        val state = _uiState.value as? ListeningFirstUiState.Active ?: return
        
        _uiState.value = state.copy(
            listeningState = state.listeningState.copy(textRevealed = true)
        )
    }
    
    /**
     * Submete a resposta do usuário.
     */
    fun submitAnswer(userAnswer: String) {
        val state = _uiState.value as? ListeningFirstUiState.Active ?: return
        val exercise = exercises.getOrNull(currentIndex) ?: return
        
        viewModelScope.launch {
            // Marca como tentativa
            val newListeningState = state.listeningState.copy(userAttempted = true)
            
            // Valida a resposta
            val result = validateAnswerUseCase(userAnswer, exercise)
            
            // Calcula XP (bonus por acertar sem ver o texto)
            val baseXp = exercise.xpReward
            val bonusMultiplier = if (!state.listeningState.textRevealed) 1.5f else 1.0f
            val xpEarned = if (result.status == ValidationStatus.CORRECT) {
                (baseXp * bonusMultiplier).toInt()
            } else 0
            
            sessionXp += xpEarned
            if (result.status == ValidationStatus.CORRECT) correctCount++
            
            // Atualiza progresso do desafio diário
            dailyChallengeRepository.incrementProgressForType(
                userId = 1L,
                challengeType = ChallengeType.LISTENING_SESSION
            )
            
            _events.send(ListeningFirstEvent.ShowFeedback(result.status == ValidationStatus.CORRECT))
            
            _uiState.value = state.copy(
                listeningState = newListeningState.copy(textRevealed = true),
                lastResult = result,
                sessionXp = sessionXp,
                correctCount = correctCount
            )
        }
    }
    
    /**
     * Avança para o próximo exercício.
     */
    fun moveToNext() {
        if (currentIndex < exercises.size - 1) {
            currentIndex++
            showCurrentExercise()
        } else {
            // Sessão completa
            val avgPlays = if (exercises.isNotEmpty()) {
                totalPlayCount.toFloat() / exercises.size
            } else 0f
            
            _uiState.value = ListeningFirstUiState.Complete(
                totalExercises = exercises.size,
                correctCount = correctCount,
                totalXp = sessionXp,
                averagePlayCount = avgPlays
            )
        }
    }
    
    private fun showCurrentExercise() {
        val exercise = exercises.getOrNull(currentIndex) ?: return
        
        _uiState.value = ListeningFirstUiState.Active(
            exercise = exercise,
            currentIndex = currentIndex,
            totalExercises = exercises.size,
            listeningState = ListeningFirstState(),
            sessionXp = sessionXp,
            correctCount = correctCount
        )
        
        // Auto-play audio ao mostrar exercício
        viewModelScope.launch {
            _events.send(ListeningFirstEvent.PlayAudio)
        }
    }
}
