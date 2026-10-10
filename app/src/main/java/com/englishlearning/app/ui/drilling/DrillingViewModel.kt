package com.englishlearning.app.ui.drilling

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.data.repository.SessionSettingsRepository
import com.englishlearning.app.domain.validation.TextNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Modelo de um drill para prática de perguntas e respostas.
 */
@Serializable
data class Drill(
    val id: Int,
    val level: Int,
    val prompt: String,
    val answer: String,
    @SerialName("alt_answers") val altAnswers: List<String> = emptyList(),
    val grammar: String
)

@Serializable
data class DrillsFile(
    val description: String,
    val drills: List<Drill>
)

sealed class DrillUiState {
    object Loading : DrillUiState()
    data class Question(
        val drill: Drill,
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
class DrillingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SessionSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DrillUiState>(DrillUiState.Loading)
    val uiState: StateFlow<DrillUiState> = _uiState.asStateFlow()

    // Expõe as configurações atuais para a UI poder exibi-las
    private val _stageRange = MutableStateFlow(
        settingsRepository.getDrillingMinStage()..settingsRepository.getDrillingMaxStage()
    )
    val stageRange: StateFlow<IntRange> = _stageRange.asStateFlow()

    private val normalizer = TextNormalizer()
    private val json = Json { ignoreUnknownKeys = true }
    
    private var allDrills: List<Drill> = emptyList()
    private var sessionDrills: List<Drill> = emptyList()
    private var currentIndex = 0
    private var score = 0
    private var timerJob: Job? = null

    init {
        loadDrills()
    }

    private fun loadDrills() {
        viewModelScope.launch {
            _uiState.value = DrillUiState.Loading
            try {
                val jsonString = context.assets
                    .open("content/drills.json")
                    .bufferedReader()
                    .use { it.readText() }
                
                val drillsFile = json.decodeFromString<DrillsFile>(jsonString)
                allDrills = drillsFile.drills

                startSession()
            } catch (e: Exception) {
                _uiState.value = DrillUiState.Done(0, 0, 0)
            }
        }
    }

    /** Inicia uma nova sessão lendo as configurações mais recentes de stage e tamanho. */
    private fun startSession() {
        val minStage  = settingsRepository.getDrillingMinStage()
        val maxStage  = settingsRepository.getDrillingMaxStage()
        val drillSize = settingsRepository.getDrillingSize()

        _stageRange.value = minStage..maxStage

        // Filtra pelo intervalo de stages configurado
        val filtered = allDrills.filter { it.level in minStage..maxStage }
        sessionDrills = filtered.shuffled().take(drillSize)

        if (sessionDrills.isEmpty()) {
            _uiState.value = DrillUiState.Done(0, 0, 0)
        } else {
            currentIndex = 0
            score = 0
            showQuestion()
        }
    }

    private fun showQuestion() {
        val drill = sessionDrills.getOrNull(currentIndex) ?: return
        _uiState.value = DrillUiState.Question(
            drill = drill,
            index = currentIndex + 1,
            total = sessionDrills.size,
            score = score,
            secondsLeft = TIMER_SECONDS,
            showResult = false
        )
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            repeat(TIMER_SECONDS) { elapsed ->
                delay(1000)
                val s = _uiState.value
                if (s is DrillUiState.Question && !s.showResult) {
                    val left = TIMER_SECONDS - elapsed - 1
                    _uiState.value = s.copy(secondsLeft = left)
                    if (left == 0) {
                        // Tempo esgotado — mostra resposta correta
                        _uiState.value = s.copy(
                            showResult = true,
                            wasCorrect = false,
                            correctAnswer = s.drill.answer
                        )
                    }
                }
            }
        }
    }

    fun submitAnswer(input: String) {
        val s = _uiState.value as? DrillUiState.Question ?: return
        if (s.showResult) return
        
        timerJob?.cancel()

        val normalizedInput = normalizer.normalize(input)
        val drill = s.drill
        
        // Verifica se a resposta está correta
        val allAccepted = listOf(drill.answer) + drill.altAnswers
        val isCorrect = allAccepted.any { accepted ->
            val normalizedAccepted = normalizer.normalize(accepted)
            // Aceita se for igual ou muito similar (permite pequenos erros)
            normalizedInput == normalizedAccepted ||
            calculateSimilarity(normalizedInput, normalizedAccepted) >= 0.85
        }
        
        if (isCorrect) score++

        _uiState.value = s.copy(
            showResult = true,
            wasCorrect = isCorrect,
            correctAnswer = drill.answer,
            score = score
        )
    }

    /**
     * Calcula similaridade entre duas strings (0.0 a 1.0).
     * Usa distância de Levenshtein normalizada.
     */
    private fun calculateSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0
        
        val maxLen = maxOf(s1.length, s2.length)
        val distance = levenshteinDistance(s1, s2)
        return 1.0 - (distance.toDouble() / maxLen)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[m][n]
    }

    fun next() {
        currentIndex++
        if (currentIndex >= sessionDrills.size) {
            val pct = if (sessionDrills.isNotEmpty()) (score * 100) / sessionDrills.size else 0
            _uiState.value = DrillUiState.Done(score, sessionDrills.size, pct)
        } else {
            showQuestion()
        }
    }

    fun restart() {
        currentIndex = 0
        score = 0
        // Re-lê configurações para pegar mudanças feitas nas Settings
        startSession()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        const val TIMER_SECONDS = 15
        // DRILL_SIZE kept for backward compatibility
        const val DRILL_SIZE = 20
    }
}
