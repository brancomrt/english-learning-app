package com.englishlearning.app.ui.flashcard

import com.englishlearning.app.domain.model.Flashcard
import com.englishlearning.app.domain.model.FlashcardRating
import com.englishlearning.app.domain.model.PartOfSpeech
import com.englishlearning.app.domain.repository.FlashcardRepository
import com.englishlearning.app.domain.repository.TtsRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Testes para FlashcardViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FlashcardViewModelTest : DescribeSpec({
    
    val testDispatcher = StandardTestDispatcher()
    
    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }
    
    afterSpec {
        Dispatchers.resetMain()
    }
    
    describe("Estado inicial") {
        
        it("deve iniciar com isSelectingLevel = true") {
            runTest {
                val viewModel = createViewModel()
                
                val state = viewModel.uiState.first()
                state.isSelectingLevel.shouldBeTrue()
                state.isLoading.shouldBeFalse()
            }
        }
        
        it("deve ter níveis 1-12 disponíveis") {
            runTest {
                val viewModel = createViewModel()
                
                val state = viewModel.uiState.first()
                state.availableLevels shouldBe (1..12).toList()
            }
        }
    }
    
    describe("selectLevel") {
        
        it("deve selecionar nível e carregar cards") {
            runTest {
                val cards = listOf(
                    createFlashcard(id = 1, levelId = 2, word = "hello"),
                    createFlashcard(id = 2, levelId = 2, word = "world")
                )
                val repository = FakeFlashcardRepository().apply {
                    dueCards = cards
                }
                val viewModel = createViewModel(flashcardRepository = repository)
                
                viewModel.selectLevel(2)
                testDispatcher.scheduler.advanceUntilIdle()
                
                val state = viewModel.uiState.first()
                state.selectedLevel shouldBe 2
                state.isSelectingLevel.shouldBeFalse()
            }
        }
        
        it("deve definir currentCard como o primeiro card") {
            runTest {
                val cards = listOf(
                    createFlashcard(id = 1, levelId = 1, word = "first"),
                    createFlashcard(id = 2, levelId = 1, word = "second")
                )
                val repository = FakeFlashcardRepository().apply {
                    dueCards = cards
                }
                val viewModel = createViewModel(flashcardRepository = repository)
                
                viewModel.selectLevel(1)
                testDispatcher.scheduler.advanceUntilIdle()
                
                val state = viewModel.uiState.first()
                state.currentCard.shouldNotBeNull()
            }
        }
    }
    
    describe("flipCard") {
        
        it("deve alternar isShowingAnswer") {
            runTest {
                val viewModel = createViewModel()
                
                var state = viewModel.uiState.first()
                state.isShowingAnswer.shouldBeFalse()
                
                viewModel.flipCard()
                state = viewModel.uiState.first()
                state.isShowingAnswer.shouldBeTrue()
                
                viewModel.flipCard()
                state = viewModel.uiState.first()
                state.isShowingAnswer.shouldBeFalse()
            }
        }
    }
    
    describe("rateCard") {
        
        it("deve avançar para próximo card após rating") {
            runTest {
                val cards = listOf(
                    createFlashcard(id = 1, levelId = 1, word = "first"),
                    createFlashcard(id = 2, levelId = 1, word = "second")
                )
                val repository = FakeFlashcardRepository().apply {
                    dueCards = cards
                }
                val viewModel = createViewModel(flashcardRepository = repository)
                
                viewModel.selectLevel(1)
                testDispatcher.scheduler.advanceUntilIdle()
                
                viewModel.rateCard(FlashcardRating.GOOD)
                testDispatcher.scheduler.advanceUntilIdle()
                
                val state = viewModel.uiState.first()
                state.reviewedCount shouldBe 1
            }
        }
        
        it("deve incrementar correctCount para ratings positivos") {
            runTest {
                val cards = listOf(
                    createFlashcard(id = 1, levelId = 1),
                    createFlashcard(id = 2, levelId = 1)
                )
                val repository = FakeFlashcardRepository().apply {
                    dueCards = cards
                }
                val viewModel = createViewModel(flashcardRepository = repository)
                
                viewModel.selectLevel(1)
                testDispatcher.scheduler.advanceUntilIdle()
                
                viewModel.rateCard(FlashcardRating.GOOD)
                testDispatcher.scheduler.advanceUntilIdle()
                
                val state = viewModel.uiState.first()
                state.correctCount shouldBe 1
            }
        }
        
        it("deve chamar repository.reviewCard") {
            runTest {
                val cards = listOf(createFlashcard(id = 1, levelId = 1))
                val repository = FakeFlashcardRepository().apply {
                    dueCards = cards
                }
                val viewModel = createViewModel(flashcardRepository = repository)
                
                viewModel.selectLevel(1)
                testDispatcher.scheduler.advanceUntilIdle()
                
                viewModel.rateCard(FlashcardRating.EASY)
                testDispatcher.scheduler.advanceUntilIdle()
                
                repository.reviewedCards.size shouldBe 1
                repository.reviewedCards[0].second shouldBe FlashcardRating.EASY
            }
        }
        
        it("deve marcar sessão como completa quando todos cards revisados") {
            runTest {
                val cards = listOf(createFlashcard(id = 1, levelId = 1))
                val repository = FakeFlashcardRepository().apply {
                    dueCards = cards
                }
                val viewModel = createViewModel(flashcardRepository = repository)
                
                viewModel.selectLevel(1)
                testDispatcher.scheduler.advanceUntilIdle()
                
                viewModel.rateCard(FlashcardRating.GOOD)
                testDispatcher.scheduler.advanceUntilIdle()
                
                val state = viewModel.uiState.first()
                state.isComplete.shouldBeTrue()
            }
        }
    }
    
    describe("playAudio") {
        
        it("deve chamar ttsRepository.speakWord") {
            runTest {
                val cards = listOf(createFlashcard(id = 1, levelId = 1, word = "hello"))
                val repository = FakeFlashcardRepository().apply {
                    dueCards = cards
                }
                val ttsRepo = FakeTtsRepository()
                val viewModel = createViewModel(
                    flashcardRepository = repository,
                    ttsRepository = ttsRepo
                )
                
                viewModel.selectLevel(1)
                testDispatcher.scheduler.advanceUntilIdle()
                
                viewModel.playAudio()
                testDispatcher.scheduler.advanceUntilIdle()
                
                ttsRepo.speakWordCallCount shouldBe 1
                ttsRepo.lastSpokenWord shouldBe "hello"
            }
        }
    }
})

