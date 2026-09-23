package com.englishlearning.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.data.repository.SessionSettingsRepository
import com.englishlearning.app.data.repository.SessionSettingsRepository.Companion.DEFAULT_QUESTIONS
import com.englishlearning.app.data.repository.SessionSettingsRepository.Companion.MAX_QUESTIONS
import com.englishlearning.app.data.repository.SessionSettingsRepository.Companion.MIN_QUESTIONS
import com.englishlearning.app.domain.repository.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LevelQuestionSetting(
    val levelId: Int,
    val levelName: String,
    val currentLimit: Int,
    val isUnlocked: Boolean,
    val minLimit: Int = MIN_QUESTIONS,
    val maxLimit: Int = MAX_QUESTIONS
)

data class SettingsUiState(
    val isLoading: Boolean = true,
    val levelSettings: List<LevelQuestionSetting> = emptyList(),
    /** ID do nível para o qual o diálogo de confirmação de desbloqueio está aberto. */
    val confirmUnlockLevelId: Int? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val settingsRepository: SessionSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val levels = contentRepository.getLevels()
            val settings = levels.map { level ->
                LevelQuestionSetting(
                    levelId = level.id,
                    levelName = levelDisplayName(level.nameKey),
                    currentLimit = settingsRepository.getQuestionsLimit(level.id),
                    isUnlocked = level.isUnlocked
                )
            }
            _uiState.value = SettingsUiState(isLoading = false, levelSettings = settings)
        }
    }

    fun updateLimit(levelId: Int, newLimit: Int) {
        settingsRepository.setQuestionsLimit(levelId, newLimit)
        _uiState.value = _uiState.value.copy(
            levelSettings = _uiState.value.levelSettings.map { s ->
                if (s.levelId == levelId) s.copy(currentLimit = newLimit) else s
            }
        )
    }

    fun resetAllToDefault() {
        _uiState.value.levelSettings.forEach { s ->
            settingsRepository.setQuestionsLimit(s.levelId, DEFAULT_QUESTIONS)
        }
        _uiState.value = _uiState.value.copy(
            levelSettings = _uiState.value.levelSettings.map { it.copy(currentLimit = DEFAULT_QUESTIONS) }
        )
    }

    /** Abre o diálogo de confirmação para desbloquear [levelId]. */
    fun requestUnlockLevel(levelId: Int) {
        _uiState.value = _uiState.value.copy(confirmUnlockLevelId = levelId)
    }

    /** Cancela o diálogo de confirmação sem desbloquear. */
    fun cancelUnlock() {
        _uiState.value = _uiState.value.copy(confirmUnlockLevelId = null)
    }

    /** Confirma e executa o desbloqueio do nível. */
    fun confirmUnlockLevel() {
        val levelId = _uiState.value.confirmUnlockLevelId ?: return
        viewModelScope.launch {
            contentRepository.unlockLevel(levelId)
            _uiState.value = _uiState.value.copy(
                confirmUnlockLevelId = null,
                levelSettings = _uiState.value.levelSettings.map { s ->
                    if (s.levelId == levelId) s.copy(isUnlocked = true) else s
                }
            )
        }
    }

    private fun levelDisplayName(nameKey: String): String = when (nameKey) {
        "level_beginner"           -> "Beginner (A1)"
        "level_elementary"         -> "Elementary (A2)"
        "level_pre_intermediate"   -> "Pre-Intermediate (A2+)"
        "level_intermediate"       -> "Intermediate (B1)"
        "level_upper_intermediate" -> "Upper-Intermediate (B2)"
        "level_advanced"           -> "Advanced (C1)"
        else -> nameKey.replace("level_", "").replaceFirstChar { it.uppercase() }
    }
}
