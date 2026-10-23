package com.englishlearning.app.ui.microlearning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.domain.model.ChallengeType
import com.englishlearning.app.domain.model.Exercise
import com.englishlearning.app.domain.model.MicrolearningConfig
import com.englishlearning.app.domain.model.MicrolearningProgress
import com.englishlearning.app.domain.model.ValidationResult
import com.englishlearning.app.domain.model.ValidationStatus
import com.englishlearning.app.domain.repository.DailyChallengeRepository
import com.englishlearning.app.domain.usecase.LoadExercisesUseCase
import com.englishlearning.app.domain.usecase.ValidateAnswerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────────────────────────────────────

sealed class MicrolearningUiState {
    object Loading : MicrolearningUiState()
    
    data class Active(
        val exercise: Exercise,
        val exerciseIndex: Int,
        val progress: MicrolearningProgress,
        val lastResult: ValidationResult? = null
    ) : MicrolearningUiState()
    
    data class Complete(
        val totalExercises: Int,
        val correctCount: Int,
        val accuracy: Float,
        val bestStreak: Int,
        val durationMs: Long,
        val completedInTime: Boolean
    ) : MicrolearningUiState()
    
    data class Error(val message: String) : MicrolearningUiState()
}

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

/**
 * ViewModel para sessões de Microlearning (2-5 minutos).
 * 
 * Sessões curtas e focadas para aprendizado rápido.
 */
@HiltViewModel
class MicrolearningViewModel @Inject constructor(
    private val loadExercisesUseCase: LoadExercisesUseCase,
    private val validateAnswerUseCase: ValidateAnswerUseCase,
    private val dailyChallengeRepository: DailyChallengeRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<MicrolearningUiState>(MicrolearningUiState.Loading)
    val uiState: StateFlow<MicrolearningUiState> = _uiState.asStateFlow()
    
    private var exercises: List<Exercise> = emptyList()
    private var currentIndex = 0
    private var correctCount = 0
    private var currentStreak = 0
    private var bestStreak = 0
    private var config = MicrolearningConfig()
    private var startTime = 0L
    private var targetDurationMs = 0L
    
    /**
     * Inicia uma sessão de microlearning.
     */
    fun startSession(
        config: MicrolearningConfig = MicrolearningConfig()
    ) {
        this.config = config
        
        viewModelScope.launch {
            _uiState.value = MicrolearningUiState.Loading
            
            // Carrega exercícios de níveis variados (interleaving)
            val allExercises = mutableListOf<Exercise>()
            
            // Carrega de múltiplos níveis para interleaving
            if (config.mixCategories) {
                for (levelId in 1..3) {
                    loadExercisesUseCase(levelId, 0).onSuccess { list ->
                        allExercises.addAll(list.take(config.exerciseCount / 2))
                    }
                }
            } else {
                loadExercisesUseCase(1, 0).onSuccess { list ->
                    allExercises.addAll(list)
                }
            }
            
            if (allExercises.isEmpty()) {
                _uiState.value = MicrolearningUiState.Error("Nenhum exercício disponível")
                return@launch
            }
            
            // Embaralha para interleaving
            exercises = allExercises.shuffled().take(config.exerciseCount)
            
            currentIndex = 0
            correctCount = 0
            currentStreak = 0
            bestStreak = 0
            startTime = System.currentTimeMillis()
            targetDurationMs = config.durationMinutes * 60 * 1000L
            
            showCurrentExercise()
        }
    }
    
    /**
     * Atualiza o timer (chamado a cada segundo).
     */
    fun updateTimer() {
        val state = _uiState.value as? MicrolearningUiState.Active ?: return
        
        val newProgress = state.progress.copy()
        
        // Verifica se o tempo acabou
        if (newProgress.isTimeUp) {
            finishSession(completedInTime = true)
            return
        }
        
        // Força re-composição para atualizar o timer
        _uiState.value = state.copy(progress = newProgress)
    }
    
    /**
     * Submete uma resposta.
     */
    fun submitAnswer(userAnswer: String) {
        val state = _uiState.value as? MicrolearningUiState.Active ?: return
        val exercise = exercises.getOrNull(currentIndex) ?: return
        
        viewModelScope.launch {
            val result = validateAnswerUseCase(userAnswer, exercise)
            
            if (result.status == ValidationStatus.CORRECT) {
                correctCount++
                currentStreak++
                if (currentStreak > bestStreak) bestStreak = currentStreak
            } else {
                currentStreak = 0
            }
            
            // Atualiza desafio diário
            dailyChallengeRepository.incrementProgressForType(
                userId = 1L,
                challengeType = ChallengeType.EXERCISE_COUNT
            )
            
            if (currentStreak > 0) {
                dailyChallengeRepository.incrementProgressForType(
                    userId = 1L,
                    challengeType = ChallengeType.STREAK_COUNT
                )
            }
            
            _uiState.value = state.copy(
                lastResult = result,
                progress = state.progress.copy(
                    correctCount = correctCount,
                    currentStreak = currentStreak
                )
            )
        }
    }
    
    /**
     * Avança para o próximo exercício.
     */
    fun moveToNext() {
        val state = _uiState.value as? MicrolearningUiState.Active ?: return
        
        // Verifica se ainda tem tempo e exercícios
        if (state.progress.isTimeUp) {
            finishSession(completedInTime = true)
            return
        }
        
        if (currentIndex < exercises.size - 1) {
            currentIndex++
            showCurrentExercise()
        } else {
            finishSession(completedInTime = false)
        }
    }
    
    private fun showCurrentExercise() {
        val exercise = exercises.getOrNull(currentIndex) ?: return
        
        val progress = MicrolearningProgress(
            startTime = startTime,
            targetDurationMs = targetDurationMs,
            exercisesCompleted = currentIndex,
            targetExercises = exercises.size,
            correctCount = correctCount,
            currentStreak = currentStreak
        )
        
        _uiState.value = MicrolearningUiState.Active(
            exercise = exercise,
            exerciseIndex = currentIndex,
            progress = progress
        )
    }
    
    private fun finishSession(completedInTime: Boolean) {
        val durationMs = System.currentTimeMillis() - startTime
        val totalExercises = currentIndex + 1
        
        _uiState.value = MicrolearningUiState.Complete(
            totalExercises = totalExercises,
            correctCount = correctCount,
            accuracy = if (totalExercises > 0) correctCount.toFloat() / totalExercises else 0f,
            bestStreak = bestStreak,
            durationMs = durationMs,
            completedInTime = completedInTime
        )
        
        // Atualiza desafio de tempo
        viewModelScope.launch {
            val minutesStudied = (durationMs / 60000).toInt()
            for (i in 0 until minutesStudied) {
                dailyChallengeRepository.incrementProgressForType(
                    userId = 1L,
                    challengeType = ChallengeType.TIME_BASED
                )
            }
        }
    }
}
