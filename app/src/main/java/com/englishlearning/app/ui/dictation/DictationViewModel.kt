package com.englishlearning.app.ui.dictation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.domain.model.DictationExercise
import com.englishlearning.app.domain.model.DictationResult
import com.englishlearning.app.domain.model.DictationMistake
import com.englishlearning.app.domain.model.MistakeType
import com.englishlearning.app.domain.repository.DictationRepository
import com.englishlearning.app.domain.repository.TtsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para a tela de Ditado no estilo Callan Method.
 *
 * Fluxo de reprodução por segmentos (3× cada):
 *   Para cada segmento:
 *     1ª vez → velocidade normal (1.0×)
 *     2ª vez → velocidade lenta  (0.75×)  ← tempo para escrever
 *     3ª vez → velocidade normal (1.0×)   ← confirmação
 *   Pausa entre segmentos.
 * Após todos os segmentos: libera campo de texto para o aluno digitar.
 */
@HiltViewModel
class DictationViewModel @Inject constructor(
    private val dictationRepository: DictationRepository,
    private val ttsRepository: TtsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DictationUiState())
    val uiState: StateFlow<DictationUiState> = _uiState.asStateFlow()

    private var exercises = listOf<DictationExercise>()
    private var currentIndex = 0
    private var sessionStartTime = System.currentTimeMillis()
    private val results = mutableListOf<DictationResult>()
    private var playbackJob: Job? = null

    init {
        loadAvailableStages()
    }

    private fun loadAvailableStages() {
        viewModelScope.launch {
            val stages = dictationRepository.getAvailableStages()
            _uiState.update {
                it.copy(isLoading = false, isSelectingStage = true, availableStages = stages)
            }
        }
    }

    fun selectStage(stage: Int) {
        _uiState.update { it.copy(selectedStage = stage, isSelectingStage = false) }
        loadExercisesByStage(stage)
    }

    private fun loadExercisesByStage(stage: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                exercises = dictationRepository.getByStage(stage)
                val completedCount = exercises.count { it.completed }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentExercise = exercises.firstOrNull(),
                        totalExercises = exercises.size,
                        completedInStage = completedCount
                    )
                }
                if (exercises.isNotEmpty()) {
                    startSegmentPlayback()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // ─── Playback por segmentos ───────────────────────────────────────────────

    /**
     * Inicia a reprodução completa do ditado: cada segmento é lido 3 vezes.
     * Repetições: normal → lento → normal.
     * Só libera o campo de entrada após todos os segmentos.
     */
    fun startSegmentPlayback() {
        val exercise = _uiState.value.currentExercise ?: return
        val segments = exercise.effectiveSegments

        playbackJob?.cancel()
        _uiState.update {
            it.copy(
                isPlayingAudio = true,
                allSegmentsPlayed = false,
                currentSegmentIndex = 0,
                currentRepetition = 0,
                totalSegments = segments.size,
                userInput = "",
                isChecked = false,
                currentResult = null,
                hintsUsed = 0,
                currentHint = null
            )
        }

        playbackJob = viewModelScope.launch {
            segments.forEachIndexed { segIdx, segment ->
                _uiState.update { it.copy(currentSegmentIndex = segIdx) }

                // 3 repetições por segmento
                val speeds = listOf(1.0f, 0.75f, 1.0f)
                speeds.forEachIndexed { repIdx, speed ->
                    _uiState.update { it.copy(currentRepetition = repIdx + 1) }
                    ttsRepository.speak(segment, speechRate = speed)
                    // Aguarda o TTS terminar
                    waitForTtsToFinish()
                    // Pausa entre repetições (maior na 2ª para escrever)
                    if (repIdx < 2) delay(if (repIdx == 1) 3000L else 1500L)
                }

                // Pausa entre segmentos
                if (segIdx < segments.lastIndex) delay(2500L)
            }

            _uiState.update {
                it.copy(
                    isPlayingAudio = false,
                    allSegmentsPlayed = true,
                    currentRepetition = 0
                )
            }
        }
    }

    /** Repete apenas o segmento atual (botão 🔄 por segmento). */
    fun replayCurrentSegment() {
        val exercise = _uiState.value.currentExercise ?: return
        val segments = exercise.effectiveSegments
        val segIdx = _uiState.value.currentSegmentIndex.coerceIn(0, segments.lastIndex)
        val segment = segments[segIdx]

        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            _uiState.update { it.copy(isPlayingAudio = true, currentRepetition = 1) }
            ttsRepository.speak(segment, speechRate = 1.0f)
            waitForTtsToFinish()
            delay(1500L)
            _uiState.update { it.copy(currentRepetition = 2) }
            ttsRepository.speak(segment, speechRate = 0.75f)
            waitForTtsToFinish()
            delay(1500L)
            _uiState.update { it.copy(isPlayingAudio = false, currentRepetition = 0) }
        }
    }

    /** Repete o ditado completo desde o início. */
    fun replayAll() {
        ttsRepository.stop()
        playbackJob?.cancel()
        startSegmentPlayback()
    }

    private suspend fun waitForTtsToFinish() {
        delay(300L) // pequena espera para o TTS iniciar
        while (ttsRepository.isSpeaking()) {
            delay(100L)
        }
    }

    // ─── Input e verificação ─────────────────────────────────────────────────

    fun updateInput(text: String) {
        _uiState.update { it.copy(userInput = text) }
    }

    fun checkAnswer() {
        val exercise = _uiState.value.currentExercise ?: return
        val userInput = _uiState.value.userInput.trim()
        val startTime = sessionStartTime

        viewModelScope.launch {
            val result = calculateResult(
                exercise = exercise,
                userText = userInput,
                timeTaken = System.currentTimeMillis() - startTime,
                hintsUsed = _uiState.value.hintsUsed
            )
            results.add(result)
            dictationRepository.saveResult(1L, exercise.id, result)

            val completedInStage = if (result.score == 100 && !exercise.completed) {
                _uiState.value.completedInStage + 1
            } else {
                _uiState.value.completedInStage
            }

            _uiState.update {
                it.copy(isChecked = true, currentResult = result, completedInStage = completedInStage)
            }
        }
    }

    fun retryExercise() {
        sessionStartTime = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                userInput = "",
                isChecked = false,
                currentResult = null,
                hintsUsed = 0,
                currentHint = null,
                allSegmentsPlayed = false
            )
        }
        startSegmentPlayback()
    }

    fun useHint() {
        val exercise = _uiState.value.currentExercise ?: return
        val hintsUsed = _uiState.value.hintsUsed
        val hint = when (hintsUsed) {
            0 -> "Palavras: ${exercise.wordCount}"
            1 -> "Primeira letra: ${exercise.text.first().uppercaseChar()}"
            2 -> exercise.translation ?: "Sem tradução disponível"
            3 -> "Foco gramatical: ${exercise.grammarFocus ?: "N/A"}"
            else -> return
        }
        _uiState.update { it.copy(hintsUsed = hintsUsed + 1, currentHint = hint) }
    }

    fun nextExercise() {
        playbackJob?.cancel()
        ttsRepository.stop()
        currentIndex++
        sessionStartTime = System.currentTimeMillis()

        if (currentIndex >= exercises.size) {
            _uiState.update {
                it.copy(
                    isComplete = true,
                    averageScore = if (results.isEmpty()) 0f else results.map { r -> r.score }.average().toFloat()
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    currentExercise = exercises[currentIndex],
                    currentIndex = currentIndex,
                    progress = currentIndex.toFloat() / exercises.size,
                    userInput = "",
                    isChecked = false,
                    currentResult = null,
                    hintsUsed = 0,
                    currentHint = null,
                    allSegmentsPlayed = false,
                    currentSegmentIndex = 0,
                    currentRepetition = 0
                )
            }
            startSegmentPlayback()
        }
    }

    fun backToStageSelection() {
        playbackJob?.cancel()
        ttsRepository.stop()
        currentIndex = 0
        exercises = emptyList()
        results.clear()
        _uiState.update {
            DictationUiState(
                isLoading = false,
                isSelectingStage = true,
                availableStages = it.availableStages
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        ttsRepository.stop()
    }

    // ─── Cálculo de resultado ─────────────────────────────────────────────────

    private fun calculateResult(
        exercise: DictationExercise,
        userText: String,
        timeTaken: Long,
        hintsUsed: Int
    ): DictationResult {
        val expectedWords = exercise.text.lowercase()
            .replace(Regex("[^a-z\\s']"), "")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        val userWords = userText.lowercase()
            .replace(Regex("[^a-z\\s']"), "")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        val mistakes = mutableListOf<DictationMistake>()
        var correctCount = 0
        val maxLen = maxOf(expectedWords.size, userWords.size)

        for (i in 0 until maxLen) {
            val expected = expectedWords.getOrNull(i)
            val actual = userWords.getOrNull(i)
            when {
                expected == null && actual != null ->
                    mistakes.add(DictationMistake(i, "", actual, MistakeType.EXTRA_WORD))
                expected != null && actual == null ->
                    mistakes.add(DictationMistake(i, expected, null, MistakeType.MISSING_WORD))
                expected != null && actual != null && expected != actual -> {
                    val type = if (levenshteinDistance(expected, actual) <= 2) MistakeType.SPELLING
                               else MistakeType.WRONG_WORD
                    mistakes.add(DictationMistake(i, expected, actual, type))
                }
                else -> correctCount++
            }
        }

        val accuracy = if (expectedWords.isNotEmpty()) correctCount.toFloat() / expectedWords.size else 0f
        val score = ((accuracy * 100).toInt() - hintsUsed * 5).coerceIn(0, 100)

        return DictationResult(
            exerciseId = exercise.id,
            expectedText = exercise.text,
            userText = userText,
            score = score,
            accuracy = accuracy,
            mistakes = mistakes,
            timeTakenMs = timeTaken,
            hintsUsed = hintsUsed
        )
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length; val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) for (j in 1..n) {
            dp[i][j] = if (s1[i - 1] == s2[j - 1]) dp[i - 1][j - 1]
                        else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
        }
        return dp[m][n]
    }
}

