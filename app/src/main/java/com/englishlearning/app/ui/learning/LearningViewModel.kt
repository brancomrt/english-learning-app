package com.englishlearning.app.ui.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.domain.gamification.LevelProgressionAlgorithm
import com.englishlearning.app.domain.model.Achievement
import com.englishlearning.app.domain.model.AttemptResult
import com.englishlearning.app.domain.model.Exercise
import com.englishlearning.app.domain.model.Hint
import com.englishlearning.app.domain.model.SessionSummary
import com.englishlearning.app.domain.model.ValidationResult
import com.englishlearning.app.domain.model.ValidationStatus
import com.englishlearning.app.domain.usecase.CheckAchievementsUseCase
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

// ---------------------------------------------------------------------------
// UI State
// ---------------------------------------------------------------------------

/**
 * Estado da tela de sessão de aprendizado.
 *
 * Requisito 13.2: LearningViewModel mantém o estado da sessão como um StateFlow.
 * Requisito 12.7: Em caso de Error, a UI deve exibir mensagem amigável com opção de retry.
 */
sealed class LearningUiState {
    /** Estado inicial antes de qualquer sessão ser iniciada. */
    object Idle : LearningUiState()

    /** Exercícios sendo carregados do repositório. */
    object Loading : LearningUiState()

    /**
     * Exercício ativo exibido ao usuário.
     *
     * Requisito 1.1: exibir answerText do exercício atual.
     * Requisito 1.6: exibir número de exercícios restantes.
     * Requisito 3.1/3.2: revelar dicas progressivamente em revealedHints.
     */
    data class ExerciseActive(
        val exercise: Exercise,
        val currentIndex: Int,
        val totalExercises: Int,
        val attemptCount: Int,
        val revealedHints: List<Hint>,
        val lastValidationResult: ValidationResult? = null,
        val sessionXp: Int = 0,
        /** true quando o usuário clicou em "Ver resposta" sem tentar responder. */
        val answerRevealed: Boolean = false
    ) : LearningUiState()

    /**
     * Sessão concluída com resumo final.
     *
     * Requisito 13.3: transicionar para SessionComplete quando todos os exercícios forem completados.
     * Requisito 13.4: SessionSummary inclui total de exercícios, acertos, XP e tempo decorrido.
     */
    data class SessionComplete(val summary: SessionSummary) : LearningUiState()

    /**
     * Erro durante o carregamento ou execução da sessão.
     *
     * Requisito 12.7: exibir mensagem de erro amigável com opção de retry.
     */
    data class Error(val message: String) : LearningUiState()
}

// ---------------------------------------------------------------------------
// One-shot Events
// ---------------------------------------------------------------------------

/**
 * Eventos de disparo único emitidos pela LearningViewModel via Channel.
 *
 * Requisito 3.4: feedback positivo com score e XP ao acertar.
 * Requisito 8.3: notificação visual imediata ao desbloquear conquista.
 * Requisito 13.3: evento de sessão finalizada.
 */
sealed class LearningEvent {
    /**
     * Feedback de validação com XP ganho.
     * Requisito 3.4: exibir feedback visual com score e XP.
     * Requisito 3.5: indicar "quase certa" quando TOO_CLOSE.
     */
    data class ShowFeedback(val result: ValidationResult, val xpEarned: Int) : LearningEvent()

    /**
     * Conquista desbloqueada durante a sessão.
     * Requisito 8.3: notificação imediata ao desbloquear conquista.
     */
    data class AchievementUnlocked(val achievement: Achievement) : LearningEvent()

    /** Sinaliza que a sessão foi finalizada. Requisito 13.3. */
    object SessionFinished : LearningEvent()
}

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

