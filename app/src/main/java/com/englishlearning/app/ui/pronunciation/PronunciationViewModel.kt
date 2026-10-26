package com.englishlearning.app.ui.pronunciation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.domain.model.PronunciationBreakdown
import com.englishlearning.app.domain.model.PronunciationExerciseType
import com.englishlearning.app.domain.model.PronunciationResult
import com.englishlearning.app.domain.model.WordPronunciationAnalysis
import com.englishlearning.app.domain.model.WordStatus
import com.englishlearning.app.domain.repository.PronunciationRepository
import com.englishlearning.app.domain.repository.SpeechRecognitionRepository
import com.englishlearning.app.domain.repository.TtsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para a tela de avaliação de pronúncia.
 */
@HiltViewModel
class PronunciationViewModel @Inject constructor(
    private val pronunciationRepository: PronunciationRepository,
    private val speechRecognitionRepository: SpeechRecognitionRepository,
    private val ttsRepository: TtsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PronunciationUiState())
    val uiState: StateFlow<PronunciationUiState> = _uiState.asStateFlow()
    
    private var sentences = listOf<String>()
    private var currentIndex = 0
    private val results = mutableListOf<PronunciationResult>()
    
    init {
        loadExercises()
    }
    
    private fun loadExercises() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Carrega frases para praticar pronúncia
                sentences = pronunciationRepository.getSentencesForPractice(15)
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentSentence = sentences.firstOrNull(),
                        totalExercises = sentences.size
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message)
                }
            }
        }
    }
    
    fun playReference() {
        val sentence = _uiState.value.currentSentence ?: return
        viewModelScope.launch {
            ttsRepository.speak(sentence)
        }
    }
    
    fun startRecording() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRecording = true) }
            speechRecognitionRepository.startListening()
        }
    }
    
    fun stopRecording() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRecording = false, isAnalyzing = true) }
            
            try {
                val recognizedText = speechRecognitionRepository.stopListening()
                val expectedText = _uiState.value.currentSentence ?: return@launch
                
                // Analisa a pronúncia
                val result = analyzePronunciation(expectedText, recognizedText)
                results.add(result)
                
                // Salva resultado
                pronunciationRepository.saveResult(1L, result)
                
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        currentResult = result
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isAnalyzing = false, error = e.message)
                }
            }
        }
    }
    
    private fun analyzePronunciation(expected: String, recognized: String): PronunciationResult {
        val expectedWords = expected.lowercase()
            .replace(Regex("[^a-z\\s']"), "")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        
        val recognizedWords = recognized.lowercase()
            .replace(Regex("[^a-z\\s']"), "")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        
        val wordAnalysis = mutableListOf<WordPronunciationAnalysis>()
        var correctCount = 0
        var recognizedSet = recognizedWords.toMutableSet()
        
        // Análise palavra por palavra
        for (expected in expectedWords) {
            val status = when {
                expected in recognizedSet -> {
                    recognizedSet.remove(expected)
                    correctCount++
                    WordStatus.CORRECT
                }
                recognizedWords.any { levenshteinDistance(it, expected) <= 2 } -> {
                    WordStatus.MISPRONOUNCED
                }
                else -> WordStatus.OMITTED
            }
            wordAnalysis.add(WordPronunciationAnalysis(expected, status))
        }
        
        // Palavras inseridas (não esperadas)
        recognizedSet.forEach { word ->
            wordAnalysis.add(WordPronunciationAnalysis(word, WordStatus.INSERTED))
        }
        
        // Calcular scores
        val accuracyScore = if (expectedWords.isNotEmpty()) {
            (correctCount * 100 / expectedWords.size)
        } else 0
        
        val completenessScore = if (expectedWords.isNotEmpty()) {
            ((expectedWords.size - wordAnalysis.count { it.status == WordStatus.OMITTED }) * 100 / expectedWords.size)
        } else 0
        
        // Fluência baseada em palavras extras e erros
        val fluencyScore = maxOf(0, 100 - (recognizedSet.size * 10) - 
            (wordAnalysis.count { it.status == WordStatus.MISPRONOUNCED } * 5))
        
        val overallScore = (accuracyScore + completenessScore + fluencyScore) / 3
        
        return PronunciationResult(
            exerciseId = currentIndex.toLong(),
            exerciseType = PronunciationExerciseType.DRILL,
            expectedText = expected,
            recognizedText = recognized,
            overallScore = overallScore,
            breakdown = PronunciationBreakdown(
                accuracyScore = accuracyScore,
                fluencyScore = fluencyScore,
                completenessScore = completenessScore
            ),
            wordAnalysis = wordAnalysis
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
    
    fun retry() {
        _uiState.update { it.copy(currentResult = null) }
    }
    
    fun nextExercise() {
        currentIndex++
        
        if (currentIndex >= sentences.size) {
            _uiState.update {
                it.copy(
                    isComplete = true,
                    averageScore = results.map { r -> r.overallScore }.average().toFloat()
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    currentSentence = sentences[currentIndex],
                    currentIndex = currentIndex,
                    progress = currentIndex.toFloat() / sentences.size,
                    currentResult = null
                )
            }
        }
    }
}

/**
 * Estado da UI de pronúncia.
 */
data class PronunciationUiState(
    val isLoading: Boolean = true,
    val currentSentence: String? = null,
    val currentIndex: Int = 0,
    val totalExercises: Int = 0,
    val progress: Float = 0f,
    val isRecording: Boolean = false,
    val isAnalyzing: Boolean = false,
    val currentResult: PronunciationResult? = null,
    val isComplete: Boolean = false,
    val averageScore: Float = 0f,
    val error: String? = null
)
