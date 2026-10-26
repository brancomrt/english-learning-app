package com.englishlearning.app.ui.dictation

import com.englishlearning.app.domain.model.DictationExercise
import com.englishlearning.app.domain.model.DictationResult
import com.englishlearning.app.domain.repository.DictationRepository
import com.englishlearning.app.domain.repository.TtsRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.ints.shouldBeGreaterThan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Testes para DictationViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DictationViewModelTest : DescribeSpec({
    
    val testDispatcher = StandardTestDispatcher()
    
    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }
    
    afterSpec {
        Dispatchers.resetMain()
    }
    
    describe("Estado inicial") {
        
        it("deve iniciar com isSelectingStage = true") {
            runTest {
                val viewModel = createViewModel()
                testDispatcher.scheduler.advanceUntilIdle()
                
                val state = viewModel.uiState.first()
                state.isSelectingStage.shouldBeTrue()
            }
        }
        
        it("deve carregar stages disponíveis ao iniciar") {
            runTest {
                val repository = FakeDictationRepository().apply {
                    availableStages = listOf(1, 2, 3)
                }
                val viewModel = createViewModel(dictationRepository = repository)
                testDispatcher.scheduler.advanceUntilIdle()
                
                val state = viewModel.uiState.first()
                state.availableStages shouldBe listOf(1, 2, 3)
            }
        }
    }
    
    describe("selectStage") {
        
        it("deve selecionar stage e carregar exercícios") {
            runTest {
                val exercises = listOf(
                    createExercise(id = 1, stage = 2),
                    createExercise(id = 2, stage = 2)
                )
                val repository = FakeDictationRepository().apply {
                    exercisesByStage[2] = exercises
                }
                val viewModel = createViewModel(dictationRepository = repository)
                testDispatcher.scheduler.advanceUntilIdle()
                
                viewModel.selectStage(2)
                testDispatcher.scheduler.advanceUntilIdle()
                
                val state = viewModel.uiState.first()
                state.selectedStage shouldBe 2
                state.isSelectingStage.shouldBeFalse()
                state.totalExercises shouldBe 2
            }
        }
        
        it("deve definir currentExercise como o primeiro exercício") {
            runTest {
                val exercises = listOf(
                    createExercise(id = 1, stage = 1, title = "First"),
                    createExercise(id = 2, stage = 1, title = "Second")
                )
                val repository = FakeDictationRepository().apply {
                    exercisesByStage[1] = exercises
                }
                val viewModel = createViewModel(dictationRepository = repository)
                testDispatcher.scheduler.advanceUntilIdle()
                
                viewModel.selectStage(1)
                testDispatcher.scheduler.advanceUntilIdle()
                
                val state = viewModel.uiState.first()
                state.currentExercise.shouldNotBeNull()
                state.currentExercise?.title shouldBe "First"
            }
        }
    }
    
    describe("updateInput") {
        
        it("deve atualizar userInput no state") {
            runTest {
                val viewModel = createViewModel()
                testDispatcher.scheduler.advanceUntilIdle()
                
                viewModel.updateInput("Hello world")
                
                val state = viewModel.uiState.first()
                state.userInput shouldBe "Hello world"
            }
        }
    }
    
    describe("useHint") {
        
        it("deve incrementar hintsUsed") {
            runTest {
                val exercises = listOf(createExercise(id = 1, stage = 1, wordCount = 10))
                val repository = FakeDictationRepository().apply {
                    exercisesByStage[1] = exercises
                }
                val viewModel = createViewModel(dictationRepository = repository)
                testDispatcher.scheduler.advanceUntilIdle()
                
                viewModel.selectStage(1)
                testDispatcher.scheduler.advanceUntilIdle()
                
                viewModel.useHint()
                
                val state = viewModel.uiState.first()
                state.hintsUsed shouldBe 1
            }
        }
        
        it("deve fornecer dicas progressivas") {
            runTest {
                val exercises = listOf(
                    createExercise(
                        id = 1, 
                        stage = 1, 
                        text = "Hello world",
                        wordCount = 2,
                        translation = "Olá mundo"
                    )
                )
                val repository = FakeDictationRepository().apply {
                    exercisesByStage[1] = exercises
                }
                val viewModel = createViewModel(dictationRepository = repository)
                testDispatcher.scheduler.advanceUntilIdle()
                
                viewModel.selectStage(1)
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Primeira dica: número de palavras
                viewModel.useHint()
                var state = viewModel.uiState.first()
                state.currentHint shouldBe "Palavras: 2"
                
                // Segunda dica: primeira letra
                viewModel.useHint()
                state = viewModel.uiState.first()
                state.currentHint shouldBe "Primeira letra: H"
                
                // Terceira dica: tradução
                viewModel.useHint()
                state = viewModel.uiState.first()
                state.currentHint shouldBe "Olá mundo"
            }
        }
    }
    
    describe("retryExercise") {
        
        it("deve resetar o estado para tentar novamente") {
            runTest {
                val exercises = listOf(createExercise(id = 1, stage = 1))
                val repository = FakeDictationRepository().apply {
                    exercisesByStage[1] = exercises
                }
                val viewModel = createViewModel(dictationRepository = repository)
                testDispatcher.scheduler.advanceUntilIdle()
                
                viewModel.selectStage(1)
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Simula uso
                viewModel.updateInput("Some text")
                viewModel.useHint()
                
                // Retry
                viewModel.retryExercise()
                
                val state = viewModel.uiState.first()
                state.userInput shouldBe ""
                state.hintsUsed shouldBe 0
                state.isChecked.shouldBeFalse()
                state.currentResult.shouldBeNull()
            }
        }
    }
    
    describe("backToStageSelection") {
        
        it("deve voltar para seleção de stage") {
            runTest {
                val exercises = listOf(createExercise(id = 1, stage = 1))
                val repository = FakeDictationRepository().apply {
                    exercisesByStage[1] = exercises
                }
                val viewModel = createViewModel(dictationRepository = repository)
                testDispatcher.scheduler.advanceUntilIdle()
                
                viewModel.selectStage(1)
                testDispatcher.scheduler.advanceUntilIdle()
                
                viewModel.backToStageSelection()
                
                val state = viewModel.uiState.first()
                state.isSelectingStage.shouldBeTrue()
            }
        }
    }
})