/**
 * ViewModel que gerencia a sessão de aprendizado (mecânica invertida).
 *
 * Responsabilidades:
 * - [startSession]: Carrega exercícios para o nível/categoria selecionados (Req. 13.1).
 * - [submitAnswer]: Valida a pergunta formulada pelo usuário (Req. 2.1).
 * - [moveToNext]: Avança para o próximo exercício ou finaliza a sessão (Req. 13.3).
 * - [requestHint]: Revela dicas progressivamente em ordem de hintOrder (Req. 3.2).
 * - Estado exposto como [StateFlow] de [LearningUiState] (Req. 13.2).
 * - Eventos de disparo único via [Channel] (conquistas, feedback, sessão finalizada).
 * - Estado preservado ao navegar para outra tela (Req. 13.5) via ViewModel lifecycle.
 *
 * @param loadExercisesUseCase    Carrega exercícios do ContentRepository.
 * @param validateAnswerUseCase   Valida a pergunta formulada pelo usuário.
 * @param updateProgressUseCase   Persiste tentativas e atualiza streak.
 * @param checkAchievementsUseCase Verifica e desbloqueia conquistas.
 */
@HiltViewModel
class LearningViewModel @Inject constructor(
    private val loadExercisesUseCase: LoadExercisesUseCase,
    private val validateAnswerUseCase: ValidateAnswerUseCase,
    private val updateProgressUseCase: UpdateProgressUseCase,
    private val checkAchievementsUseCase: CheckAchievementsUseCase
) : ViewModel() {

    // -------------------------------------------------------------------------
    // State & Events
    // -------------------------------------------------------------------------

    private val _uiState = MutableStateFlow<LearningUiState>(LearningUiState.Idle)

    /**
     * Estado observável da tela de aprendizado.
     * Requisito 13.2: StateFlow de LearningUiState.
     * Requisito 13.5: estado preservado enquanto o ViewModel estiver ativo.
     */
    val uiState: StateFlow<LearningUiState> = _uiState.asStateFlow()

    private val _events = Channel<LearningEvent>(Channel.BUFFERED)

    /**
     * Fluxo de eventos de disparo único consumidos pela UI.
     * Usa Channel para garantir entrega exatamente uma vez mesmo que a UI
     * não esteja coletando no momento do envio.
     */
    val events: Flow<LearningEvent> = _events.receiveAsFlow()

    // -------------------------------------------------------------------------
    // Session state (internal)
    // -------------------------------------------------------------------------

    private var exercises: List<Exercise> = emptyList()
    private var currentIndex = 0
    private var sessionStartTime = 0L
    private var sessionCorrectCount = 0
    private var sessionTotalXp = 0
    private var levelId = 0
    private var categoryId = 0
    private var currentAttemptCount = 0
    private val unlockedAchievementsThisSession = mutableListOf<Achievement>()

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Inicia uma nova sessão de aprendizado carregando os exercícios do
     * ContentRepository para o nível e categoria selecionados.
     *
     * Transições de estado:
     * - [LearningUiState.Loading] → enquanto os exercícios são carregados.
     * - [LearningUiState.ExerciseActive] → quando os exercícios são carregados com sucesso.
     * - [LearningUiState.Error] → quando há falha ao carregar.
     *
     * Requisito 13.1: carregar exercícios usando levelId e categoryId.
     * Requisito 12.7: transicionar para Error com mensagem amigável em caso de falha.
     *
     * @param levelId    ID do nível selecionado pelo usuário.
     * @param categoryId ID da categoria selecionada pelo usuário.
     */
    fun startSession(levelId: Int, categoryId: Int) {
        this.levelId = levelId
        this.categoryId = categoryId
        viewModelScope.launch {
            _uiState.value = LearningUiState.Loading
            loadExercisesUseCase(levelId, categoryId).fold(
                onSuccess = { list ->
                    exercises = list
                    currentIndex = 0
                    sessionStartTime = System.currentTimeMillis()
                    sessionCorrectCount = 0
                    sessionTotalXp = 0
                    currentAttemptCount = 0
                    unlockedAchievementsThisSession.clear()
                    showCurrentExercise()
                },
                onFailure = { e ->
                    _uiState.value = LearningUiState.Error(
                        e.message ?: "Erro ao carregar exercícios. Tente novamente."
                    )
                }
            )
        }
    }

    /**
     * Valida a pergunta formulada pelo usuário para o exercício atual.
     *
     * Fluxo:
     * 1. Valida a pergunta via [ValidateAnswerUseCase] (síncrono, sem IO).
     * 2. Calcula o XP via [LevelProgressionAlgorithm.calculatePoints].
     * 3. Persiste a tentativa via [UpdateProgressUseCase].
     * 4. Emite [LearningEvent.ShowFeedback] com o resultado e XP.
     * 5. Verifica novas conquistas via [CheckAchievementsUseCase].
     * 6. Emite [LearningEvent.AchievementUnlocked] para cada nova conquista.
     * 7. Atualiza o [_uiState] com o resultado da validação.
     *
     * Ignorado silenciosamente se o estado atual não for [LearningUiState.ExerciseActive].
     *
     * Requisito 2.1: validar pergunta submetida pelo usuário.
     * Requisito 3.1: exibir primeira dica ao errar na primeira tentativa.
     * Requisito 4.1: calcular XP baseado em dificuldade, tentativas e streak.
     * Requisito 8.2/8.3: desbloquear e notificar conquistas imediatamente.
     *
     * @param userQuestion Pergunta digitada pelo usuário.
     */
    fun submitAnswer(userQuestion: String) {
        val state = _uiState.value as? LearningUiState.ExerciseActive ?: return
        val exercise = exercises.getOrNull(currentIndex) ?: return

        viewModelScope.launch {
            // 1. Validate (synchronous — no suspend needed)
            val result = validateAnswerUseCase(userQuestion, exercise)

            // 2. Calculate XP earned
            val xpEarned = LevelProgressionAlgorithm.calculatePoints(
                status = result.status,
                difficultyScore = exercise.difficultyScore,
                attemptNumber = state.attemptCount + 1,
                hasActiveStreak = false // streak status checked independently
            )

            // 3. Track session counters
            currentAttemptCount++
            if (result.status == ValidationStatus.CORRECT) sessionCorrectCount++
            sessionTotalXp += xpEarned

            // 4. Persist attempt and update streak
            updateProgressUseCase(
                userId = CheckAchievementsUseCase.DEFAULT_USER_ID,
                levelId = levelId,
                categoryId = categoryId,
                attemptResult = AttemptResult(
                    exerciseId = exercise.id,
                    userId = CheckAchievementsUseCase.DEFAULT_USER_ID,
                    userAnswer = userQuestion,
                    validationResult = result,
                    xpEarned = xpEarned,
                    attemptedAt = System.currentTimeMillis()
                )
            )

            // 5. Emit feedback event (Req. 3.4, 3.5)
            _events.send(LearningEvent.ShowFeedback(result, xpEarned))

            // 6. Check for newly unlocked achievements (Req. 8.2, 8.3)
            val newAchievements = checkAchievementsUseCase()
            newAchievements.forEach { achievement ->
                unlockedAchievementsThisSession.add(achievement)
                _events.send(LearningEvent.AchievementUnlocked(achievement))
            }

            // 7. Update UI state with validation result and updated XP
            _uiState.value = state.copy(
                attemptCount = state.attemptCount + 1,
                lastValidationResult = result,
                sessionXp = sessionTotalXp
            )
        }
    }

    /**
     * Avança para o próximo exercício da fila ou finaliza a sessão.
     *
     * Lógica da fila:
     * - Se ainda há exercícios não visitados, incrementa [currentIndex] e chama
     *   [showCurrentExercise] para exibir o próximo.
     * - Se o exercício atual é o último, gera um [SessionSummary] com os dados
     *   acumulados e transiciona para [LearningUiState.SessionComplete].
     *
     * Ignorado silenciosamente se o estado atual não for [LearningUiState.ExerciseActive].
     *
     * Requisito 13.3: transicionar para SessionComplete ao completar todos os exercícios.
     * Requisito 13.4: SessionSummary com totalExercises, correctAnswers, totalXpEarned e
     *                  tempo decorrido.
     */
    fun moveToNext() {
        if (currentIndex < exercises.size - 1) {
            // More exercises remain
            currentIndex++
            currentAttemptCount = 0
            showCurrentExercise()
        } else {
            // All exercises completed — build summary and complete the session
            val elapsed = System.currentTimeMillis() - sessionStartTime
            val summary = SessionSummary(
                totalExercises = exercises.size,
                correctAnswers = sessionCorrectCount,
                totalXpEarned = sessionTotalXp,
                durationMillis = elapsed,
                levelId = levelId,
                categoryId = categoryId,
                achievementsUnlocked = unlockedAchievementsThisSession.toList()
            )
            _uiState.value = LearningUiState.SessionComplete(summary)
            viewModelScope.launch {
                _events.send(LearningEvent.SessionFinished)
            }
        }
    }

    /**
     * Descarta o resultado da última validação para que o usuário possa
     * tentar novamente sem avançar ao próximo exercício.
     *
     * Utilizado pelo FeedbackScreen quando o usuário pressiona "Tentar novamente".
     * Não altera o contador de tentativas (já incrementado em [submitAnswer]).
     */
    fun clearLastResult() {
        val state = _uiState.value as? LearningUiState.ExerciseActive ?: return
        _uiState.value = state.copy(lastValidationResult = null)
    }

    /**
     * Revela a próxima dica progressiva do exercício atual.
     *
     * A revelação segue a ordem de [Hint.hintOrder]: cada chamada revela a próxima
     * dica não exibida. Dicas do tipo ANSWER (reveladoras) aparecem apenas depois
     * que todas as dicas anteriores já foram reveladas, respeitando a ordem natural.
     *
     * Ignorado silenciosamente se o estado atual não for [LearningUiState.ExerciseActive]
     * ou se todas as dicas já foram reveladas.
     *
     * Requisito 3.1: exibir a primeira dica ao errar na primeira tentativa.
     * Requisito 3.2: revelar dicas progressivamente em ordem de hintOrder.
     * Requisito 3.3: dicas do tipo ANSWER exibidas apenas após as anteriores.
     *
     * @see Hint.hintOrder
     * @see com.englishlearning.app.domain.model.HintType.ANSWER
     */
    fun requestHint() {
        val state = _uiState.value as? LearningUiState.ExerciseActive ?: return
        val exercise = exercises.getOrNull(currentIndex) ?: return
        val nextHintIndex = state.revealedHints.size
        if (nextHintIndex < exercise.hints.size) {
            _uiState.value = state.copy(
                revealedHints = exercise.hints.take(nextHintIndex + 1)
            )
        }
    }

    /**
     * Revela a resposta aceita principal sem contar como tentativa.
     * O usuário verá a resposta e deverá clicar em "Próximo" para avançar.
     */
    fun revealAnswer() {
        val state = _uiState.value as? LearningUiState.ExerciseActive ?: return
        _uiState.value = state.copy(answerRevealed = true)
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Transiciona o estado para [LearningUiState.ExerciseActive] com o exercício
     * no índice [currentIndex], resetando as dicas e o resultado anterior.
     *
     * Requisito 1.1: exibir answerText do exercício atual na tela de aprendizado.
     * Requisito 1.6: exibir número de exercícios restantes (totalExercises - currentIndex - 1).
     */
    private fun showCurrentExercise() {
        if (exercises.isEmpty()) return
        val exercise = exercises[currentIndex]
        _uiState.value = LearningUiState.ExerciseActive(
            exercise = exercise,
            currentIndex = currentIndex,
            totalExercises = exercises.size,
            attemptCount = 0,
            revealedHints = emptyList()
        )
    }
}