// ═══════════════════════════════════════════════════════════════════════════════
// Helpers e Fakes
// ═══════════════════════════════════════════════════════════════════════════════

private fun createViewModel(
    flashcardRepository: FlashcardRepository = FakeFlashcardRepository(),
    ttsRepository: TtsRepository = FakeTtsRepository()
): FlashcardViewModel {
    return FlashcardViewModel(flashcardRepository, ttsRepository)
}

private fun createFlashcard(
    id: Long = 1,
    levelId: Int = 1,
    word: String = "test",
    definition: String = "A trial or experiment",
    exampleSentence: String = "This is a test.",
    partOfSpeech: PartOfSpeech? = PartOfSpeech.NOUN,
    pronunciation: String? = "test",
    dueDate: String = "2026-10-26"
) = Flashcard(
    id = id,
    word = word,
    definition = definition,
    exampleSentence = exampleSentence,
    levelId = levelId,
    partOfSpeech = partOfSpeech,
    pronunciation = pronunciation,
    audioUrl = null,
    imageUrl = null,
    dueDate = dueDate
)

private class FakeFlashcardRepository : FlashcardRepository {
    var dueCards = listOf<Flashcard>()
    var newCards = listOf<Flashcard>()
    var reviewedCards = mutableListOf<Pair<Long, FlashcardRating>>()
    
    override suspend fun getDueCards(userId: Long, today: String, limit: Int): List<Flashcard> =
        dueCards
    
    override suspend fun getNewCards(userId: Long, limit: Int): List<Flashcard> =
        newCards
    
    override suspend fun reviewCard(cardId: Long, rating: FlashcardRating) {
        reviewedCards.add(cardId to rating)
    }
    
    override suspend fun getById(id: Long): Flashcard? =
        (dueCards + newCards).find { it.id == id }
    
    override suspend fun getDueCount(userId: Long, today: String): Int =
        dueCards.size
    
    override suspend fun getNewCount(userId: Long): Int =
        newCards.size
    
    override suspend fun getByLevel(levelId: Int): List<Flashcard> =
        (dueCards + newCards).filter { it.levelId == levelId }
    
    override suspend fun search(userId: Long, query: String, limit: Int): List<Flashcard> =
        (dueCards + newCards).filter { it.word.contains(query, ignoreCase = true) }.take(limit)
    
    override suspend fun getStatistics(userId: Long): com.englishlearning.app.domain.model.FlashcardStatistics =
        com.englishlearning.app.domain.model.FlashcardStatistics()
    
    override suspend fun resetProgress(userId: Long, today: String) {
        // No-op for tests
    }
    
    override suspend fun extractVocabularyFromDrills() {
        // No-op for tests
    }
    
    override fun getDueCountFlow(userId: Long, today: String): kotlinx.coroutines.flow.Flow<Int> =
        kotlinx.coroutines.flow.flowOf(dueCards.size)
}

private class FakeTtsRepository : TtsRepository {
    var speakCallCount = 0
    var speakWordCallCount = 0
    var lastSpokenText: String? = null
    var lastSpokenWord: String? = null
    
    override suspend fun speak(text: String, speechRate: Float) {
        speakCallCount++
        lastSpokenText = text
    }
    
    override suspend fun speakWord(word: String, preferNative: Boolean): Boolean {
        speakWordCallCount++
        lastSpokenWord = word
        return false
    }
    
    override fun stop() {}
    
    override fun isSpeaking(): Boolean = false
    
    override fun setDefaultSpeed(speed: Float) {}
    
    override fun getDefaultSpeed(): Float = 1.0f
    
    override fun hasNativeAudio(word: String): Boolean = false
}
