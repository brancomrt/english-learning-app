package com.englishlearning.app.ui.dailychallenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.domain.model.DailyChallengeSet
import com.englishlearning.app.domain.repository.DailyChallengeRepository
import com.englishlearning.app.domain.usecase.GenerateDailyChallengesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────────────────────────────────────

sealed class DailyChallengeUiState {
    object Loading : DailyChallengeUiState()
    
    data class Loaded(
        val challengeSet: DailyChallengeSet
    ) : DailyChallengeUiState()
    
    data class Error(val message: String) : DailyChallengeUiState()
}

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

/**
 * ViewModel para a tela de Desafios Diários.
 */
@HiltViewModel
class DailyChallengeViewModel @Inject constructor(
    private val generateChallengesUseCase: GenerateDailyChallengesUseCase,
    private val challengeRepository: DailyChallengeRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<DailyChallengeUiState>(DailyChallengeUiState.Loading)
    val uiState: StateFlow<DailyChallengeUiState> = _uiState.asStateFlow()
    
    private val userId = 1L
    
    /**
     * Carrega os desafios do dia.
     */
    fun loadChallenges() {
        viewModelScope.launch {
            _uiState.value = DailyChallengeUiState.Loading
            
            try {
                val challengeSet = generateChallengesUseCase(userId)
                _uiState.value = DailyChallengeUiState.Loaded(challengeSet)
                
                // Observa atualizações em tempo real
                observeChallenges()
            } catch (e: Exception) {
                _uiState.value = DailyChallengeUiState.Error(
                    e.message ?: "Erro ao carregar desafios"
                )
            }
        }
    }
    
    /**
     * Observa atualizações nos desafios.
     */
    private fun observeChallenges() {
        viewModelScope.launch {
            challengeRepository.observeTodayChallenges(userId).collect { challengeSet ->
                if (challengeSet != null) {
                    _uiState.value = DailyChallengeUiState.Loaded(challengeSet)
                }
            }
        }
    }
    
    /**
     * Força atualização dos desafios.
     */
    fun refreshChallenges() {
        loadChallenges()
    }
}
