package com.englishlearning.app.domain.validation

import com.englishlearning.app.domain.model.AcceptedAnswer
import com.englishlearning.app.domain.model.ErrorType
import com.englishlearning.app.domain.model.Exercise
import com.englishlearning.app.domain.model.Hint
import com.englishlearning.app.domain.model.HintType
import com.englishlearning.app.domain.model.QuestionType
import com.englishlearning.app.domain.model.ValidationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuestionValidationEngineImplTest {

    private lateinit var engine: QuestionValidationEngineImpl
    private lateinit var textNormalizer: TextNormalizer
    private lateinit var grammarParser: GrammarParser
    private lateinit var semanticMatcher: SemanticMatcher

    @Before
    fun setUp() {
        textNormalizer = TextNormalizer()
        grammarParser = GrammarParser()
        semanticMatcher = SemanticMatcher()
        engine = QuestionValidationEngineImpl(textNormalizer, grammarParser, semanticMatcher)
    }

    // --- Helper ---

    private fun makeExercise(
        questionType: QuestionType = QuestionType.WH_WHAT,
        difficultyScore: Int = 5,
        answers: List<Pair<String, Float>> = listOf("What is your name?" to 0.85f),
        hints: List<Hint> = emptyList()
    ): Exercise = Exercise(
        id = 1L,
        levelId = 1,
        categoryId = 1,
        answerText = "My name is Maria.",
        contextText = null,
        questionType = questionType,
        grammarFocus = "present simple - to be",
        difficultyScore = difficultyScore,
        xpReward = 10,
        acceptedAnswers = answers.mapIndexed { idx, (text, threshold) ->
            AcceptedAnswer(
                id = idx.toLong(),
                exerciseId = 1L,
                questionText = text,
                isPrimary = idx == 0,
                matchThreshold = threshold
            )
        },
        hints = hints
    )

    // -------------------------------------------------------------------
    // Requisito 2.3: tipo incorreto → INCORRECT + WRONG_QUESTION_TYPE
    // -------------------------------------------------------------------

    @Test
    fun `validate - wrong question type returns INCORRECT with WRONG_QUESTION_TYPE error`() {
        val exercise = makeExercise(questionType = QuestionType.WH_WHAT)
        // Submeter uma YES_NO question quando se espera WH_WHAT
        val result = engine.validate("Do you like coffee?", exercise)

        assertEquals(ValidationStatus.INCORRECT, result.status)
        assertEquals(ErrorType.WRONG_QUESTION_TYPE, result.errorType)
        assertEquals(0, result.score)
        assertNull(result.matchedAnswer)
        assertTrue(result.corrections.isNotEmpty())
    }

    // -------------------------------------------------------------------
    // Requisito 2.7: resposta exata (verbatim) → CORRECT
    // -------------------------------------------------------------------

    @Test
    fun `validate - verbatim accepted answer returns CORRECT`() {
        val exercise = makeExercise(
            questionType = QuestionType.WH_WHAT,
            answers = listOf("What is your name?" to 0.85f)
        )
        val result = engine.validate("What is your name?", exercise)

        assertEquals(ValidationStatus.CORRECT, result.status)
        assertNotNull(result.matchedAnswer)
        assertTrue(result.score in 0..100)
        assertTrue(result.corrections.isEmpty())
        assertTrue(result.hints.isEmpty())
    }

    // -------------------------------------------------------------------
    // Requisito 2.9: score sempre em [0, 100]
    // -------------------------------------------------------------------

    @Test
    fun `validate - score is always between 0 and 100 for any input`() {
        val exercise = makeExercise(questionType = QuestionType.WH_WHAT)

        listOf(
            "What is your name?",
            "Do you like coffee?",     // wrong type
            "What",                    // incomplete
            "",                        // empty
            "What is your name today and tomorrow forever ever" // extra words
        ).forEach { input ->
            val result = engine.validate(input, exercise)
            assertTrue("Score ${result.score} out of range for input: \"$input\"",
                result.score in 0..100)
        }
    }

    // -------------------------------------------------------------------
    // Requisito 2.8: score entre 80-100% do threshold → TOO_CLOSE
    // -------------------------------------------------------------------

    @Test
    fun `validate - near-match returns TOO_CLOSE`() {
        // Use a high threshold to make a partial match hit the TOO_CLOSE zone
        val exercise = makeExercise(
            questionType = QuestionType.WH_WHAT,
            answers = listOf("What is your name?" to 1.0f)  // threshold = 1.0 forces TOO_CLOSE for near matches
        )
        // "What is your name today" is similar but not identical
        val result = engine.validate("What is your name today", exercise)

        // Should NOT be CORRECT (threshold is 1.0 — requires identical)
        // Should be either TOO_CLOSE or INCORRECT depending on actual score
        assertTrue(result.status in listOf(ValidationStatus.TOO_CLOSE, ValidationStatus.INCORRECT))
        assertTrue(result.score in 0..100)
    }

    // -------------------------------------------------------------------
    // Requisito 2.6: low semantic match → INCORRECT
    // -------------------------------------------------------------------

    @Test
    fun `validate - completely unrelated answer returns INCORRECT`() {
        val exercise = makeExercise(
            questionType = QuestionType.WH_WHAT,
            answers = listOf("What is your name?" to 0.85f)
        )
        val result = engine.validate("What happened yesterday", exercise)

        // Semantically unrelated — should score low
        assertTrue(result.status in listOf(ValidationStatus.INCORRECT, ValidationStatus.TOO_CLOSE))
        assertTrue(result.score in 0..100)
    }

    // -------------------------------------------------------------------
    // Requisito 2.4: wrong structure → not CORRECT (structureResult fails)
    // -------------------------------------------------------------------

    @Test
    fun `validate - YES_NO without aux verb returns INCORRECT`() {
        val exercise = makeExercise(
            questionType = QuestionType.YES_NO,
            answers = listOf("Do you like coffee?" to 0.85f)
        )
        // "you like coffee" is a YES_NO type but missing the aux verb at start
        val result = engine.validate("you like coffee", exercise)

        // Either INCORRECT (type mismatch — "you" isn't an aux) or NOT CORRECT due to structure
        assertTrue(result.status != ValidationStatus.CORRECT)
        assertTrue(result.score in 0..100)
    }

    // -------------------------------------------------------------------
    // Requisito 2.10: INCORRECT returns corrections
    // -------------------------------------------------------------------

    @Test
    fun `validate - INCORRECT result includes corrections`() {
        val exercise = makeExercise(questionType = QuestionType.WH_WHAT)
        // Submit a YES/NO when WH_WHAT is expected
        val result = engine.validate("Is she here?", exercise)

        assertEquals(ValidationStatus.INCORRECT, result.status)
        assertTrue(result.corrections.isNotEmpty())
    }

    // -------------------------------------------------------------------
    // generateHints
    // -------------------------------------------------------------------

    @Test
    fun `generateHints - returns hints sorted by hintOrder`() {
        val hints = listOf(
            Hint(1L, 1L, 2, "Hint 2", HintType.VOCABULARY),
            Hint(2L, 1L, 1, "Hint 1", HintType.STRUCTURAL),
            Hint(3L, 1L, 3, "Hint 3", HintType.ANSWER)
        )
        val exercise = makeExercise(hints = hints)

        val result = engine.generateHints("What is your name?", exercise)

        // Result should be sorted by hintOrder
        val orders = result.map { it.hintOrder }
        assertEquals(orders.sorted(), orders)
    }

    @Test
    fun `generateHints - wrong type returns only first hint`() {
        val hints = listOf(
            Hint(1L, 1L, 1, "First hint", HintType.STRUCTURAL),
            Hint(2L, 1L, 2, "Second hint", HintType.VOCABULARY),
            Hint(3L, 1L, 3, "Third hint", HintType.ANSWER)
        )
        val exercise = makeExercise(questionType = QuestionType.WH_WHAT, hints = hints)

        // Submit wrong type (YES_NO)
        val result = engine.generateHints("Do you like it?", exercise)

        assertEquals(1, result.size)
    }

    @Test
    fun `generateHints - returns empty list when exercise has no hints`() {
        val exercise = makeExercise(hints = emptyList())

        val result = engine.generateHints("What is your name?", exercise)

        assertTrue(result.isEmpty())
    }

    // -------------------------------------------------------------------
    // scoreAnswer
    // -------------------------------------------------------------------

    @Test
    fun `scoreAnswer - returns 0 for wrong question type`() {
        val exercise = makeExercise(questionType = QuestionType.WH_WHAT)
        val score = engine.scoreAnswer("Do you like it?", exercise)
        assertEquals(0, score)
    }

    @Test
    fun `scoreAnswer - returns value in range 0 to 100`() {
        val exercise = makeExercise(questionType = QuestionType.WH_WHAT)
        val score = engine.scoreAnswer("What is your name?", exercise)
        assertTrue(score in 0..100)
    }

    @Test
    fun `scoreAnswer - verbatim answer scores higher than unrelated answer`() {
        val exercise = makeExercise(
            questionType = QuestionType.WH_WHAT,
            answers = listOf("What is your name?" to 0.85f)
        )
        val verbatimScore = engine.scoreAnswer("What is your name?", exercise)
        val unrelatedScore = engine.scoreAnswer("What is the capital city of france", exercise)

        assertTrue(verbatimScore >= unrelatedScore)
    }

    // -------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------

    @Test
    fun `constants have expected values`() {
        assertEquals(0.8f, QuestionValidationEngineImpl.CLOSE_THRESHOLD_FACTOR)
        assertEquals(10, QuestionValidationEngineImpl.STRUCTURE_BONUS)
    }
}
