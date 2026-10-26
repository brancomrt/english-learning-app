package com.englishlearning.app.domain.model

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldBeEmpty

/**
 * Testes para os modelos de Dictation.
 */
class DictationModelsTest : DescribeSpec({
    
    describe("DictationExercise") {
        
        it("deve criar exercício com valores padrão") {
            val exercise = DictationExercise(
                id = 1,
                stage = 1,
                title = "Test Exercise",
                text = "This is a test."
            )
            
            exercise.id shouldBe 1
            exercise.stage shouldBe 1
            exercise.title shouldBe "Test Exercise"
            exercise.text shouldBe "This is a test."
            exercise.translation shouldBe null
            exercise.wordCount shouldBe 0
            exercise.grammarFocus shouldBe null
            exercise.completed shouldBe false
        }
        
        it("deve criar exercício completo") {
            val exercise = DictationExercise(
                id = 1,
                stage = 3,
                title = "Shopping",
                text = "I went to the shop yesterday.",
                translation = "Eu fui à loja ontem.",
                wordCount = 6,
                grammarFocus = "Simple Past",
                completed = true
            )
            
            exercise.stage shouldBe 3
            exercise.wordCount shouldBe 6
            exercise.grammarFocus shouldBe "Simple Past"
            exercise.completed shouldBe true
        }
    }
    
    describe("DictationResult") {
        
        it("deve calcular score corretamente sem erros") {
            val result = DictationResult(
                exerciseId = 1,
                expectedText = "This is a test.",
                userText = "This is a test.",
                score = 100,
                accuracy = 1.0f,
                mistakes = emptyList(),
                timeTakenMs = 5000,
                hintsUsed = 0
            )
            
            result.score shouldBe 100
            result.accuracy shouldBe 1.0f
            result.mistakes.shouldBeEmpty()
        }
        
        it("deve registrar erros corretamente") {
            val mistakes = listOf(
                DictationMistake(
                    position = 2,
                    expected = "a",
                    actual = "the",
                    type = MistakeType.WRONG_WORD
                )
            )
            
            val result = DictationResult(
                exerciseId = 1,
                expectedText = "This is a test.",
                userText = "This is the test.",
                score = 75,
                accuracy = 0.75f,
                mistakes = mistakes,
                timeTakenMs = 5000,
                hintsUsed = 0
            )
            
            result.mistakes shouldHaveSize 1
            result.mistakes[0].type shouldBe MistakeType.WRONG_WORD
            result.mistakes[0].expected shouldBe "a"
            result.mistakes[0].actual shouldBe "the"
        }
        
        it("deve registrar diferentes tipos de erros") {
            val mistakes = listOf(
                DictationMistake(0, "Hello", "Helo", MistakeType.SPELLING),
                DictationMistake(1, "world", null, MistakeType.MISSING_WORD),
                DictationMistake(2, "", "extra", MistakeType.EXTRA_WORD)
            )
            
            mistakes[0].type shouldBe MistakeType.SPELLING
            mistakes[1].type shouldBe MistakeType.MISSING_WORD
            mistakes[2].type shouldBe MistakeType.EXTRA_WORD
        }
    }
    
    describe("MistakeType") {
        
        it("deve ter todos os tipos de erro definidos") {
            MistakeType.entries.map { it.name } shouldBe listOf(
                "SPELLING",
                "MISSING_WORD", 
                "EXTRA_WORD",
                "WRONG_WORD",
                "PUNCTUATION",
                "CAPITALIZATION"
            )
        }
    }
})