// ═══════════════════════════════════════════════════════════════════════════════
// Helpers e Fakes
// ═══════════════════════════════════════════════════════════════════════════════

private fun createViewModel(
    dictationRepository: DictationRepository = FakeDictationRepository(),
    ttsRepository: TtsRepository = FakeTtsRepository()
): DictationViewModel {
    return DictationViewModel(dictationRepository, ttsRepository)
}

private fun createExercise(
    id: Long = 1,
    stage: Int = 1,
    title: String = "Test Exercise",
    text: String = "This is a test.",
    translation: String? = null,
    wordCount: Int = 4,
    grammarFocus: String? = null,
    completed: Boolean = false
) = DictationExercise(
    id = id,
    stage = stage,
    title = title,
    text = text,
    translation = translation,
    wordCount = wordCount,
    grammarFocus = grammarFocus,
    completed = completed
)

private class FakeDictationRepository : DictationRepository {
    var availableStages = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
    var exercisesByStage = mutableMapOf<Int, List<DictationExercise>>()
    var savedResults = mutableListOf<DictationResult>()
    
    override suspend fun getAvailableStages(): List<Int> = availableStages
    
    override suspend fun getByStage(stage: Int): List<DictationExercise> =
        exercisesByStage[stage] ?: emptyList()
    
    override fun getByStageFlow(stage: Int): kotlinx.coroutines.flow.Flow<List<DictationExercise>> =
        kotlinx.coroutines.flow.flowOf(exercisesByStage[stage] ?: emptyList())
    
    override suspend fun getById(id: Long): DictationExercise? =
        exercisesByStage.values.flatten().find { it.id == id }
    
    override suspend fun getAllExercises(): List<DictationExercise> =
        exercisesByStage.values.flatten()
    
    override fun getAllExercisesFlow(): kotlinx.coroutines.flow.Flow<List<DictationExercise>> =
        kotlinx.coroutines.flow.flowOf(exercisesByStage.values.flatten())
    
    override suspend fun getIncompleteByStage(stage: Int): List<DictationExercise> =
        exercisesByStage[stage]?.filter { !it.completed } ?: emptyList()
    
    override suspend fun saveResult(userId: Long, exerciseId: Long, result: DictationResult) {
        savedResults.add(result)
    }
    
    override suspend fun markCompleted(exerciseId: Long, completed: Boolean) {
        // No-op for tests
    }
    
    override suspend fun getCompletedCountByStage(stage: Int): Int =
        exercisesByStage[stage]?.count { it.completed } ?: 0
    
    override suspend fun getTotalCompletedCount(): Int =
        exercisesByStage.values.flatten().count { it.completed }
    
    override suspend fun getProgress(userId: Long, exerciseId: Long): com.englishlearning.app.domain.model.DictationProgress? = null
    
    override suspend fun getStatistics(userId: Long): com.englishlearning.app.domain.model.DictationStatistics =
        com.englishlearning.app.domain.model.DictationStatistics()
    
    override suspend fun getDueExercises(userId: Long, today: String, limit: Int): List<DictationExercise> =
        emptyList()
    
    override suspend fun getNewExercisesByStage(userId: Long, stage: Int, limit: Int): List<DictationExercise> =
        exercisesByStage[stage] ?: emptyList()
    
    override suspend fun getDueCount(userId: Long, today: String): Int = 0
    
    override fun getDueCountFlow(userId: Long, today: String): kotlinx.coroutines.flow.Flow<Int> =
        kotlinx.coroutines.flow.flowOf(0)
}

private class FakeTtsRepository : TtsRepository {
    var speakCallCount = 0
    var lastSpokenText: String? = null
    var lastSpeechRate: Float? = null
    
    override suspend fun speak(text: String, speechRate: Float) {
        speakCallCount++
        lastSpokenText = text
        lastSpeechRate = speechRate
    }
    
    override suspend fun speakWord(word: String, preferNative: Boolean): Boolean {
        speakCallCount++
        lastSpokenText = word
        return false // Simula uso de TTS
    }
    
    override fun stop() {}
    
    override fun isSpeaking(): Boolean = false
    
    override fun setDefaultSpeed(speed: Float) {}
    
    override fun getDefaultSpeed(): Float = 1.0f
    
    override fun hasNativeAudio(word: String): Boolean = false
}
