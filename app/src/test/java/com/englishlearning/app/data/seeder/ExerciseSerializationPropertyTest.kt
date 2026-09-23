package com.englishlearning.app.data.seeder

import io.kotest.core.spec.style.FreeSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.forAll
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Property-based tests for JSON serialization round-trip of [ExerciseJsonModel].
 *
 * **Validates: Requisitos 11.3**
 *
 * Propriedade 5: Round-trip de Serialização de Exercício
 * Para qualquer ExerciseJsonModel válido, serializar para JSON com kotlinx.serialization
 * e desserializar produz um objeto equivalente ao original.
 */
class ExerciseSerializationPropertyTest : FreeSpec({

    // Use a lenient Json instance matching the one in DatabaseSeeder
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // ---------------------------------------------------------------------------
    // Generators
    // ---------------------------------------------------------------------------

    val questionTypes = listOf(
        "WH_WHAT", "WH_WHERE", "WH_WHEN", "WH_WHO", "WH_WHY",
        "WH_HOW", "WH_HOW_MUCH", "WH_HOW_MANY", "WH_WHICH",
        "YES_NO", "TAG", "INDIRECT", "CHOICE"
    )
    val hintTypes = listOf("structural", "vocabulary", "grammar", "answer")

    // Arb.float(min, max) — Kotest 5.x uses named params or positional (Float, Float)
    val arbMatchThreshold: Arb<Float> = Arb.float(min = 0.70f, max = 1.00f)

    val arbAcceptedAnswer: Arb<AcceptedAnswerJsonModel> = Arb.bind(
        Arb.long(0L..999L),
        Arb.string(minSize = 3, maxSize = 80),
        Arb.boolean(),
        arbMatchThreshold
    ) { id, text, isPrimary, threshold ->
        AcceptedAnswerJsonModel(
            id = id,
            questionText = text,
            isPrimary = isPrimary,
            matchThreshold = threshold
        )
    }

    val arbHint: Arb<HintJsonModel> = Arb.bind(
        Arb.long(0L..999L),
        Arb.int(1..5),
        Arb.string(minSize = 3, maxSize = 100),
        Arb.int(0..3)
    ) { id, order, text, typeIndex ->
        HintJsonModel(
            id = id,
            hintOrder = order,
            hintText = text,
            hintType = hintTypes[typeIndex]
        )
    }

    /**
     * Builds a full ExerciseJsonModel. Uses chained Arb.bind (≤6 params) + map to avoid
     * arity limits in Kotest's Arb.bind overloads.
     */
    data class ExercisePart1(
        val id: Long, val levelId: Int, val categoryId: Int,
        val answerText: String, val contextText: String?, val qtIndex: Int
    )
    data class ExercisePart2(
        val grammarFocus: String, val difficultyScore: Int, val xpReward: Int
    )

    val arbPart1: Arb<ExercisePart1> = Arb.bind(
        Arb.long(1L..9_999L),
        Arb.int(1..5),
        Arb.int(1..20),
        Arb.string(minSize = 3, maxSize = 120),
        Arb.string(minSize = 1, maxSize = 60).orNull(nullProbability = 0.4),
        Arb.int(0..12)
    ) { id, levelId, categoryId, answerText, contextText, qtIndex ->
        ExercisePart1(id, levelId, categoryId, answerText, contextText, qtIndex)
    }

    val arbPart2: Arb<ExercisePart2> = Arb.bind(
        Arb.string(minSize = 3, maxSize = 30),
        Arb.int(1..10),
        Arb.int(5..100)
    ) { grammarFocus, difficultyScore, xpReward ->
        ExercisePart2(grammarFocus, difficultyScore, xpReward)
    }

    val arbExercise: Arb<ExerciseJsonModel> = Arb.bind(arbPart1, arbPart2) { p1, p2 ->
        ExerciseJsonModel(
            id = p1.id,
            levelId = p1.levelId,
            categoryId = p1.categoryId,
            answerText = p1.answerText,
            contextText = p1.contextText,
            questionType = questionTypes[p1.qtIndex],
            grammarFocus = p2.grammarFocus,
            difficultyScore = p2.difficultyScore,
            xpReward = p2.xpReward,
            sortOrder = 0,
            acceptedAnswers = listOf(
                AcceptedAnswerJsonModel(
                    id = 1L,
                    questionText = "What is your name?",
                    isPrimary = true,
                    matchThreshold = 0.85f
                )
            ),
            hints = emptyList()
        )
    }

    // ---------------------------------------------------------------------------
    // Propriedade 5: Round-trip de Serialização de Exercício
    // serialize(x) → deserialize → x  (para qualquer ExerciseJsonModel válido)
    // Valida: Requisitos 11.3
    // ---------------------------------------------------------------------------
    "Propriedade 5 - Round-trip de Serialização: serialize→deserialize produz objeto equivalente" {
        forAll(
            iterations = 500,
            arbExercise
        ) { original ->
            val serialized = json.encodeToString(original)
            val deserialized = json.decodeFromString<ExerciseJsonModel>(serialized)
            deserialized == original
        }
    }

    // ---------------------------------------------------------------------------
    // Corolário: round-trip preserva contextText nulo
    // ---------------------------------------------------------------------------
    "Propriedade 5 (corolário) - Round-trip preserva contextText nulo" {
        forAll(
            iterations = 200,
            Arb.long(1L..9_999L),
            Arb.string(minSize = 3, maxSize = 80)
        ) { id, answerText ->
            val exercise = ExerciseJsonModel(
                id = id,
                levelId = 1,
                categoryId = 1,
                answerText = answerText,
                contextText = null,
                questionType = "YES_NO",
                grammarFocus = "present simple",
                difficultyScore = 3,
                xpReward = 10,
                sortOrder = 0,
                acceptedAnswers = emptyList(),
                hints = emptyList()
            )
            val serialized = json.encodeToString(exercise)
            val deserialized = json.decodeFromString<ExerciseJsonModel>(serialized)
            deserialized == exercise
        }
    }

    // ---------------------------------------------------------------------------
    // Corolário: round-trip preserva listas aninhadas de AcceptedAnswer e Hint
    // ---------------------------------------------------------------------------
    "Propriedade 5 (corolário) - Round-trip preserva listas de acceptedAnswers e hints" {
        forAll(
            iterations = 200,
            Arb.bind(
                Arb.long(1L..9_999L),
                Arb.list(arbAcceptedAnswer, 1..3),
                Arb.list(arbHint, 0..3)
            ) { id, answers, hints ->
                Triple(id, answers, hints)
            }
        ) { (id, answers, hints) ->
            val exercise = ExerciseJsonModel(
                id = id,
                levelId = 1,
                categoryId = 2,
                answerText = "I am fine.",
                contextText = null,
                questionType = "WH_HOW",
                grammarFocus = "to be",
                difficultyScore = 2,
                xpReward = 15,
                sortOrder = 1,
                acceptedAnswers = answers,
                hints = hints
            )
            val serialized = json.encodeToString(exercise)
            val deserialized = json.decodeFromString<ExerciseJsonModel>(serialized)
            deserialized == exercise
        }
    }
})
