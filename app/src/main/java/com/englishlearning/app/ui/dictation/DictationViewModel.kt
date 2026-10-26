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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para a tela de Ditado no estilo Callan Method.
 * 
 * Funcionalidades:
 * - Seleção de Stage (1-12)
 * - Textos completos (parágrafos)
 * - Reprodução de áudio 3x (padrão Callan)
 * - Correção detalhada palavra por palavra
 * - Possibilidade de refazer o ditado
 * - Marca como completo quando 100%
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
    
    init {
        loadAvailableStages()
    }
    
    private fun loadAvailableStages() {
        viewModelScope.launch {
            val stages = dictationRepository.getAvailableStages()
            _uiState.update { 
                it.copy(
                    isLoading = false, 
                    isSelectingStage = true,
                    availableStages = stages
                ) 
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
                // Carrega exercícios do Stage selecionado
                exercises = dictationRepository.getByStage(stage)
                
                // Calcula contagem de completos
                val completedCount = exercises.count { it.completed }
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentExercise = exercises.firstOrNull(),
                        totalExercises = exercises.size,
                        completedInStage = completedCount
                    )
                }
                
                // Auto-play o primeiro áudio
                if (exercises.isNotEmpty()) {
                    playAudio()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message)
                }
            }
        }
    }
    
    fun updateInput(text: String) {
        _uiState.update { it.copy(userInput = text) }
    }
    
    /**
     * Reproduz o áudio do texto.
     * No método Callan, o professor lê 3 vezes.
     */
    fun playAudio() {
        val text = _uiState.value.currentExercise?.text ?: return
        viewModelScope.launch {
            ttsRepository.speak(text)
            _uiState.update { it.copy(playCount = it.playCount + 1) }
        }
    }
    
    /**
     * Reproduz o áudio mais lentamente.
     */
    fun playAudioSlow() {
        val text = _uiState.value.currentExercise?.text ?: return
        viewModelScope.launch {
            ttsRepository.speak(text, speechRate = 0.6f)
            _uiState.update { it.copy(playCount = it.playCount + 1) }
        }
    }
    
    /**
     * Mostra dicas progressivas.
     */
    fun useHint() {
        val exercise = _uiState.value.currentExercise ?: return
        val hintsUsed = _uiState.value.hintsUsed
        
        // Tipos de dicas progressivas
        val hint = when (hintsUsed) {
            0 -> "Palavras: ${exercise.wordCount}"
            1 -> "Primeira letra: ${exercise.text.first()}"
            2 -> exercise.translation ?: "Sem tradução disponível"
            3 -> "Foco gramatical: ${exercise.grammarFocus ?: "N/A"}"
            else -> return // Máximo de dicas atingido
        }
        
        _uiState.update {
            it.copy(
                hintsUsed = hintsUsed + 1,
                currentHint = hint
            )
        }
    }
    
    /**
     * Verifica a resposta do usuário.
     */
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
            
            // Salva progresso no repositório
            // Se score = 100, marca como completo automaticamente
            dictationRepository.saveResult(1L, exercise.id, result)
            
            // Atualiza contagem de completos se necessário
            val completedInStage = if (result.score == 100 && !exercise.completed) {
                _uiState.value.completedInStage + 1
            } else {
                _uiState.value.completedInStage
            }
            
            _uiState.update {
                it.copy(
                    isChecked = true,
                    currentResult = result,
                    completedInStage = completedInStage
                )
            }
        }
    }
    
    /**
     * Refaz o ditado atual (limpa a resposta e permite tentar novamente).
     */
    fun retryExercise() {
        sessionStartTime = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                userInput = "",
                isChecked = false,
                currentResult = null,
                hintsUsed = 0,
                currentHint = null,
                playCount = 0
            )
        }
        // Auto-play áudio ao refazer
        playAudio()
    }
    
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
        
        // Comparação palavra por palavra
        val maxLen = maxOf(expectedWords.size, userWords.size)
        for (i in 0 until maxLen) {
            val expected = expectedWords.getOrNull(i)
            val actual = userWords.getOrNull(i)
            
            when {
                expected == null && actual != null -> {
                    // Palavra extra
                    mistakes.add(DictationMistake(i, "", actual, MistakeType.EXTRA_WORD))
                }
                expected != null && actual == null -> {
                    // Palavra faltando
                    mistakes.add(DictationMistake(i, expected, null, MistakeType.MISSING_WORD))
                }
                expected != null && actual != null && expected != actual -> {
                    // Verificar se é erro de ortografia (similar)
                    val mistakeType = if (levenshteinDistance(expected, actual) <= 2) {
                        MistakeType.SPELLING
                    } else {
                        MistakeType.WRONG_WORD
                    }
                    mistakes.add(DictationMistake(i, expected, actual, mistakeType))
                }
                expected == actual -> {
                    correctCount++
                }
            }
        }
        
        val accuracy = if (expectedWords.isNotEmpty()) {
            correctCount.toFloat() / expectedWords.size
        } else 0f
        
        // Calcular score com penalidades
        var score = (accuracy * 100).toInt()
        score -= hintsUsed * 5 // Penalidade por dica
        score = score.coerceIn(0, 100)
        
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
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        
        for (i in 1..m) {
            for (j in 1..n) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        return dp[m][n]
    }
    
    fun nextExercise() {
        currentIndex++
        sessionStartTime = System.currentTimeMillis()
        
        if (currentIndex >= exercises.size) {
            _uiState.update {
                it.copy(
                    isComplete = true,
                    averageScore = results.map { r -> r.score }.average().toFloat()
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
                    playCount = 0
                )
            }
            // Auto-play próximo áudio
            playAudio()
        }
    }
    
    /**
     * Volta para a seleção de Stage.
     */
    fun backToStageSelection() {
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
}

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
    val playCount: Int = 0,
    val isComplete: Boolean = false,
    val averageScore: Float = 0f,
    val error: String? = null,
    // Filtro por Stage (Callan Method)
    val availableStages: List<Int> = (1..12).toList(),
    val selectedStage: Int = 1,
    val isSelectingStage: Boolean = true,
    // Contagem de completos
    val completedInStage: Int = 0
)
