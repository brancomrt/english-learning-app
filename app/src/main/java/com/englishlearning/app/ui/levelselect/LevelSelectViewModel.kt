package com.englishlearning.app.ui.levelselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.domain.model.Level
import com.englishlearning.app.domain.repository.ContentRepository
import com.englishlearning.app.domain.repository.ProgressRepository
import com.englishlearning.app.domain.usecase.GetAvailableLevelsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Nível com dados de progresso calculados para exibição na LevelSelectScreen.
 *
 * Requisito 6.3: exibir níveis desbloqueados, em progresso e bloqueados.
 * Requisito 6.5: exibir taxa de progresso (exercícios completados / total) por nível.
 */
data class LevelWithProgress(
    val level: Level,
    /** Progresso do nível no intervalo [0.0, 1.0]. */
    val progressPercent: Float,
    val completedExercises: Int,
    val totalExercises: Int
)

/**
 * Estado da LevelSelectScreen.
 *
 * Requisito 6.3: representa os estados de carregamento, lista de níveis e erro.
 */
data class LevelSelectUiState(
    val isLoading: Boolean = true,
    val levels: List<LevelWithProgress> = emptyList(),
    val error: String? = null
)

/**
 * ViewModel da tela de seleção de nível (LevelSelectScreen).
 *
 * Responsabilidades:
 * - Carregar a lista de níveis disponíveis via [GetAvailableLevelsUseCase] (Req. 6.1, 6.3).
 * - Para cada nível, calcular o total de exercícios e o total de exercícios concluídos
 *   a fim de gerar [LevelWithProgress.progressPercent] (Req. 6.5).
 * - Expor o estado como [StateFlow] de [LevelSelectUiState].
 *
 * @param getAvailableLevelsUseCase Caso de uso que retorna todos os níveis disponíveis.
 * @param contentRepository         Repositório para buscar exercícios por nível.
 * @param progressRepository        Repositório para buscar progresso do usuário por nível.
 */
@HiltViewModel
class LevelSelectViewModel @Inject constructor(
    private val getAvailableLevelsUseCase: GetAvailableLevelsUseCase,
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LevelSelectUiState())
    val uiState: StateFlow<LevelSelectUiState> = _uiState.asStateFlow()

    init {
        loadLevels()
    }

    /**
     * Carrega os níveis e calcula o progresso para cada um.
     *
     * Para cada nível:
     * 1. Busca todos os exercícios do nível para obter [totalExercises].
     * 2. Busca o progresso por nível/categoria e soma os exercícios concluídos.
     * 3. Calcula [progressPercent] = completedExercises / totalExercises.
     *
     * Requisito 6.3: exibir status de desbloqueio de cada nível.
     * Requisito 6.5: exibir taxa de progresso por nível.
     */
    fun loadLevels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val levels = getAvailableLevelsUseCase()
                levels.map { level ->
                    async { buildLevelWithProgress(level) }
                }.awaitAll()
            }.onSuccess { levelsWithProgress ->
                _uiState.update {
                    it.copy(isLoading = false, levels = levelsWithProgress)
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "Erro ao carregar os níveis"
                    )
                }
            }
        }
    }

    /**
     * Constrói um [LevelWithProgress] para um único nível, agregando
     * o progresso de todas as categorias do nível.
     *
     * Req. 6.5: progressPercent = exercícios completados / total de exercícios do nível.
     * Quando não há exercícios no nível, progressPercent retorna 0.0.
     */
    private suspend fun buildLevelWithProgress(level: Level): LevelWithProgress {
        // Total de exercícios disponíveis no nível
        val exercises = contentRepository.getExercisesByLevel(level.id)
        val totalExercises = exercises.size

        // Exercícios concluídos: agrega progresso de cada categoria do nível
        val completedExercises = if (totalExercises > 0) {
            val categoryIds = exercises.map { it.categoryId }.distinct()
            categoryIds.sumOf { categoryId ->
                progressRepository
                    .getLevelProgress(DEFAULT_USER_ID, level.id, categoryId)
                    .exercisesCompleted
            }
        } else {
            0
        }

        val progressPercent = if (totalExercises > 0) {
            (completedExercises.toFloat() / totalExercises).coerceIn(0f, 1f)
        } else {
            0f
        }

        return LevelWithProgress(
            level = level,
            progressPercent = progressPercent,
            completedExercises = completedExercises,
            totalExercises = totalExercises
        )
    }

    companion object {
        private const val DEFAULT_USER_ID = 1L
    }
}
