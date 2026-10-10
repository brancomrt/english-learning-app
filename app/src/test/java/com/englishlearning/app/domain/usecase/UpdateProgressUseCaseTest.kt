package com.englishlearning.app.domain.usecase

import com.englishlearning.app.domain.model.SrsCardType
import com.englishlearning.app.domain.model.ValidationStatus
import com.englishlearning.app.testutil.FakeProgressRepository
import com.englishlearning.app.testutil.FakeSrsRepository
import com.englishlearning.app.testutil.makeAttemptResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Testes unitários para [UpdateProgressUseCase].
 *
 * Valida que:
 * - updateProgress e updateStreak são sempre chamados.
 * - SRS card GRAMMAR é criado apenas para CORRECT e TOO_CLOSE.
 * - INCORRECT e PARTIAL não criam SRS card.
 *
 * Requisitos: 9.3, 5.1, 16.1, 18.1.
 */
class UpdateProgressUseCaseTest {

    private lateinit var fakeProgressRepo: FakeProgressRepository
    private lateinit var fakeSrsRepo: FakeSrsRepository
    private lateinit var useCase: UpdateProgressUseCase

    @Before
    fun setUp() {
        fakeProgressRepo = FakeProgressRepository()
        fakeSrsRepo      = FakeSrsRepository()
        useCase          = UpdateProgressUseCase(fakeProgressRepo, fakeSrsRepo)
    }

    // ── updateProgress e updateStreak sempre chamados ───────────────────────

    @Test
    fun `updateProgress is always called on invoke`() = runTest {
        useCase(userId = 1L, levelId = 1, categoryId = 1,
                attemptResult = makeAttemptResult(status = ValidationStatus.CORRECT))

        assertEquals(1, fakeProgressRepo.updateProgressCallCount)
    }

    @Test
    fun `updateStreak is always called on invoke`() = runTest {
        useCase(userId = 1L, levelId = 1, categoryId = 1,
                attemptResult = makeAttemptResult(status = ValidationStatus.INCORRECT))

        assertEquals(1, fakeProgressRepo.updateStreakCallCount)
    }

    @Test
    fun `updateProgress receives the correct userId`() = runTest {
        useCase(userId = 42L, levelId = 1, categoryId = 1,
                attemptResult = makeAttemptResult())

        assertEquals(42L, fakeProgressRepo.lastUserId)
    }

    @Test
    fun `updateProgress receives the correct attemptResult`() = runTest {
        val attempt = makeAttemptResult(exerciseId = 99L, xpEarned = 150)

        useCase(userId = 1L, levelId = 1, categoryId = 1, attemptResult = attempt)

        assertEquals(99L, fakeProgressRepo.lastAttemptResult?.exerciseId)
        assertEquals(150, fakeProgressRepo.lastAttemptResult?.xpEarned)
    }

    // ── SRS card criado apenas para CORRECT e TOO_CLOSE ────────────────────

    @Test
    fun `CORRECT status creates SRS card of type GRAMMAR`() = runTest {
        useCase(userId = 1L, levelId = 1, categoryId = 1,
                attemptResult = makeAttemptResult(status = ValidationStatus.CORRECT))

        assertEquals(1, fakeSrsRepo.cardsCreated.size)
        assertEquals(SrsCardType.GRAMMAR, fakeSrsRepo.cardsCreated[0].cardType)
    }

    @Test
    fun `TOO_CLOSE status creates SRS card`() = runTest {
        useCase(userId = 1L, levelId = 1, categoryId = 1,
                attemptResult = makeAttemptResult(status = ValidationStatus.TOO_CLOSE))

        assertEquals(1, fakeSrsRepo.cardsCreated.size)
    }

    @Test
    fun `INCORRECT status does not create SRS card`() = runTest {
        useCase(userId = 1L, levelId = 1, categoryId = 1,
                attemptResult = makeAttemptResult(status = ValidationStatus.INCORRECT))

        assertEquals(0, fakeSrsRepo.cardsCreated.size)
    }

    @Test
    fun `PARTIAL status does not create SRS card`() = runTest {
        useCase(userId = 1L, levelId = 1, categoryId = 1,
                attemptResult = makeAttemptResult(status = ValidationStatus.PARTIAL))

        assertEquals(0, fakeSrsRepo.cardsCreated.size)
    }

    // ── SRS card usa exerciseId correto ──────────────────────────────────────

    @Test
    fun `SRS card refId matches exerciseId from attemptResult`() = runTest {
        useCase(userId = 5L, levelId = 1, categoryId = 1,
                attemptResult = makeAttemptResult(exerciseId = 77L,
                    status = ValidationStatus.CORRECT))

        assertEquals(77L, fakeSrsRepo.cardsCreated[0].refId)
        assertEquals(5L, fakeSrsRepo.cardsCreated[0].userId)
    }

    // ── Chamadas múltiplas ───────────────────────────────────────────────────

    @Test
    fun `two CORRECT attempts create two SRS cards`() = runTest {
        repeat(2) {
            useCase(userId = 1L, levelId = 1, categoryId = 1,
                    attemptResult = makeAttemptResult(status = ValidationStatus.CORRECT))
        }

        assertEquals(2, fakeSrsRepo.cardsCreated.size)
        assertEquals(2, fakeProgressRepo.updateProgressCallCount)
        assertEquals(2, fakeProgressRepo.updateStreakCallCount)
    }
}
