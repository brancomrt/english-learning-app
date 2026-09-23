package com.englishlearning.app.ui.lesson

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.englishlearning.app.domain.lesson.LessonContent
import com.englishlearning.app.domain.lesson.LessonContentProvider
import com.englishlearning.app.domain.model.QuestionType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Estado da tela de MiniLesson.
 *
 * @property lessonContent  Conteúdo da lição carregado para o QuestionType selecionado.
 * @property categoryId     Identificador da categoria selecionada.
 * @property questionType   Tipo de pergunta da categoria selecionada.
 */
data class LessonUiState(
    val lessonContent: LessonContent? = null,
    val categoryId: Int = 0,
    val questionType: QuestionType? = null
)

/**
 * ViewModel da tela de MiniLesson (LessonScreen).
 *
 * Responsabilidades:
 * - Verificar se a MiniLesson de uma categoria já foi vista via SharedPreferences (Req. 15.1).
 * - Registrar a lição como vista no SharedPreferences (Req. 15.7).
 * - Fornecer o LessonContent correspondente ao QuestionType da categoria (Req. 15.2, 15.9, 15.10).
 * - Expor o estado da tela como StateFlow<LessonUiState> (Req. 15.4).
 *
 * A chave no SharedPreferences segue o padrão `lesson_seen_{categoryId}` (Req. 15.7).
 *
 * @param prefsProvider  Lambda que retorna o [SharedPreferences] a usar.
 *                       Defaults to `context.getSharedPreferences("app_prefs", MODE_PRIVATE)`.
 *                       The `internal` visibility allows unit tests to supply a fake in-memory
 *                       instance without requiring Robolectric or instrumentation.
 */
@HiltViewModel
open class LessonViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    /**
     * Lazily-resolved SharedPreferences. Internal so tests can create an instance using
     * the internal secondary constructor that overrides this provider.
     */
    internal var prefsProvider: () -> SharedPreferences = {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    private val prefs: SharedPreferences
        get() = prefsProvider()

    private val _uiState = MutableStateFlow(LessonUiState())
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()

    // -------------------------------------------------------------------------
    // SharedPreferences helpers — Requisitos 15.1, 15.7
    // -------------------------------------------------------------------------

    /**
     * Retorna `true` se a MiniLesson desta categoria ainda **não** foi vista,
     * ou seja, o usuário deve ser direcionado para a LessonScreen.
     *
     * Requisito 15.1: verificar se a lição já foi visualizada.
     * Requisito 15.2: se não foi vista, exibir a LessonScreen.
     * Requisito 15.3: se já foi vista, iniciar a sessão diretamente.
     */
    fun shouldShowLesson(categoryId: Int): Boolean {
        return !prefs.getBoolean("lesson_seen_$categoryId", false)
    }

    /**
     * Persiste `lesson_seen_{categoryId} = true` no SharedPreferences.
     * Deve ser chamado quando o usuário pressiona "Começar" ou "Pular".
     *
     * Requisito 15.5 / 15.6 / 15.7: registrar lição como vista.
     */
    fun markLessonSeen(categoryId: Int) {
        prefs.edit().putBoolean("lesson_seen_$categoryId", true).apply()
    }

    // -------------------------------------------------------------------------
    // Lesson content — Requisitos 15.4, 15.9, 15.10
    // -------------------------------------------------------------------------

    /**
     * Retorna o [LessonContent] correspondente ao [QuestionType] informado,
     * ou `null` se o tipo não possuir mapeamento (Req. 15.12).
     */
    fun getLessonContent(questionType: QuestionType): LessonContent? {
        return LessonContentProvider.forQuestionType(questionType)
    }

    // -------------------------------------------------------------------------
    // UI state — Requisito 13.2 (StateFlow pattern)
    // -------------------------------------------------------------------------

    /**
     * Carrega o estado da tela para a categoria e tipo de pergunta selecionados.
     * Atualiza o [uiState] com o [LessonContent] correspondente ao [questionType].
     */
    fun loadLesson(categoryId: Int, questionType: QuestionType) {
        _uiState.update { state ->
            state.copy(
                categoryId = categoryId,
                questionType = questionType,
                lessonContent = LessonContentProvider.forQuestionType(questionType)
            )
        }
    }
}
