package com.englishlearning.app.di

import android.content.Context
import androidx.room.Room
import com.englishlearning.app.data.database.EnglishLearningDatabase
import com.englishlearning.app.data.database.dao.AcceptedAnswerDao
import com.englishlearning.app.data.database.dao.AchievementDao
import com.englishlearning.app.data.database.dao.CategoryDao
import com.englishlearning.app.data.database.dao.ConversationChainDao
import com.englishlearning.app.data.database.dao.DailyChallengeDao
import com.englishlearning.app.data.database.dao.DictationDao
import com.englishlearning.app.data.database.dao.ExerciseDao
import com.englishlearning.app.data.database.dao.FlashcardDao
import com.englishlearning.app.data.database.dao.GrammarLessonDao
import com.englishlearning.app.data.database.dao.HintDao
import com.englishlearning.app.data.database.dao.LevelDao
import com.englishlearning.app.data.database.dao.ProgressDao
import com.englishlearning.app.data.database.dao.PronunciationDao
import com.englishlearning.app.data.database.dao.SrsDao
import com.englishlearning.app.data.database.dao.StreakDao
import com.englishlearning.app.data.database.dao.SyncDao
import com.englishlearning.app.data.database.dao.UserProfileDao
import com.englishlearning.app.data.database.dao.VocabularyDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module que provê o [EnglishLearningDatabase] e todos os seus DAOs
 * no escopo Singleton da aplicação.
 *
 * Inclui a migration [EnglishLearningDatabase.MIGRATION_1_2] que adiciona as
 * tabelas `srs_cards` e `vocabulary_cards` sem destruir dados existentes.
 *
 * Requisito 9.2: banco de dados Room inicializado como Singleton com
 * DatabaseSeeder chamado de forma assíncrona na inicialização.
 * Requisito 16.2, 18.6: migration para versão 2 com suporte a SRS.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Cria e fornece o banco de dados Room como Singleton.
     *
     * A migration [EnglishLearningDatabase.MIGRATION_1_2] é registrada explicitamente
     * para preservar dados de usuários ao adicionar as novas tabelas SRS.
     * [fallbackToDestructiveMigration] cobre quaisquer outras transições de esquema
     * não mapeadas durante o desenvolvimento.
     *
     * Requisito 9.2: banco Room configurado como fonte de dados local única.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EnglishLearningDatabase {
        return Room.databaseBuilder(
            context,
            EnglishLearningDatabase::class.java,
            EnglishLearningDatabase.DATABASE_NAME
        )
            .addMigrations(
                EnglishLearningDatabase.MIGRATION_1_2,
                EnglishLearningDatabase.MIGRATION_2_3,
                EnglishLearningDatabase.MIGRATION_3_4,
                EnglishLearningDatabase.MIGRATION_4_5,
                EnglishLearningDatabase.MIGRATION_5_6,
                EnglishLearningDatabase.MIGRATION_6_7,
                EnglishLearningDatabase.MIGRATION_7_8
            )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideLevelDao(db: EnglishLearningDatabase): LevelDao = db.levelDao()

    @Provides
    fun provideCategoryDao(db: EnglishLearningDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideExerciseDao(db: EnglishLearningDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun provideAcceptedAnswerDao(db: EnglishLearningDatabase): AcceptedAnswerDao =
        db.acceptedAnswerDao()

    @Provides
    fun provideHintDao(db: EnglishLearningDatabase): HintDao = db.hintDao()

    @Provides
    fun provideProgressDao(db: EnglishLearningDatabase): ProgressDao = db.progressDao()

    @Provides
    fun provideStreakDao(db: EnglishLearningDatabase): StreakDao = db.streakDao()

    @Provides
    fun provideAchievementDao(db: EnglishLearningDatabase): AchievementDao = db.achievementDao()

    @Provides
    fun provideUserProfileDao(db: EnglishLearningDatabase): UserProfileDao = db.userProfileDao()

    /** Provê [SrsDao] para operações com SRS cards. Requisito 16.2. */
    @Provides
    fun provideSrsDao(db: EnglishLearningDatabase): SrsDao = db.srsDao()

    /** Provê [VocabularyDao] para operações com vocabulary cards. Requisito 16.9. */
    @Provides
    fun provideVocabularyDao(db: EnglishLearningDatabase): VocabularyDao = db.vocabularyDao()

    /** Provê [DailyChallengeDao] para desafios diários. */
    @Provides
    fun provideDailyChallengeDao(db: EnglishLearningDatabase): DailyChallengeDao =
        db.dailyChallengeDao()

    /** Provê [ConversationChainDao] para conversation chains. */
    @Provides
    fun provideConversationChainDao(db: EnglishLearningDatabase): ConversationChainDao =
        db.conversationChainDao()

    // ═══════════════════════════════════════════════════════════════════════
    // v1.28.0 DAOs
    // ═══════════════════════════════════════════════════════════════════════

    /** Provê [FlashcardDao] para flashcards de vocabulário. */
    @Provides
    fun provideFlashcardDao(db: EnglishLearningDatabase): FlashcardDao =
        db.flashcardDao()

    /** Provê [DictationDao] para exercícios de ditado. */
    @Provides
    fun provideDictationDao(db: EnglishLearningDatabase): DictationDao =
        db.dictationDao()

    /** Provê [GrammarLessonDao] para lições de gramática. */
    @Provides
    fun provideGrammarLessonDao(db: EnglishLearningDatabase): GrammarLessonDao =
        db.grammarLessonDao()

    /** Provê [PronunciationDao] para avaliação de pronúncia. */
    @Provides
    fun providePronunciationDao(db: EnglishLearningDatabase): PronunciationDao =
        db.pronunciationDao()

    /** Provê [SyncDao] para sincronização com a nuvem. */
    @Provides
    fun provideSyncDao(db: EnglishLearningDatabase): SyncDao =
        db.syncDao()
}
