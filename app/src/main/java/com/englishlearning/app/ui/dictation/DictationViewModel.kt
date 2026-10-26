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
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel para a tela de Ditado.
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
        // Não carrega automaticamente - espera seleção de nível
        _uiState.update { it.copy(isLoading = false, isSelectingLevel = true) }
    }
    
    fun selectLevel(level: Int) {
        _uiState.update { it.copy(selectedLevel = level, isSelectingLevel = false) }
        loadExercises(level)
    }
    
    private fun loadExercises(levelId: Int = _uiState.value.selectedLevel) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val today = LocalDate.now().toString()
                val userId = 1L
                
                // Carrega exercícios devidos + novos do nível selecionado
                val dueExercises = dictationRepository.getDueExercises(userId, today, 15)
                    .filter { it.levelId == levelId }
                val newExercises = dictationRepository.getNewExercises(userId, maxLevel = levelId, 10)
                    .filter { it.levelId == levelId }
                
                exercises = (dueExercises + newExercises).shuffled()
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentExercise = exercises.firstOrNull(),
                        totalExercises = exercises.size
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
    
    fun playAudio() {
        val sentence = _uiState.value.currentExercise?.sentence ?: return
        viewModelScope.launch {
            ttsRepository.speak(sentence)
            _uiState.update { it.copy(playCount = it.playCount + 1) }
        }
    }
    
    fun playAudioSlow() {
        val sentence = _uiState.value.currentExercise?.sentence ?: return
        viewModelScope.launch {
            ttsRepository.speak(sentence, speechRate = 0.7f)
            _uiState.update { it.copy(playCount = it.playCount + 1) }
        }
    }
    
    fun useHint() {
        val exercise = _uiState.value.currentExercise ?: return
        val hintsUsed = _uiState.value.hintsUsed
        
        // Tipos de dicas progressivas
        val hint = when (hintsUsed) {
            0 -> "Palavras: ${exercise.sentence.split(" ").size}"
            1 -> "Primeira letra: ${exercise.sentence.first()}"
            2 -> exercise.translation ?: "Sem tradução disponível"
            else -> return // Máximo de dicas atingido
        }
        
        _uiState.update {
            it.copy(
                hintsUsed = hintsUsed + 1,
                currentHint = hint
            )
        }
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
            
            // Salva progresso no repositório
            dictationRepository.saveResult(1L, exercise.id, result)
            
            _uiState.update {
                it.copy(
                    isChecked = true,
                    currentResult = result
                )
            }
        }
    }
    
    private fun calculateResult(
        exercise: DictationExercise,
        userText: String,
        timeTaken: Long,
        hintsUsed: Int
    ): DictationResult {
        val expectedWords = exercise.sentence.lowercase()
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
            expectedText = exercise.sentence,
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
}

/**
 * Estado da UI de Ditado.
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
    // Filtro por nível
    val availableLevels: List<Int> = (1..12).toList(),
    val selectedLevel: Int = 1,
    val isSelectingLevel: Boolean = true
)
