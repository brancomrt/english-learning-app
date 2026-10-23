package com.englishlearning.app.ui.errorreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.data.database.dao.ProgressDao
import com.englishlearning.app.data.database.entity.AttemptEntity
import com.englishlearning.app.domain.model.Exercise
import com.englishlearning.app.domain.repository.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Informações sobre um exercício com erro para revisão.
 *
 * @property exercise O exercício que o usuário errou.
 * @property lastAttempt A última tentativa incorreta do usuário.
 */
data class ErrorReviewItem(
    val exercise: Exercise,
    val lastAttempt: AttemptEntity?
)

/**
 * Estado da tela de revisão de erros.
 */
sealed class ErrorReviewUiState {
    /** Carregando exercícios com erros. */
    object Loading : ErrorReviewUiState()

    /** Nenhum exercício com erro pendente. */
    object Empty : ErrorReviewUiState()

    /** Exercício atual sendo revisado. */
    data class Reviewing(
        val currentItem: ErrorReviewItem,
        val currentIndex: Int,
        val totalCount: Int
    ) : ErrorReviewUiState()

    /** Sessão de revisão concluída. */
    data class SessionComplete(
        val totalReviewed: Int,
        val correctCount: Int,
        val incorrectCount: Int
    ) : ErrorReviewUiState()

    /** Erro ao carregar os dados. */
    data class Error(val message: String) : ErrorReviewUiState()
}

/** ID do único usuário do app (modo single-user). */
private const val DEFAULT_USER_ID = 1L

/**
 * ViewModel para a tela de revisão de erros.
 *
 * Carrega exercícios que o usuário errou e ainda não acertou,
 * permitindo que ele pratique novamente esses exercícios específicos.
 */
@HiltViewModel
class ErrorReviewViewModel @Inject constructor(
    private val progressDao: ProgressDao,
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ErrorReviewUiState>(ErrorReviewUiState.Loading)
    val uiState: StateFlow<ErrorReviewUiState> = _uiState.asStateFlow()

    private var errorItems: List<ErrorReviewItem> = emptyList()
    private var currentIndex: Int = 0
    private var correctCount: Int = 0
    private var incorrectCount: Int = 0

    /**
     * Carrega os exercícios com erros pendentes para revisão.
     */
    fun loadErrorExercises() {
        viewModelScope.launch {
            _uiState.value = ErrorReviewUiState.Loading
            try {
                val incorrectExerciseIds = progressDao.getIncorrectExerciseIds(DEFAULT_USER_ID)

                if (incorrectExerciseIds.isEmpty()) {
                    _uiState.value = ErrorReviewUiState.Empty
                    return@launch
                }

                // Carrega os exercícios completos e as últimas tentativas incorretas
                val items = incorrectExerciseIds.mapNotNull { exerciseId ->
                    val exercise = contentRepository.getExerciseById(exerciseId)
                    if (exercise != null) {
                        val lastAttempt = progressDao.getLastIncorrectAttempt(DEFAULT_USER_ID, exerciseId)
                        ErrorReviewItem(exercise, lastAttempt)
                    } else {
                        null
                    }
                }

                if (items.isEmpty()) {
                    _uiState.value = ErrorReviewUiState.Empty
                    return@launch
                }

                errorItems = items
                currentIndex = 0
                correctCount = 0
                incorrectCount = 0
                showCurrentExercise()

            } catch (e: Exception) {
                _uiState.value = ErrorReviewUiState.Error(
                    e.message ?: "Erro ao carregar exercícios"
                )
            }
        }
    }

    /**
     * Registra o resultado da tentativa atual e avança para o próximo exercício.
     *
     * @param wasCorrect true se o usuário acertou o exercício.
     */
    fun submitResult(wasCorrect: Boolean) {
        if (wasCorrect) {
            correctCount++
        } else {
            incorrectCount++
        }

        currentIndex++

        if (currentIndex >= errorItems.size) {
            // Sessão concluída
            _uiState.value = ErrorReviewUiState.SessionComplete(
                totalReviewed = errorItems.size,
                correctCount = correctCount,
                incorrectCount = incorrectCount
            )
        } else {
            showCurrentExercise()
        }
    }

    /**
     * Pula o exercício atual sem registrar resultado.
     */
    fun skipExercise() {
        currentIndex++

        if (currentIndex >= errorItems.size) {
            _uiState.value = ErrorReviewUiState.SessionComplete(
                totalReviewed = errorItems.size,
                correctCount = correctCount,
                incorrectCount = incorrectCount
            )
        } else {
            showCurrentExercise()
        }
    }

    /**
     * Reinicia o carregamento após um erro.
     */
    fun retry() {
        loadErrorExercises()
    }

    private fun showCurrentExercise() {
        if (currentIndex < errorItems.size) {
            _uiState.value = ErrorReviewUiState.Reviewing(
                currentItem = errorItems[currentIndex],
                currentIndex = currentIndex,
                totalCount = errorItems.size
            )
        }
    }
}
