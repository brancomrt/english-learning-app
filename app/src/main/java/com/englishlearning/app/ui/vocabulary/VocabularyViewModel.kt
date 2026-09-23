package com.englishlearning.app.ui.vocabulary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.data.database.dao.VocabularyDao
import com.englishlearning.app.data.database.entity.VocabularyCardEntity
import com.englishlearning.app.domain.repository.ContentRepository
import com.englishlearning.app.domain.model.Category
import com.englishlearning.app.domain.model.Level
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VocabularyUiState(
    val isLoading: Boolean = true,
    val levels: List<Level> = emptyList(),
    val categories: List<Category> = emptyList(),
    val filteredCategories: List<Category> = emptyList(),
    val cards: List<VocabularyCardEntity> = emptyList(),
    val selectedLevelId: Int = -1,
    val selectedCategoryId: Int = -1,
    val flippedCardId: Long = -1L
)

@HiltViewModel
class VocabularyViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val vocabularyDao: VocabularyDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(VocabularyUiState())
    val uiState: StateFlow<VocabularyUiState> = _uiState.asStateFlow()

    init {
        loadInitial()
    }

    private fun loadInitial() {
        viewModelScope.launch {
            val levels = contentRepository.getLevels()
            val categories = contentRepository.getCategories()
            val firstLevel = levels.firstOrNull()
            val levelId = firstLevel?.id ?: -1
            val filteredCats = categories.filter { it.levelId == levelId }
            val catId = filteredCats.firstOrNull()?.id ?: -1
            val cards = if (levelId != -1 && catId != -1)
                vocabularyDao.getByLevelAndCategory(levelId, catId)
            else emptyList()
            _uiState.value = VocabularyUiState(
                isLoading = false,
                levels = levels,
                categories = categories,
                filteredCategories = filteredCats,
                cards = cards,
                selectedLevelId = levelId,
                selectedCategoryId = catId
            )
        }
    }

    fun selectLevel(levelId: Int) {
        val filtered = _uiState.value.categories.filter { it.levelId == levelId }
        val catId = filtered.firstOrNull()?.id ?: -1
        _uiState.value = _uiState.value.copy(
            selectedLevelId = levelId,
            filteredCategories = filtered,
            selectedCategoryId = catId,
            flippedCardId = -1L
        )
        loadCards(levelId, catId)
    }

    fun selectCategory(categoryId: Int) {
        _uiState.value = _uiState.value.copy(
            selectedCategoryId = categoryId,
            flippedCardId = -1L
        )
        loadCards(_uiState.value.selectedLevelId, categoryId)
    }

    fun flipCard(cardId: Long) {
        _uiState.value = _uiState.value.copy(
            flippedCardId = if (_uiState.value.flippedCardId == cardId) -1L else cardId
        )
    }

    private fun loadCards(levelId: Int, categoryId: Int) {
        if (levelId == -1 || categoryId == -1) return
        viewModelScope.launch {
            val cards = vocabularyDao.getByLevelAndCategory(levelId, categoryId)
            _uiState.value = _uiState.value.copy(cards = cards)
        }
    }
}
