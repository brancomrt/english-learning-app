package com.englishlearning.app.data.repository

import com.englishlearning.app.testutil.FakeContext
import com.englishlearning.app.testutil.FakeSharedPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Testes unitários para [SessionSettingsRepository].
 *
 * Cobre todas as configurações: exercícios por nível, vocabulário e drilling.
 * Usa [FakeSharedPreferences] para evitar dependência do Android runtime.
 */
class SessionSettingsRepositoryTest {

    private lateinit var prefs: FakeSharedPreferences
    private lateinit var repo: SessionSettingsRepository

    @Before
    fun setUp() {
        prefs = FakeSharedPreferences()
        repo  = SessionSettingsRepository(FakeContext(prefs))
    }

    // ── Exercícios por nível ─────────────────────────────────────────────────

    @Test
    fun `getQuestionsLimit returns DEFAULT_QUESTIONS when never set`() {
        val result = repo.getQuestionsLimit(levelId = 1)
        assertEquals(SessionSettingsRepository.DEFAULT_QUESTIONS, result)
    }

    @Test
    fun `setQuestionsLimit stores value and getQuestionsLimit retrieves it`() {
        repo.setQuestionsLimit(levelId = 1, limit = 25)
        assertEquals(25, repo.getQuestionsLimit(levelId = 1))
    }

    @Test
    fun `setQuestionsLimit clamps value below MIN to MIN_QUESTIONS`() {
        repo.setQuestionsLimit(levelId = 1, limit = 0)
        assertEquals(SessionSettingsRepository.MIN_QUESTIONS, repo.getQuestionsLimit(1))
    }

    @Test
    fun `setQuestionsLimit clamps value above MAX to MAX_QUESTIONS`() {
        repo.setQuestionsLimit(levelId = 1, limit = 999)
        assertEquals(SessionSettingsRepository.MAX_QUESTIONS, repo.getQuestionsLimit(1))
    }

    @Test
    fun `different levelIds have independent limits`() {
        repo.setQuestionsLimit(levelId = 1, limit = 10)
        repo.setQuestionsLimit(levelId = 2, limit = 20)
        repo.setQuestionsLimit(levelId = 3, limit = 30)

        assertEquals(10, repo.getQuestionsLimit(1))
        assertEquals(20, repo.getQuestionsLimit(2))
        assertEquals(30, repo.getQuestionsLimit(3))
    }

    @Test
    fun `getAllLimits returns map with correct values for each levelId`() {
        repo.setQuestionsLimit(1, 8)
        repo.setQuestionsLimit(2, 15)

        val limits = repo.getAllLimits(listOf(1, 2, 3))

        assertEquals(8, limits[1])
        assertEquals(15, limits[2])
        assertEquals(SessionSettingsRepository.DEFAULT_QUESTIONS, limits[3])
    }

    // ── Vocabulário ──────────────────────────────────────────────────────────

    @Test
    fun `getVocabLimit returns DEFAULT_VOCAB_LIMIT when never set`() {
        assertEquals(SessionSettingsRepository.DEFAULT_VOCAB_LIMIT, repo.getVocabLimit())
    }

    @Test
    fun `setVocabLimit stores and getVocabLimit retrieves it`() {
        repo.setVocabLimit(50)
        assertEquals(50, repo.getVocabLimit())
    }

    @Test
    fun `setVocabLimit can be overwritten`() {
        repo.setVocabLimit(30)
        repo.setVocabLimit(75)
        assertEquals(75, repo.getVocabLimit())
    }

    // ── Drilling — stage mínimo ──────────────────────────────────────────────

    @Test
    fun `getDrillingMinStage returns DEFAULT_DRILLING_MIN_STAGE when never set`() {
        assertEquals(
            SessionSettingsRepository.DEFAULT_DRILLING_MIN_STAGE,
            repo.getDrillingMinStage()
        )
    }

    @Test
    fun `setDrillingMinStage stores value correctly`() {
        repo.setDrillingMinStage(3)
        assertEquals(3, repo.getDrillingMinStage())
    }