/** Retorna os segmentos definidos ou divide automaticamente por frases. */
val DictationExercise.effectiveSegments: List<String>
    get() = if (segments.isNotEmpty()) segments
            else text.split(Regex("(?<=[.?!])\\s+")).filter { it.isNotBlank() }

/**
 * Estado da UI de Ditado (estilo Callan Method).
 */
data class DictationUiState(
    val isLoading: Boolean = true,
    val currentExercise: DictationExercise? = null,
    val currentIndex: Int = 0,
    val totalExercises: Int = 0,
    val progress: Float = 0f,
    val userInput: String = "",
    val isChecked: Boolean = false,
    val currentResult: DictationResult? = null,
    val hintsUsed: Int = 0,
    val currentHint: String? = null,
    val isComplete: Boolean = false,
    val averageScore: Float = 0f,
    val error: String? = null,
    // Stage selection
    val availableStages: List<Int> = (1..12).toList(),
    val selectedStage: Int = 1,
    val isSelectingStage: Boolean = true,
    val completedInStage: Int = 0,
    // Playback por segmentos (3× cada)
    val isPlayingAudio: Boolean = false,
    val allSegmentsPlayed: Boolean = false,
    val currentSegmentIndex: Int = 0,
    val currentRepetition: Int = 0,  // 1, 2 ou 3 (0 = inativo)
    val totalSegments: Int = 0
)
