package com.englishlearning.app.domain.usecase

import com.englishlearning.app.domain.model.*
import com.englishlearning.app.testutil.FakeValidationEngine
import com.englishlearning.app.testutil.makeExercise
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Testes unitários para [ValidateAnswerUseCase].
 *
 * Verifica que o use case delega corretamente ao [QuestionValidationEngine]
 * e retorna o resultado sem modificações.
 *
 * Requisitos: 2.1, 13.1.
 */
class ValidateAnswerUseCaseTest {

    private lateinit var fakeEngine: FakeValidationEngine
    private lateinit var useCase: ValidateAnswerUseCase

    @Before
    fun setUp() {
        fakeEngine = FakeValidationEngine()
        useCase    = ValidateAnswerUseCase(fakeEngine)
    }

    @Test
    fun `delegates to validation engine exactly once`() {
        useCase("What is this?", makeExercise())

        assertEquals(1, fakeEngine.callCount)
    }

    @Test
    fun `passes user question unchanged to engine`() {
        val question = "Where do you live?"
        useCase(question, makeExercise())

        assertEquals(question, fakeEngine.lastUserQuestion)
    }

    @Test
    fun `passes exercise unchanged to engine`() {
        val exercise = makeExercise(id = 42L)
        useCase("some question", exercise)

        assertEquals(42L, fakeEngine.lastExercise?.id)
    }

    @Test
    fun `returns exactly what the engine returns`() {
        val expected = ValidationResult(
            status = ValidationStatus.TOO_CLOSE, score = 70,
            matchedAnswer = null, errorType = ErrorType.INVERTED_WORD_ORDER,
            corrections = emptyList(), hints = emptyList()
        )
        fakeEngine = FakeValidationEngine(fixedResult = expected)
        useCase    = ValidateAnswerUseCase(fakeEngine)

        val result = useCase("some answer", makeExercise())

        assertEquals(expected, result)
    }

    @Test
    fun `does not mutate question or exercise before passing to engine`() {
        val question = "  What is this?  "   // whitespace intentionally present
        val exercise = makeExercise(difficultyScore = 7)

        useCase(question, exercise)

        // Use case must not trim or alter inputs before delegation
        assertEquals(question, fakeEngine.lastUserQuestion)
        assertEquals(7, fakeEngine.lastExercise?.difficultyScore)
    }

    @Test
    fun `handles empty string question gracefully`() {
        val result = useCase("", makeExercise())

        assertNotNull(result)
        assertEquals(1, fakeEngine.callCount)
    }

    @Test
    fun `correct result has score 100 and CORRECT status`() {
        val result = useCase("What is this?", makeExercise())

        assertEquals(ValidationStatus.CORRECT, result.status)
        assertEquals(100, result.score)
    }
}
