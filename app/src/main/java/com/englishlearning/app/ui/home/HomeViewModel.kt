package com.englishlearning.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.data.database.dao.ProgressDao
import com.englishlearning.app.domain.model.Level
import com.englishlearning.app.domain.model.UserStats
import com.englishlearning.app.domain.repository.SrsRepository
import com.englishlearning.app.domain.usecase.CheckDailyStreakUseCase
import com.englishlearning.app.domain.usecase.GetAvailableLevelsUseCase
import com.englishlearning.app.domain.usecase.GetUserStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado da HomeScreen.
 *
 * Requisito 5.6: streak exibido prominentemente na HomeScreen.
 * Requisito 6.3: exibir níveis desbloqueados, em progresso e bloqueados.
 * Requisito 12.1: navegar para HomeScreen após splash/onboarding.
 * Requisito 16.6: badge com contagem de SRS cards devidos na HomeScreen.
 *
 * @property isLoading        Indica carregamento inicial dos dados.
 * @property userStats        Estatísticas gerais do usuário (XP, streak, nível atual).
 * @property currentStreak    Streak diário atual do usuário (Req. 5.6).
 * @property unlockedLevels   Lista de níveis com [Level.isUnlocked] == true (Req. 6.3).
 * @property dueCardCount     Contagem de SRS cards com dueDate <= hoje (Req. 16.6, 18.4).
 * @property errorExerciseCount Contagem de exercícios com erros pendentes para revisão.
 * @property error            Mensagem de erro quando o carregamento falha (Req. 12.7).
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val userStats: UserStats? = null,
    val currentStreak: Int = 0,
    val unlockedLevels: List<Level> = emptyList(),
    val dueCardCount: Int = 0,
    val errorExerciseCount: Int = 0,
    val error: String? = null
)

/** ID do único usuário do app (modo single-user). */
private const val DEFAULT_USER_ID = 1L

/**
 * ViewModel da HomeScreen.
 *
 * Combina [GetUserStatsUseCase], [CheckDailyStreakUseCase] e a contagem reativa de
 * SRS cards devidos como Flows, e carrega os níveis disponíveis via
 * [GetAvailableLevelsUseCase] (one-shot suspend).
 *
 * Requisito 5.6: streak exibido na HomeScreen.
 * Requisito 6.3: informações sobre níveis desbloqueados disponíveis na UI.
 * Requisito 12.1: estado inicial da HomeScreen após navegação.
 * Requisito 12.7: exibir mensagem de erro amigável com opção de retry.
 * Requisito 16.6: badge "Revisar Agora" com [HomeUiState.dueCardCount] (visível quando > 0).
 * Requisito 18.4: seção "Revisar Agora" exibida apenas quando há cards devidos.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getUserStatsUseCase: GetUserStatsUseCase,
    private val getAvailableLevelsUseCase: GetAvailableLevelsUseCase,
    private val checkDailyStreakUseCase: CheckDailyStreakUseCase,
    private val srsRepository: SrsRepository,
    private val progressDao: ProgressDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    /**
     * Carrega os dados da HomeScreen:
     * 1. Busca os níveis disponíveis (one-shot suspend).
     * 2. Combina o Flow de estatísticas do usuário, o Flow de streak diário, o Flow de
     *    contagem de SRS cards devidos e o Flow de exercícios com erros,
     *    emitindo um novo [HomeUiState] a cada atualização.
     *
     * Precondição: repositórios disponíveis via injeção de dependência.
     * Postcondição: [uiState] transiciona de isLoading=true para isLoading=false
     *               (com dados ou com erro).
     */
    private fun loadHomeData() {
        viewModelScope.launch {
            try {
                // One-shot: carrega os níveis disponíveis antes de observar os fluxos
                val levels = getAvailableLevelsUseCase()

                // Combina stats, streak, contagem de SRS cards devidos e erros (Req. 5.6, 6.3, 16.6)
                combine(
                    getUserStatsUseCase(),
                    checkDailyStreakUseCase(),
                    srsRepository.observeDueCardCount(DEFAULT_USER_ID),
                    progressDao.observeIncorrectExerciseCount(DEFAULT_USER_ID)
                ) { stats, streak, dueCount, errorCount ->
                    HomeUiState(
                        isLoading = false,
                        userStats = stats,
                        currentStreak = streak,
                        unlockedLevels = levels.filter { it.isUnlocked },
                        dueCardCount = dueCount,
                        errorExerciseCount = errorCount,
                        error = null
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                // Req. 12.7: exibir mensagem de erro amigável
                _uiState.value = HomeUiState(
                    isLoading = false,
                    error = e.message ?: "Erro ao carregar dados"
                )
            }
        }
    }

    /**
     * Reinicia o carregamento dos dados após uma falha.
     *
     * Requisito 12.7: opção de retry quando uiState transiciona para Error.
     */
    fun retry() {
        _uiState.value = HomeUiState(isLoading = true)
        loadHomeData()
    }
}
