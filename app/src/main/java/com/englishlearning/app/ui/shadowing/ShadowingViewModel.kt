package com.englishlearning.app.ui.shadowing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.domain.model.ChallengeType
import com.englishlearning.app.domain.model.Exercise
import com.englishlearning.app.domain.model.ShadowingFeedback
import com.englishlearning.app.domain.model.ShadowingResult
import com.englishlearning.app.domain.model.ShadowingState
import com.englishlearning.app.domain.repository.DailyChallengeRepository
import com.englishlearning.app.domain.usecase.LoadExercisesUseCase
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

sealed class ShadowingUiState {
    object Loading : ShadowingUiState()
    
    data class Active(
        val exercise: Exercise,
        val currentIndex: Int,
        val totalExercises: Int,
        val shadowingState: ShadowingState,
        val lastResult: ShadowingResult? = null,
        val sessionScore: Float = 0f, // Média de similaridade
        val totalAttempts: Int = 0
    ) : ShadowingUiState()
    
    data class Complete(
        val totalExercises: Int,
        val averageScore: Float,
        val excellentCount: Int,
        val goodCount: Int,
        val needsWorkCount: Int
    ) : ShadowingUiState()
    
    data class Error(val message: String) : ShadowingUiState()
}

sealed class ShadowingEvent {
    object PlayModelAudio : ShadowingEvent()
    object StartRecording : ShadowingEvent()
    data class ShowResult(val feedback: ShadowingFeedback) : ShadowingEvent()
}

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

/**
 * ViewModel para o modo Shadowing/Echo.
 * 
 * O usuário ouve uma frase modelo e tenta repeti-la imediatamente,
 * treinando pronúncia e fluência.
 */
