package com.englishlearning.app.di

import com.englishlearning.app.data.repository.ContentRepositoryImpl
import com.englishlearning.app.data.repository.ConversationChainRepositoryImpl
import com.englishlearning.app.data.repository.DailyChallengeRepositoryImpl
import com.englishlearning.app.data.repository.DictationRepositoryImpl
import com.englishlearning.app.data.repository.FlashcardRepositoryImpl
import com.englishlearning.app.data.repository.GrammarRepositoryImpl
import com.englishlearning.app.data.repository.ProgressRepositoryImpl
import com.englishlearning.app.data.repository.PronunciationRepositoryImpl
import com.englishlearning.app.data.repository.SpeechRecognitionRepositoryImpl
import com.englishlearning.app.data.repository.SrsRepositoryImpl
import com.englishlearning.app.data.repository.TtsRepositoryImpl
import com.englishlearning.app.domain.gamification.StreakCalculator
import com.englishlearning.app.domain.repository.ContentRepository
import com.englishlearning.app.domain.repository.ConversationChainRepository
import com.englishlearning.app.domain.repository.DailyChallengeRepository
import com.englishlearning.app.domain.repository.DictationRepository
import com.englishlearning.app.domain.repository.FlashcardRepository
import com.englishlearning.app.domain.repository.GrammarRepository
import com.englishlearning.app.domain.repository.ProgressRepository
import com.englishlearning.app.domain.repository.PronunciationRepository
import com.englishlearning.app.domain.repository.SpeechRecognitionRepository
import com.englishlearning.app.domain.repository.SrsRepository
import com.englishlearning.app.domain.repository.TtsRepository
import com.englishlearning.app.domain.validation.GrammarParser
import com.englishlearning.app.domain.validation.QuestionValidationEngine
import com.englishlearning.app.domain.validation.QuestionValidationEngineImpl
import com.englishlearning.app.domain.validation.SemanticMatcher
import com.englishlearning.app.domain.validation.TextNormalizer
import dagger.Binds
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module que vincula as interfaces de repositório às suas implementações
 * concretas e provê o motor de validação e seus colaboradores.
 *
 * Requisito 9.4: persistência e acesso offline via repositórios vinculados aqui.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Vincula [ContentRepositoryImpl] à interface [ContentRepository].
     *
     * Requisito 9.4: ContentRepository provê acesso offline a níveis, categorias e exercícios.
     */
    @Binds
    @Singleton
    abstract fun bindContentRepository(impl: ContentRepositoryImpl): ContentRepository

    /**
     * Vincula [ProgressRepositoryImpl] à interface [ProgressRepository].
     *
     * Requisito 9.4: ProgressRepository persiste tentativas, streaks e conquistas.
     */
    @Binds
    @Singleton
    abstract fun bindProgressRepository(impl: ProgressRepositoryImpl): ProgressRepository

    /**
     * Vincula [SrsRepositoryImpl] à interface [SrsRepository].
     *
     * Requisito 16.1: SrsRepository gerencia criação e agendamento de SRS cards.
     * Requisito 16.6: observable de cards devidos para badge na HomeScreen.
     * Requisito 18.6: persistência offline completa.
     */
    @Binds
    @Singleton
    abstract fun bindSrsRepository(impl: SrsRepositoryImpl): SrsRepository

    /**
     * Vincula [DailyChallengeRepositoryImpl] à interface [DailyChallengeRepository].
     */
    @Binds
    @Singleton
    abstract fun bindDailyChallengeRepository(impl: DailyChallengeRepositoryImpl): DailyChallengeRepository

    /**
     * Vincula [ConversationChainRepositoryImpl] à interface [ConversationChainRepository].
     */
    @Binds
    @Singleton
    abstract fun bindConversationChainRepository(impl: ConversationChainRepositoryImpl): ConversationChainRepository

    // ═══════════════════════════════════════════════════════════════════════
    // v1.28.0 Repositories
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Vincula [FlashcardRepositoryImpl] à interface [FlashcardRepository].
     */
    @Binds
    @Singleton
    abstract fun bindFlashcardRepository(impl: FlashcardRepositoryImpl): FlashcardRepository

    /**
     * Vincula [DictationRepositoryImpl] à interface [DictationRepository].
     */
    @Binds
    @Singleton
    abstract fun bindDictationRepository(impl: DictationRepositoryImpl): DictationRepository

    /**
     * Vincula [GrammarRepositoryImpl] à interface [GrammarRepository].
     */
    @Binds
    @Singleton
    abstract fun bindGrammarRepository(impl: GrammarRepositoryImpl): GrammarRepository

    /**
     * Vincula [PronunciationRepositoryImpl] à interface [PronunciationRepository].
     */
    @Binds
    @Singleton
    abstract fun bindPronunciationRepository(impl: PronunciationRepositoryImpl): PronunciationRepository

    /**
     * Vincula [SpeechRecognitionRepositoryImpl] à interface [SpeechRecognitionRepository].
     */
    @Binds
    @Singleton
    abstract fun bindSpeechRecognitionRepository(impl: SpeechRecognitionRepositoryImpl): SpeechRecognitionRepository

    /**
     * Vincula [TtsRepositoryImpl] à interface [TtsRepository].
     */
    @Binds
    @Singleton
    abstract fun bindTtsRepository(impl: TtsRepositoryImpl): TtsRepository

    companion object {

        /**
         * Provê [QuestionValidationEngineImpl] como implementação de [QuestionValidationEngine].
         *
         * Requisitos 2.1–2.10: motor de validação com pipeline de normalização, parsing
         * gramatical e matching semântico.
         */
        @Provides
        @Singleton
        fun provideQuestionValidationEngine(
            textNormalizer: TextNormalizer,
            grammarParser: GrammarParser,
            semanticMatcher: SemanticMatcher
        ): QuestionValidationEngine {
            return QuestionValidationEngineImpl(textNormalizer, grammarParser, semanticMatcher)
        }

        @Provides
        @Singleton
        fun provideTextNormalizer(): TextNormalizer = TextNormalizer()

        @Provides
        @Singleton
        fun provideGrammarParser(): GrammarParser = GrammarParser()

        @Provides
        @Singleton
        fun provideSemanticMatcher(): SemanticMatcher = SemanticMatcher()

        @Provides
        @Singleton
        fun provideStreakCalculator(): StreakCalculator = StreakCalculator()
    }
}
