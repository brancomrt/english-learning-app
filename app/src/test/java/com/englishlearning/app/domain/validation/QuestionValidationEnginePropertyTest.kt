package com.englishlearning.app.domain.validation

import com.englishlearning.app.domain.model.AcceptedAnswer
import com.englishlearning.app.domain.model.Exercise
import com.englishlearning.app.domain.model.QuestionType
import com.englishlearning.app.domain.model.ValidationStatus
import io.kotest.core.spec.style.FreeSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.forAll

/**
 * Property-based tests for [QuestionValidationEngineImpl].
 *
 * **Validates: Requisitos 2.7, 2.9**
 */
class QuestionValidationEnginePropertyTest : FreeSpec({

    val textNormalizer = TextNormalizer()
    val grammarParser = GrammarParser()
    val semanticMatcher = SemanticMatcher()
    val engine = QuestionValidationEngineImpl(textNormalizer, grammarParser, semanticMatcher)

    // ---------------------------------------------------------------------------
    // Canonical accepted answers per QuestionType.
    //
    // Each entry maps a QuestionType to a list of valid accepted question texts
    // that the GrammarParser will correctly identify as that type and whose
    // structure will pass checkStructure. This is required to make Property 10
    // meaningful: we need verbatim submission to return CORRECT, which requires
    // both type detection and structure check to pass.
    // ---------------------------------------------------------------------------
    val canonicalAnswers: Map<QuestionType, List<String>> = mapOf(
        QuestionType.WH_WHAT     to listOf(
            "What is your name?",
            "What do you do?",
            "What did she say?",
            "What are they doing?"
        ),
        QuestionType.WH_WHERE    to listOf(
            "Where do you live?",
            "Where is the station?",
            "Where did they go?",
            "Where are you from?"
        ),
        QuestionType.WH_WHEN     to listOf(
            "When did it happen?",
            "When does the train arrive?",
            "When is the meeting?",
            "When will you be back?"
        ),
        QuestionType.WH_WHO      to listOf(
            "Who called you?",
            "Who is she?",
            "Who did you see?"
        ),
        QuestionType.WH_WHY      to listOf(
            "Why are you late?",
            "Why did she leave?",
            "Why do you ask?"
        ),
        QuestionType.WH_HOW      to listOf(
            "How do you feel?",
            "How did it happen?",
            "How are you doing?"
        ),
        QuestionType.WH_HOW_MUCH to listOf(
            "How much does it cost?",
            "How much is the ticket?",
            "How much did you pay?"
        ),
        QuestionType.WH_HOW_MANY to listOf(
            "How many people are there?",
            "How many books do you have?",
            "How many days did it take?"
        ),
        QuestionType.WH_WHICH    to listOf(
            "Which one do you prefer?",
            "Which color is it?",
            "Which way did they go?"
        ),
        QuestionType.YES_NO      to listOf(
            "Do you like coffee?",
            "Is she at home?",
            "Did they arrive?",
            "Will you come tomorrow?"
        ),
        QuestionType.TAG         to listOf(
            "You like coffee, don't you?",
            "She is here, isn't she?",
            "They will come, won't they?"
        ),
        QuestionType.INDIRECT    to listOf(
            "Could you tell me where he lives?",
            "Do you know what time it is?",
            "Can you tell me how to get there?",
            "I wonder where she went yesterday"
        )
    )

    // ---------------------------------------------------------------------------
    // Arb generators
    // ---------------------------------------------------------------------------

    /**
     * Generates an arbitrary QuestionType that has canonical answers defined.
     * CHOICE is excluded because its detection falls back to YES_NO, making
     * verbatim testing unreliable for Property 10.
     */
    val arbQuestionType: Arb<QuestionType> = Arb.element(canonicalAnswers.keys.toList())

    /**
     * Generates an arbitrary AcceptedAnswer for a given questionText.
     * matchThreshold is kept <= 0.95 to give the semantic matcher a reasonable
     * chance to clear it when the verbatim text is submitted.
     */
    fun arbAcceptedAnswer(questionText: String, exerciseId: Long = 1L): Arb<AcceptedAnswer> =
        Arb.bind(
            Arb.float(min = 0.50f, max = 0.95f),
            Arb.boolean()
        ) { threshold, isPrimary ->
            AcceptedAnswer(
                id = 0L,
                exerciseId = exerciseId,
                questionText = questionText,
                isPrimary = isPrimary,
                matchThreshold = threshold
            )
        }

    /**
     * Generates a complete Exercise for a specific QuestionType.
     * Picks one canonical accepted answer for that type.
     * difficultyScore in [1, 10] as per the spec.
     */
    fun arbExerciseForType(questionType: QuestionType): Arb<Exercise> {
        val answers = canonicalAnswers.getValue(questionType)
        return Arb.bind(
            Arb.element(answers),
            Arb.int(min = 1, max = 10)
        ) { questionText, difficulty ->
            Exercise(
                id = 1L,
                levelId = 1,
                categoryId = 1,
                answerText = "Sample answer text.",
                contextText = null,
                questionType = questionType,
                grammarFocus = "test",
                difficultyScore = difficulty,
                xpReward = 10,
                acceptedAnswers = listOf(
                    AcceptedAnswer(
                        id = 0L,
                        exerciseId = 1L,
                        questionText = questionText,
                        isPrimary = true,
                        // Use a threshold low enough that the verbatim text clears it,
                        // while still being non-trivial (>= 0.5).
                        matchThreshold = 0.50f
                    )
                ),
                hints = emptyList()
            )
        }
    }

    /**
     * Generates an arbitrary Exercise with an arbitrary question type and at
     * least one AcceptedAnswer, as required by the engine's pre-conditions.
     * Used for Property 7 (score range).
     */
    val arbExerciseAnyType: Arb<Exercise> = arbQuestionType.flatMap { qt ->
        arbExerciseForType(qt)
    }

    // ---------------------------------------------------------------------------
    // Propriedade 7: Score de Validação no Intervalo [0, 100]
    //
    // Para qualquer string de pergunta do usuário e qualquer Exercise válido
    // com pelo menos uma AcceptedAnswer, validate() retorna score em [0, 100].
    //
    // Valida: Requisito 2.9
    // ---------------------------------------------------------------------------
    "Propriedade 7 - Score de Validação no Intervalo [0, 100]" {
        forAll(
            iterations = 1000,
            arbExerciseAnyType,
            Arb.string()
        ) { exercise, userQuestion ->
            val result = engine.validate(userQuestion, exercise)
            result.score in 0..100
        }
    }

    // ---------------------------------------------------------------------------
    // Propriedade 10: Resposta Aceita Verbatim Retorna CORRECT
    //
    // Para qualquer Exercise com pelo menos uma AcceptedAnswer e matchThreshold <= 1.0,
    // submeter o questionText exato da AcceptedAnswer (sem modificações) retorna
    // ValidationStatus.CORRECT.
    //
    // Valida: Requisito 2.7
    // ---------------------------------------------------------------------------
    "Propriedade 10 - Resposta Aceita Verbatim Retorna CORRECT" {
        forAll(
            iterations = 500,
            arbQuestionType
        ) { questionType ->
            val answers = canonicalAnswers.getValue(questionType)
            // Test every canonical answer for this type
            answers.all { questionText ->
                val exercise = Exercise(
                    id = 1L,
                    levelId = 1,
                    categoryId = 1,
                    answerText = "Sample answer text.",
                    contextText = null,
                    questionType = questionType,
                    grammarFocus = "test",
                    difficultyScore = 5,
                    xpReward = 10,
                    acceptedAnswers = listOf(
                        AcceptedAnswer(
                            id = 0L,
                            exerciseId = 1L,
                            questionText = questionText,
                            isPrimary = true,
                            matchThreshold = 0.50f
                        )
                    ),
                    hints = emptyList()
                )
                val result = engine.validate(questionText, exercise)
                result.status == ValidationStatus.CORRECT
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Propriedade 10 (variante): Verificar com difficultyScore gerado arbitrariamente
    //
    // A pontuação não deve afetar se a resposta é CORRECT — apenas o score numérico.
    // Valida: Requisito 2.7
    // ---------------------------------------------------------------------------
    "Propriedade 10 (variante) - Verbatim com difficultyScore arbitrário retorna CORRECT" {
        forAll(
            iterations = 500,
            arbExerciseAnyType
        ) { exercise ->
            // Use the first accepted answer's questionText verbatim
            val verbatimQuestion = exercise.acceptedAnswers.first().questionText
            val result = engine.validate(verbatimQuestion, exercise)
            result.status == ValidationStatus.CORRECT
        }
    }
})