@HiltViewModel
class ShadowingViewModel @Inject constructor(
    private val loadExercisesUseCase: LoadExercisesUseCase,
    private val dailyChallengeRepository: DailyChallengeRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ShadowingUiState>(ShadowingUiState.Loading)
    val uiState: StateFlow<ShadowingUiState> = _uiState.asStateFlow()
    
    private val _events = Channel<ShadowingEvent>(Channel.BUFFERED)
    val events: Flow<ShadowingEvent> = _events.receiveAsFlow()
    
    private var exercises: List<Exercise> = emptyList()
    private var currentIndex = 0
    private var scoreSum = 0f
    private var totalAttempts = 0
    private var excellentCount = 0
    private var goodCount = 0
    private var needsWorkCount = 0
    
    /**
     * Inicia uma sessão de Shadowing.
     */
    fun startSession(levelId: Int, categoryId: Int, exerciseLimit: Int = 10) {
        viewModelScope.launch {
            _uiState.value = ShadowingUiState.Loading
            
            loadExercisesUseCase(levelId, categoryId).fold(
                onSuccess = { list ->
                    exercises = list.take(exerciseLimit)
                    currentIndex = 0
                    scoreSum = 0f
                    totalAttempts = 0
                    excellentCount = 0
                    goodCount = 0
                    needsWorkCount = 0
                    showCurrentExercise()
                },
                onFailure = { e ->
                    _uiState.value = ShadowingUiState.Error(
                        e.message ?: "Erro ao carregar exercícios"
                    )
                }
            )
        }
    }
    
    /**
     * Registra que o áudio modelo foi reproduzido.
     */
    fun onModelAudioPlayed() {
        val state = _uiState.value as? ShadowingUiState.Active ?: return
        
        _uiState.value = state.copy(
            shadowingState = state.shadowingState.copy(modelAudioPlayed = true)
        )
    }
    
    /**
     * Inicia gravação da tentativa do usuário.
     */
    fun startRecording() {
        val state = _uiState.value as? ShadowingUiState.Active ?: return
        
        _uiState.value = state.copy(
            shadowingState = state.shadowingState.copy(isRecording = true)
        )
        
        viewModelScope.launch {
            _events.send(ShadowingEvent.StartRecording)
        }
    }
    
    /**
     * Processa o resultado da fala do usuário.
     */
    fun onSpeechResult(spokenText: String) {
        val state = _uiState.value as? ShadowingUiState.Active ?: return
        val exercise = exercises.getOrNull(currentIndex) ?: return
        
        // Calcula similaridade entre o texto falado e o original
        val originalText = exercise.displayText
        val similarity = calculateSimilarity(spokenText, originalText)
        
        val feedback = when {
            similarity >= 0.90f -> ShadowingFeedback.EXCELLENT
            similarity >= 0.75f -> ShadowingFeedback.GOOD
            similarity >= 0.50f -> ShadowingFeedback.FAIR
            else -> ShadowingFeedback.NEEDS_WORK
        }
        
        // Atualiza contadores
        totalAttempts++
        scoreSum += similarity
        when (feedback) {
            ShadowingFeedback.EXCELLENT -> excellentCount++
            ShadowingFeedback.GOOD, ShadowingFeedback.FAIR -> goodCount++
            ShadowingFeedback.NEEDS_WORK -> needsWorkCount++
        }
        
        val result = ShadowingResult(
            originalText = originalText,
            userSpokenText = spokenText,
            similarityScore = similarity,
            feedback = feedback,
            pronunciationTips = generatePronunciationTips(spokenText, originalText)
        )
        
        viewModelScope.launch {
            _events.send(ShadowingEvent.ShowResult(feedback))
        }
        
        _uiState.value = state.copy(
            shadowingState = state.shadowingState.copy(
                userAttemptText = spokenText,
                similarityScore = similarity,
                attemptCount = state.shadowingState.attemptCount + 1,
                isRecording = false
            ),
            lastResult = result,
            sessionScore = scoreSum / totalAttempts,
            totalAttempts = totalAttempts
        )
    }
    
    /**
     * Permite tentar novamente o mesmo exercício.
     */
    fun tryAgain() {
        val state = _uiState.value as? ShadowingUiState.Active ?: return
        
        _uiState.value = state.copy(
            shadowingState = ShadowingState(
                currentPhrase = state.shadowingState.currentPhrase,
                attemptCount = state.shadowingState.attemptCount
            ),
            lastResult = null
        )
    }
    
    /**
     * Avança para o próximo exercício.
     */
    fun moveToNext() {
        viewModelScope.launch {
            // Atualiza desafio diário
            dailyChallengeRepository.incrementProgressForType(
                userId = 1L,
                challengeType = ChallengeType.SHADOWING_SESSION
            )
        }
        
        if (currentIndex < exercises.size - 1) {
            currentIndex++
            showCurrentExercise()
        } else {
            _uiState.value = ShadowingUiState.Complete(
                totalExercises = exercises.size,
                averageScore = if (totalAttempts > 0) scoreSum / totalAttempts else 0f,
                excellentCount = excellentCount,
                goodCount = goodCount,
                needsWorkCount = needsWorkCount
            )
        }
    }
    
    private fun showCurrentExercise() {
        val exercise = exercises.getOrNull(currentIndex) ?: return
        
        _uiState.value = ShadowingUiState.Active(
            exercise = exercise,
            currentIndex = currentIndex,
            totalExercises = exercises.size,
            shadowingState = ShadowingState(
                currentPhrase = exercise.displayText
            ),
            sessionScore = if (totalAttempts > 0) scoreSum / totalAttempts else 0f,
            totalAttempts = totalAttempts
        )
        
        // Auto-play do modelo
        viewModelScope.launch {
            _events.send(ShadowingEvent.PlayModelAudio)
        }
    }
    
    /**
     * Calcula similaridade entre dois textos usando Jaccard + Levenshtein.
     */
    private fun calculateSimilarity(spoken: String, original: String): Float {
        val spokenNorm = spoken.lowercase().trim()
        val originalNorm = original.lowercase().trim()
        
        if (spokenNorm == originalNorm) return 1.0f
        if (spokenNorm.isEmpty() || originalNorm.isEmpty()) return 0f
        
        // Jaccard similarity nas palavras
        val spokenWords = spokenNorm.split("\\s+".toRegex()).toSet()
        val originalWords = originalNorm.split("\\s+".toRegex()).toSet()
        val intersection = spokenWords.intersect(originalWords).size
        val union = spokenWords.union(originalWords).size
        val jaccardSim = if (union > 0) intersection.toFloat() / union else 0f
        
        // Levenshtein distance normalizado
        val levenshteinSim = 1f - (levenshteinDistance(spokenNorm, originalNorm).toFloat() /
                maxOf(spokenNorm.length, originalNorm.length))
        
        // Média ponderada
        return (jaccardSim * 0.4f + levenshteinSim * 0.6f).coerceIn(0f, 1f)
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
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[m][n]
    }
    
    /**
     * Gera dicas de pronúncia baseadas nas diferenças.
     */
    private fun generatePronunciationTips(spoken: String, original: String): List<String> {
        val tips = mutableListOf<String>()
        
        val spokenWords = spoken.lowercase().split("\\s+".toRegex())
        val originalWords = original.lowercase().split("\\s+".toRegex())
        
        // Identifica palavras faltantes
        val missing = originalWords.filter { it !in spokenWords }
        if (missing.isNotEmpty() && missing.size <= 3) {
            tips.add("Palavra(s) não detectada(s): ${missing.joinToString(", ")}")
        }
        
        // Dica sobre contrações
        if (original.contains("'") && !spoken.contains("'")) {
            tips.add("Lembre-se de usar contrações: it's, don't, isn't, etc.")
        }
        
        return tips.take(3)
    }
}
