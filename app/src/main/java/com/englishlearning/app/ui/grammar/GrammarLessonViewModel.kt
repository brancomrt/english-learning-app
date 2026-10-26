package com.englishlearning.app.ui.grammar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.domain.model.GrammarLesson
import com.englishlearning.app.domain.repository.GrammarRepository
import com.englishlearning.app.domain.repository.TtsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para a tela de lições de gramática.
 */
@HiltViewModel
class GrammarLessonViewModel @Inject constructor(
    private val grammarRepository: GrammarRepository,
    private val ttsRepository: TtsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GrammarLessonUiState())
    val uiState: StateFlow<GrammarLessonUiState> = _uiState.asStateFlow()
    
    init {
        loadLessons()
    }
    
    private fun loadLessons() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val userId = 1L
                // Busca lições de todos os níveis com progresso
                val allLessons = grammarRepository.getLessonsWithProgress(userId)
                
                // Agrupa por nível para facilitar filtro
                val lessonsByLevel = allLessons.groupBy { it.levelId }
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        lessons = allLessons,
                        lessonsByLevel = lessonsByLevel,
                        availableLevels = lessonsByLevel.keys.sorted()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message)
                }
            }
        }
    }
    
    fun filterByLevel(levelId: Int?) {
        _uiState.update { state ->
            val filtered = if (levelId == null) {
                state.lessonsByLevel.values.flatten()
            } else {
                state.lessonsByLevel[levelId] ?: emptyList()
            }
            state.copy(
                selectedLevel = levelId,
                filteredLessons = filtered
            )
        }
    }
    
    fun selectLesson(lesson: GrammarLesson) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Carrega exemplos da lição
                val fullLesson = grammarRepository.getLessonWithExamples(lesson.id)
                
                // Marca como visualizada
                grammarRepository.markAsViewed(1L, lesson.id)
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentLesson = fullLesson,
                        currentExampleIndex = 0
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message)
                }
            }
        }
    }
    
    fun viewExample(index: Int) {
        _uiState.update {
            it.copy(currentExampleIndex = maxOf(it.currentExampleIndex, index + 1))
        }
    }
    
    fun playAudio(text: String) {
        viewModelScope.launch {
            ttsRepository.speak(text)
        }
    }
    
    fun completeLesson() {
        val lessonId = _uiState.value.currentLesson?.id ?: return
        
        viewModelScope.launch {
            try {
                grammarRepository.markAsCompleted(1L, lessonId)
                
                // Volta para lista e atualiza
                _uiState.update { it.copy(currentLesson = null) }
                loadLessons()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    fun goBack() {
        _uiState.update { it.copy(currentLesson = null) }
    }
}

/**
 * Estado da UI de lições de gramática.
 */
data class GrammarLessonUiState(
    val isLoading: Boolean = true,
    val lessons: List<GrammarLesson> = emptyList(),
    val lessonsByLevel: Map<Int, List<GrammarLesson>> = emptyMap(),
    val filteredLessons: List<GrammarLesson> = emptyList(),
    val availableLevels: List<Int> = emptyList(),
    val selectedLevel: Int? = null,
    val currentLesson: GrammarLesson? = null,
    val currentExampleIndex: Int = 0,
    val error: String? = null
)