    @Test
    fun `setDrillingMinStage clamps below MIN_STAGE to MIN_STAGE`() {
        repo.setDrillingMinStage(0)
        assertEquals(SessionSettingsRepository.MIN_STAGE, repo.getDrillingMinStage())
    }

    @Test
    fun `setDrillingMinStage clamps above MAX_STAGE to MAX_STAGE`() {
        repo.setDrillingMinStage(999)
        assertEquals(SessionSettingsRepository.MAX_STAGE, repo.getDrillingMinStage())
    }

    // ── Drilling — stage máximo ──────────────────────────────────────────────

    @Test
    fun `getDrillingMaxStage returns DEFAULT_DRILLING_MAX_STAGE when never set`() {
        assertEquals(
            SessionSettingsRepository.DEFAULT_DRILLING_MAX_STAGE,
            repo.getDrillingMaxStage()
        )
    }

    @Test
    fun `setDrillingMaxStage stores value correctly`() {
        repo.setDrillingMaxStage(8)
        assertEquals(8, repo.getDrillingMaxStage())
    }

    @Test
    fun `setDrillingMaxStage clamps below MIN_STAGE to MIN_STAGE`() {
        repo.setDrillingMaxStage(-5)
        assertEquals(SessionSettingsRepository.MIN_STAGE, repo.getDrillingMaxStage())
    }

    @Test
    fun `setDrillingMaxStage clamps above MAX_STAGE to MAX_STAGE`() {
        repo.setDrillingMaxStage(100)
        assertEquals(SessionSettingsRepository.MAX_STAGE, repo.getDrillingMaxStage())
    }

    @Test
    fun `min and max stage can be set independently`() {
        repo.setDrillingMinStage(2)
        repo.setDrillingMaxStage(9)

        assertEquals(2, repo.getDrillingMinStage())
        assertEquals(9, repo.getDrillingMaxStage())
    }

    // ── Drilling — tamanho da sessão ─────────────────────────────────────────

    @Test
    fun `getDrillingSize returns DEFAULT_DRILLING_SIZE when never set`() {
        assertEquals(
            SessionSettingsRepository.DEFAULT_DRILLING_SIZE,
            repo.getDrillingSize()
        )
    }

    @Test
    fun `setDrillingSize stores value correctly`() {
        repo.setDrillingSize(30)
        assertEquals(30, repo.getDrillingSize())
    }

    @Test
    fun `setDrillingSize clamps below MIN_DRILL_SIZE to MIN_DRILL_SIZE`() {
        repo.setDrillingSize(0)
        assertEquals(SessionSettingsRepository.MIN_DRILL_SIZE, repo.getDrillingSize())
    }

    @Test
    fun `setDrillingSize clamps above MAX_DRILL_SIZE to MAX_DRILL_SIZE`() {
        repo.setDrillingSize(9999)
        assertEquals(SessionSettingsRepository.MAX_DRILL_SIZE, repo.getDrillingSize())
    }

    // ── Constants ────────────────────────────────────────────────────────────

    @Test
    fun `MIN_STAGE is 1 and MAX_STAGE is 12`() {
        assertEquals(1,  SessionSettingsRepository.MIN_STAGE)
        assertEquals(12, SessionSettingsRepository.MAX_STAGE)
    }

    @Test
    fun `default stage range covers all 12 stages`() {
        assertEquals(1,  SessionSettingsRepository.DEFAULT_DRILLING_MIN_STAGE)
        assertEquals(12, SessionSettingsRepository.DEFAULT_DRILLING_MAX_STAGE)
    }

    @Test
    fun `default drilling size is 20`() {
        assertEquals(20, SessionSettingsRepository.DEFAULT_DRILLING_SIZE)
    }

    @Test
    fun `MIN_DRILL_SIZE is 5 and MAX_DRILL_SIZE is 50`() {
        assertEquals(5,  SessionSettingsRepository.MIN_DRILL_SIZE)
        assertEquals(50, SessionSettingsRepository.MAX_DRILL_SIZE)
    }

    @Test
    fun `DEFAULT_QUESTIONS is 10`() {
        assertEquals(10, SessionSettingsRepository.DEFAULT_QUESTIONS)
    }
}
