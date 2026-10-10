package com.englishlearning.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório que persiste as preferências de sessão do usuário.
 *
 * Atualmente gerencia o limite de questões por nível por sessão:
 * o usuário pode configurar quantas questões quer que apareçam em cada
 * nível (entre [MIN_QUESTIONS] e [MAX_QUESTIONS]).
 *
 * Armazenado em SharedPreferences com chaves "questions_limit_level_N".
 */
@Singleton
class SessionSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Retorna o limite de questões configurado para o nível [levelId].
     * Se nunca foi configurado, retorna [DEFAULT_QUESTIONS].
     */
    fun getQuestionsLimit(levelId: Int): Int =
        prefs.getInt(keyFor(levelId), DEFAULT_QUESTIONS)

    /**
     * Salva o limite de questões para o nível [levelId].
     * O valor é coerced entre [MIN_QUESTIONS] e [MAX_QUESTIONS].
     */
    fun setQuestionsLimit(levelId: Int, limit: Int) {
        prefs.edit()
            .putInt(keyFor(levelId), limit.coerceIn(MIN_QUESTIONS, MAX_QUESTIONS))
            .apply()
    }

    /**
     * Retorna um mapa de levelId → limite para todos os níveis conhecidos.
     * Útil para carregar o estado inicial da tela de configurações.
     */
    fun getAllLimits(levelIds: List<Int>): Map<Int, Int> =
        levelIds.associateWith { getQuestionsLimit(it) }

    private fun keyFor(levelId: Int) = "questions_limit_level_$levelId"

    // ── Vocabulário ─────────────────────────────────────────────────────────

    fun getVocabLimit(): Int =
        prefs.getInt(KEY_VOCAB_LIMIT, DEFAULT_VOCAB_LIMIT)

    fun setVocabLimit(limit: Int) {
        prefs.edit().putInt(KEY_VOCAB_LIMIT, limit).apply()
    }

    // ── Drilling ─────────────────────────────────────────────────────────

    /** Stage mínimo do Drilling (1–12). */
    fun getDrillingMinStage(): Int =
        prefs.getInt(KEY_DRILLING_MIN_STAGE, DEFAULT_DRILLING_MIN_STAGE)

    fun setDrillingMinStage(stage: Int) {
        prefs.edit()
            .putInt(KEY_DRILLING_MIN_STAGE, stage.coerceIn(MIN_STAGE, MAX_STAGE))
            .apply()
    }

    /** Stage máximo do Drilling (1–12). */
    fun getDrillingMaxStage(): Int =
        prefs.getInt(KEY_DRILLING_MAX_STAGE, DEFAULT_DRILLING_MAX_STAGE)

    fun setDrillingMaxStage(stage: Int) {
        prefs.edit()
            .putInt(KEY_DRILLING_MAX_STAGE, stage.coerceIn(MIN_STAGE, MAX_STAGE))
            .apply()
    }

    /** Número de questões por sessão no Drilling (5–50). */
    fun getDrillingSize(): Int =
        prefs.getInt(KEY_DRILLING_SIZE, DEFAULT_DRILLING_SIZE)

    fun setDrillingSize(size: Int) {
        prefs.edit()
            .putInt(KEY_DRILLING_SIZE, size.coerceIn(MIN_DRILL_SIZE, MAX_DRILL_SIZE))
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "app_prefs"
        const val MIN_QUESTIONS = 5
        const val MAX_QUESTIONS = 50
        const val DEFAULT_QUESTIONS = 10
        const val KEY_VOCAB_LIMIT = "vocab_session_limit"
        const val DEFAULT_VOCAB_LIMIT = 20
        const val MIN_VOCAB = 5
        const val MAX_VOCAB = 100
        // Drilling
        private const val KEY_DRILLING_MIN_STAGE  = "drilling_min_stage"
        private const val KEY_DRILLING_MAX_STAGE  = "drilling_max_stage"
        private const val KEY_DRILLING_SIZE = "drilling_size"
        const val MIN_STAGE                = 1
        const val MAX_STAGE                = 12
        const val DEFAULT_DRILLING_MIN_STAGE = 1
        const val DEFAULT_DRILLING_MAX_STAGE = 12
        const val MIN_DRILL_SIZE           = 5
        const val MAX_DRILL_SIZE           = 50
        const val DEFAULT_DRILLING_SIZE = 20
    }
}
