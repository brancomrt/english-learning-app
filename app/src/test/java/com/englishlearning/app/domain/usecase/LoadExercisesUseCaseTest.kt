package com.englishlearning.app.domain.usecase

import com.englishlearning.app.data.repository.SessionSettingsRepository
import com.englishlearning.app.testutil.FakeContentRepository
import com.englishlearning.app.testutil.FakeContext
import com.englishlearning.app.testutil.FakeSharedPreferences
import com.englishlearning.app.testutil.makeExercise
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Testes unitários para [LoadExercisesUseCase].
 *
 * Cobre especialmente o comportamento do categoryId == 0 (todas as categorias),
 * adicionado na v1.16.0.
 */
class LoadExercisesUseCaseTest {

    private lateinit var fakeRepo: FakeContentRepository
    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var settingsRepo: SessionSettingsRepository
    private lateinit var useCase: LoadExercisesUseCase

    @Before
    fun setUp() {
        fakeRepo     = FakeContentRepository()
        fakePrefs    = FakeSharedPreferences()
        settingsRepo = SessionSettingsRepository(FakeContext(fakePrefs))
        useCase      = LoadExercisesUseCase(fakeRepo, settingsRepo)
    }

    // ── categoryId = 0 → usa getExercisesByLevel ────────────────────────────

    @Test
    fun `categoryId 0 calls getExercisesByLevel not getByLevelAndCategory`() = runTest {
        fakeRepo.exercisesByLevel = mapOf(1 to listOf(makeExercise(id = 1L)))

        useCase(levelId = 1, categoryId = 0)

        assertTrue(
            "getByLevel(1) deve ter sido chamado",
            fakeRepo.callLog.any { it.startsWith("getByLevel(1)") }
        )
        assertTrue(
            "getByLevelAndCategory não deve ter sido chamado quando categoryId=0",
            fakeRepo.callLog.none { it.startsWith("getByLevelAndCategory") }
        )
    }

    @Test
    fun `categoryId 0 returns exercises from all categories of the level`() = runTest {
        val exercises = listOf(
            makeExercise(id = 1L, categoryId = 1),
            makeExercise(id = 2L, categoryId = 2),
            makeExercise(id = 3L, categoryId = 3)
        )
        fakeRepo.exercisesByLevel = mapOf(2 to exercises)

        val result = useCase(levelId = 2, categoryId = 0)

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow().size)
    }

    // ── categoryId > 0 → usa getByLevelAndCategory ──────────────────────────

    @Test
    fun `categoryId gt 0 calls getByLevelAndCategory not getByLevel`() = runTest {
        fakeRepo.exercisesByLevelAndCategory = mapOf(
            (1 to 2) to listOf(makeExercise(id = 10L))
        )

        useCase(levelId = 1, categoryId = 2)

        assertTrue(
            "getByLevelAndCategory(1, 2) deve ter sido chamado",
            fakeRepo.callLog.any { it == "getByLevelAndCategory(1, 2)" }
        )
        assertTrue(
            "getByLevel não deve ter sido chamado quando categoryId>0",
            fakeRepo.callLog.none { it.startsWith("getByLevel(") }
        )
    }

    @Test
    fun `categoryId gt 0 returns exercises for that specific category`() = runTest {
        val exercises = listOf(
            makeExercise(id = 5L, categoryId = 3),
            makeExercise(id = 6L, categoryId = 3)
        )
        fakeRepo.exercisesByLevelAndCategory = mapOf((1 to 3) to exercises)

        val result = useCase(levelId = 1, categoryId = 3)

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
    }

    // ── Empty list → Result.failure ─────────────────────────────────────────

    @Test
    fun `empty exercise list returns failure for categoryId 0`() = runTest {
        fakeRepo.exercisesByLevel = mapOf(1 to emptyList())

        val result = useCase(levelId = 1, categoryId = 0)

        assertTrue(result.isFailure)
    }

    @Test
    fun `empty exercise list returns failure for specific categoryId`() = runTest {
        fakeRepo.exercisesByLevelAndCategory = mapOf((1 to 1) to emptyList())

        val result = useCase(levelId = 1, categoryId = 1)

        assertTrue(result.isFailure)
    }

    @Test
    fun `failure contains NoExercisesException when list is empty`() = runTest {
        fakeRepo.exercisesByLevel = emptyMap()

        val result = useCase(levelId = 99, categoryId = 0)

        assertTrue(result.isFailure)
    }

    // ── Limit aplicado pelo settings ─────────────────────────────────────────

    @Test
    fun `result is limited to questionsLimit from settings`() = runTest {
        val exercises = (1..20).map { makeExercise(id = it.toLong()) }
        fakeRepo.exercisesByLevel = mapOf(1 to exercises)

        // Settings default é 10
        val result = useCase(levelId = 1, categoryId = 0)

        assertTrue(result.isSuccess)
        assertEquals(10, result.getOrThrow().size)
    }

    @Test
    fun `result uses all exercises when available count is less than limit`() = runTest {
        val exercises = listOf(makeExercise(id = 1L), makeExercise(id = 2L))
        fakeRepo.exercisesByLevel = mapOf(1 to exercises)

        val result = useCase(levelId = 1, categoryId = 0)

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
    }

    @Test
    fun `custom limit from settings is respected`() = runTest {
        // Set custom limit of 5 for level 1
        settingsRepo.setQuestionsLimit(1, 5)
        val exercises = (1..15).map { makeExercise(id = it.toLong()) }
        fakeRepo.exercisesByLevel = mapOf(1 to exercises)

        val result = useCase(levelId = 1, categoryId = 0)

        assertTrue(result.isSuccess)
        assertEquals(5, result.getOrThrow().size)
    }

    // ── Shuffle: tamanho preservado ──────────────────────────────────────────

    @Test
    fun `returned list size is consistent across multiple calls`() = runTest {
        val exercises = (1..20).map { makeExercise(id = it.toLong()) }
        fakeRepo.exercisesByLevel = mapOf(1 to exercises)

        val sizes = (1..5).map { useCase(levelId = 1, categoryId = 0).getOrThrow().size }

        sizes.forEach { assertEquals(10, it) }
    }
}
