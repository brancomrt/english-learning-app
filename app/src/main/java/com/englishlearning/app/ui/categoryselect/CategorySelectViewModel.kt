package com.englishlearning.app.ui.categoryselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.domain.model.Category
import com.englishlearning.app.domain.repository.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado da CategorySelectScreen.
 *
 * Requisito 7.2: exibir categorias disponíveis para o nível selecionado.
 *
 * @property isLoading    Indica carregamento das categorias.
 * @property categories   Lista de categorias filtradas para o nível selecionado.
 * @property levelId      ID do nível cujas categorias estão sendo exibidas.
 * @property error        Mensagem de erro quando o carregamento falha (Req. 12.7).
 */
data class CategorySelectUiState(
    val isLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val levelId: Int = 0,
    val error: String? = null
)

/**
 * ViewModel da CategorySelectScreen.
 *
 * Carrega as categorias disponíveis para um determinado nível via [ContentRepository]
 * e as expõe como [StateFlow] de [CategorySelectUiState].
 *
 * Requisito 7.1: exercícios organizados em categorias temáticas dentro de cada nível.
 * Requisito 7.2: exibir as categorias disponíveis naquele nível na CategorySelectScreen.
 *
 * @param contentRepository Repositório para buscar categorias por nível.
 */
@HiltViewModel
class CategorySelectViewModel @Inject constructor(
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategorySelectUiState())
    val uiState: StateFlow<CategorySelectUiState> = _uiState.asStateFlow()

    /**
     * Carrega e filtra as categorias para o nível fornecido.
     *
     * Busca todas as categorias via [ContentRepository.getCategories] e filtra
     * aquelas cujo [Category.levelId] corresponde ao [levelId] informado.
     *
     * Precondição: [levelId] é um ID de nível válido no banco de dados.
     * Postcondição: [uiState] transiciona para isLoading=false com a lista de
     *               categorias do nível ou com uma mensagem de erro.
     *
     * Requisito 7.2: exibir categorias disponíveis para o nível selecionado.
     *
     * @param levelId ID do nível selecionado na LevelSelectScreen.
     */
    fun loadCategoriesForLevel(levelId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, levelId = levelId) }
            runCatching {
                contentRepository.getCategories()
                    .filter { it.levelId == levelId }
                    .sortedBy { it.sortOrder }
            }.onSuccess { categories ->
                _uiState.update {
                    it.copy(isLoading = false, categories = categories)
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "Erro ao carregar categorias"
                    )
                }
            }
        }
    }

    /**
     * Reinicia o carregamento das categorias após falha.
     *
     * Requisito 12.7: opção de retry quando o estado de erro é exibido.
     */
    fun retry() {
        loadCategoriesForLevel(_uiState.value.levelId)
    }
}
