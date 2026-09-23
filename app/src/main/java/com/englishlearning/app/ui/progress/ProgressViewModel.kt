package com.englishlearning.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.domain.model.Achievement
import com.englishlearning.app.domain.model.UserStats
import com.englishlearning.app.domain.repository.ProgressRepository
import com.englishlearning.app.domain.usecase.CheckAchievementsUseCase
import com.englishlearning.app.domain.usecase.CheckDailyStreakUseCase
import com.englishlearning.app.domain.usecase.GetUserStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado da ProgressScreen.
 *
 * Requisito 8.4: exibir conquistas desbloqueadas e bloqueadas.
 * Requisito 5.5: exibir currentStreak e longestStreak.
 */
data class ProgressUiState(
    val isLoading: Boolean = true,
    val achievements: List<Achievement> = emptyList(),
    val userStats: UserStats? = null,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val error: String? = null
)

/**
 * ViewModel da tela de progresso (ProgressScreen).
 *
 * Responsabilidades:
 * - Observar conquistas do usuário via [CheckAchievementsUseCase.observe] (Req. 8.4).
 * - Observar estatísticas gerais via [GetUserStatsUseCase] (Req. 6.3, 5.6).
 * - Observar o streak diário atual via [CheckDailyStreakUseCase] (Req. 5.5, 5.6).
 * - Combinar os três fluxos em um único [StateFlow] de [ProgressUiState].
 * - Marcar conquistas como visualizadas via [markAchievementsAsSeen] (Req. 8.5).
 *
 * @param checkAchievementsUseCase Caso de uso para observar conquistas do usuário.
 * @param getUserStatsUseCase      Caso de uso para observar estatísticas gerais.
 * @param checkDailyStreakUseCase  Caso de uso para observar o streak diário atual.
 * @param progressRepository       Repositório para marcar conquistas como vistas e
 *                                 observar o registro de streak completo (longestStreak).
 */
@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val checkAchievementsUseCase: CheckAchievementsUseCase,
    private val getUserStatsUseCase: GetUserStatsUseCase,
    private val checkDailyStreakUseCase: CheckDailyStreakUseCase,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    /**
     * Estado da UI combinado a partir dos três fluxos reativos.
     *
     * Usa [combine] para manter a UI sincronizada com qualquer atualização em:
     * - conquistas (achievements)
     * - estatísticas gerais (userStats, longestStreak)
     * - streak atual (currentStreak)
     *
     * Requisito 8.4: conquistas desbloqueadas e bloqueadas visíveis na ProgressScreen.
     * Requisito 5.5: currentStreak e longestStreak exibidos.
     * Requisito 6.3: informações de nível atual disponíveis via UserStats.
     */
    val uiState: StateFlow<ProgressUiState> = combine(
        checkAchievementsUseCase.observe(DEFAULT_USER_ID),
        getUserStatsUseCase(DEFAULT_USER_ID),
        progressRepository.observeStreak(DEFAULT_USER_ID)
    ) { achievements, userStats, streakRecord ->
        ProgressUiState(
            isLoading = false,
            achievements = achievements,
            userStats = userStats,
            currentStreak = streakRecord?.currentStreak ?: userStats.currentStreak,
            longestStreak = streakRecord?.longestStreak ?: userStats.longestStreak,
            error = null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProgressUiState(isLoading = true)
    )

    /**
     * Marca todas as conquistas do usuário como visualizadas (`isNew = false`).
     *
     * Deve ser chamado quando o usuário abre a ProgressScreen para que o indicador
     * de "nova conquista" seja removido.
     *
     * Requisito 8.5: isNew atualizado para false após visualização pelo usuário.
     */
    fun markAchievementsAsSeen() {
        viewModelScope.launch {
            progressRepository.markAchievementsAsSeen(DEFAULT_USER_ID)
        }
    }

    companion object {
        private const val DEFAULT_USER_ID = 1L
    }
}
