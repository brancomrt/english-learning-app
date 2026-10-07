package com.englishlearning.app.ui.callan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.data.database.entity.VocabularyCardEntity
import com.englishlearning.app.data.repository.SessionSettingsRepository
import com.englishlearning.app.domain.model.Exercise
import com.englishlearning.app.domain.model.ExerciseMode
import com.englishlearning.app.domain.repository.ContentRepository
import com.englishlearning.app.domain.usecase.ValidateAnswerUseCase
import com.englishlearning.app.domain.validation.TextNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DrillUiState {
    object Loading : DrillUiState()
    data class Question(
        val exercise: Exercise,
        val index: Int,
        val total: Int,
        val score: Int,
        val secondsLeft: Int = 15,
        val showResult: Boolean = false,
        val wasCorrect: Boolean = false,
        val correctAnswer: String = ""
    ) : DrillUiState()
    data class Done(
        val score: Int,
        val total: Int,
        val percentage: Int
    ) : DrillUiState()
}

@HiltViewModel
class CallanDrillViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val validateAnswerUseCase: ValidateAnswerUseCase,
    private val settingsRepository: SessionSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DrillUiState>(DrillUiState.Loading)
    val uiState: StateFlow<DrillUiState> = _uiState.asStateFlow()

    private val normalizer = TextNormalizer()
    private var exercises: List<Exercise> = emptyList()
    private var currentIndex = 0
    private var score = 0

    init { loadDrill() }

    private fun loadDrill() {
        viewModelScope.launch {
            _uiState.value = DrillUiState.Loading
            val levels = contentRepository.getLevels().filter { it.isUnlocked }
            val allExercises = mutableListOf<Exercise>()
            for (level in levels) {
                val cats = contentRepository.getCategories().filter { it.levelId == level.id }
                for (cat in cats) {
                    val exs = contentRepository.getExercisesByLevelAndCategory(level.id, cat.id)
                    allExercises.addAll(exs)
                }
            }
            // Filtra só ANSWER_FROM_QUESTION (estilo Callan: pergunta → resposta)
            exercises = allExercises
                .filter { it.exerciseMode == ExerciseMode.ANSWER_FROM_QUESTION }
                .shuffled()
                .take(DRILL_SIZE)

            if (exercises.isEmpty()) {
                _uiState.value = DrillUiState.Done(0, 0, 0)
            } else {
                currentIndex = 0
                score = 0
                showQuestion()
                startTimer()
            }
        }
    }

    private fun showQuestion() {
        val ex = exercises.getOrNull(currentIndex) ?: return
        _uiState.value = DrillUiState.Question(
            exercise = ex,
            index = currentIndex + 1,
            total = exercises.size,
            score = score,
            secondsLeft = TIMER_SECONDS,
            showResult = false
        )
        startTimer()
    }

    private fun startTimer() {
        viewModelScope.launch {
            repeat(TIMER_SECONDS) { elapsed ->
                delay(1000)
                val s = _uiState.value
                if (s is DrillUiState.Question && !s.showResult) {
                    val left = TIMER_SECONDS - elapsed - 1
                    _uiState.value = s.copy(secondsLeft = left)
                    if (left == 0) {
                        // Tempo esgotado — mostra resposta
                        val correct = s.exercise.acceptedAnswers
                            .firstOrNull { it.isPrimary }?.questionText
                            ?: s.exercise.acceptedAnswers.firstOrNull()?.questionText ?: ""
                        _uiState.value = s.copy(showResult = true, wasCorrect = false, correctAnswer = correct)
                    }
                }
            }
        }
    }

    fun submitAnswer(input: String) {
        val s = _uiState.value as? DrillUiState.Question ?: return
        if (s.showResult) return

        val result = validateAnswerUseCase(input, s.exercise)
        val isCorrect = result.status.name == "CORRECT" || result.status.name == "TOO_CLOSE"
        if (isCorrect) score++

        val correct = s.exercise.acceptedAnswers
            .firstOrNull { it.isPrimary }?.questionText
            ?: s.exercise.acceptedAnswers.firstOrNull()?.questionText ?: ""

        _uiState.value = s.copy(
            showResult = true,
            wasCorrect = isCorrect,
            correctAnswer = correct,
            score = score
        )
    }

    fun next() {
        currentIndex++
        if (currentIndex >= exercises.size) {
            val pct = if (exercises.isNotEmpty()) (score * 100) / exercises.size else 0
            _uiState.value = DrillUiState.Done(score, exercises.size, pct)
        } else {
            showQuestion()
        }
    }

    fun restart() {
        currentIndex = 0
        score = 0
        exercises = exercises.shuffled()
        showQuestion()
    }

    companion object {
        const val TIMER_SECONDS = 15
        const val DRILL_SIZE = 20
    }
}
