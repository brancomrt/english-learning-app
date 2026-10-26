package com.englishlearning.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.englishlearning.app.data.database.converter.Converters
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
import com.englishlearning.app.data.database.entity.AcceptedAnswerEntity
import com.englishlearning.app.data.database.entity.AchievementEntity
import com.englishlearning.app.data.database.entity.AttemptEntity
import com.englishlearning.app.data.database.entity.CategoryEntity
import com.englishlearning.app.data.database.entity.ChainExerciseEntity
import com.englishlearning.app.data.database.entity.ChainProgressEntity
import com.englishlearning.app.data.database.entity.ConversationChainEntity
import com.englishlearning.app.data.database.entity.DailyChallengeEntity
import com.englishlearning.app.data.database.entity.DictationExerciseEntity
import com.englishlearning.app.data.database.entity.DictationProgressEntity
import com.englishlearning.app.data.database.entity.ExerciseEntity
import com.englishlearning.app.data.database.entity.FlashcardEntity
import com.englishlearning.app.data.database.entity.GrammarExampleEntity
import com.englishlearning.app.data.database.entity.GrammarLessonEntity
import com.englishlearning.app.data.database.entity.GrammarProgressEntity
import com.englishlearning.app.data.database.entity.HintEntity
import com.englishlearning.app.data.database.entity.LevelEntity
import com.englishlearning.app.data.database.entity.PronunciationResultEntity
import com.englishlearning.app.data.database.entity.PronunciationStatsEntity
import com.englishlearning.app.data.database.entity.SrsCardEntity
import com.englishlearning.app.data.database.entity.StreakRecordEntity
import com.englishlearning.app.data.database.entity.SyncLogEntity
import com.englishlearning.app.data.database.entity.SyncMetadataEntity
import com.englishlearning.app.data.database.entity.SyncSettingsEntity
import com.englishlearning.app.data.database.entity.UserProfileEntity
import com.englishlearning.app.data.database.entity.UserProgressEntity
import com.englishlearning.app.data.database.entity.VocabularyCardEntity

/**
 * Banco de dados Room principal do aplicativo.
 *
 * Versão 2 adiciona as tabelas `srs_cards` e `vocabulary_cards` para suporte
 * ao sistema de Repetição Espaçada (SRS) com algoritmo FSRS-4.5.
 * A migration [MIGRATION_1_2] aplica o DDL necessário sem destruir dados existentes.
 *
 * A migration strategy (fallbackToDestructiveMigration) é configurada no builder
 * dentro do DatabaseModule (camada de injeção de dependência), mantendo esta
 * classe focada apenas na definição do esquema.
 *
 * Requisito 9.1: persistência de todas as entidades no Room Database.
 * Requisito 16.2: SrsCardEntity com todos os campos FSRS.
 * Requisito 18.6: persistência completa offline dos SRS cards.
 */
@Database(
    entities = [
        // ── v1 ──────────────────────────────────────────────────────────────
        LevelEntity::class,
        CategoryEntity::class,
        ExerciseEntity::class,
        AcceptedAnswerEntity::class,
        HintEntity::class,
        UserProfileEntity::class,
        UserProgressEntity::class,
        AttemptEntity::class,
        StreakRecordEntity::class,
        AchievementEntity::class,
        // ── v2 ──────────────────────────────────────────────────────────────
        SrsCardEntity::class,
        VocabularyCardEntity::class,
        // ── v5 (Learning Methodology Features) ──────────────────────────────
        DailyChallengeEntity::class,
        ConversationChainEntity::class,
        ChainExerciseEntity::class,
        ChainProgressEntity::class,
        // ── v6 (v1.28.0 Features) ───────────────────────────────────────────
        FlashcardEntity::class,
        DictationExerciseEntity::class,
        DictationProgressEntity::class,
        GrammarLessonEntity::class,
        GrammarExampleEntity::class,
        GrammarProgressEntity::class,
        PronunciationResultEntity::class,
        PronunciationStatsEntity::class,
        SyncMetadataEntity::class,
        SyncSettingsEntity::class,
        SyncLogEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class EnglishLearningDatabase : RoomDatabase() {

    companion object {
        const val DATABASE_NAME = "english_learning_db"

        /**
         * Migração da versão 1 para a versão 2.
         *
         * Cria a tabela `srs_cards` com todos os campos do algoritmo FSRS-4.5
         * e a tabela `vocabulary_cards` para os cards de vocabulário.
         * Ambas incluem os índices necessários para performance nas queries de
         * cards devidos (dueDate, userId) e busca por referência (refId).
         *
         * Requisito 16.2: campos FSRS stability, difficulty, elapsedDays,
         *                 scheduledDays, reps, lapses, state, dueDate, lastReviewDate.
         * Requisito 16.9: campos word, definition, exampleSentence, levelId, categoryId.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `vocabulary_cards` ADD COLUMN `image_url` TEXT"
                )
            }
        }

        /**
         * Migração da versão 4 para a versão 5.
         * 
         * Adiciona tabelas para Daily Challenges e Conversation Chains
         * como parte das melhorias metodológicas de aprendizado.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Tabela de desafios diários
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_challenges` (
                        `id`              INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `userId`          INTEGER NOT NULL,
                        `type`            TEXT    NOT NULL,
                        `title`           TEXT    NOT NULL,
                        `description`     TEXT    NOT NULL,
                        `targetValue`     INTEGER NOT NULL,
                        `currentProgress` INTEGER NOT NULL DEFAULT 0,
                        `xpReward`        INTEGER NOT NULL,
                        `bonusXp`         INTEGER NOT NULL DEFAULT 0,
                        `date`            TEXT    NOT NULL,
                        `isCompleted`     INTEGER NOT NULL DEFAULT 0,
                        `completedAt`     INTEGER,
                        `iconEmoji`       TEXT    NOT NULL DEFAULT '🎯'
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_daily_challenges_userId_date` ON `daily_challenges` (`userId`, `date`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_daily_challenges_userId_type` ON `daily_challenges` (`userId`, `type`)"
                )

                // Tabela de conversation chains
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `conversation_chains` (
                        `id`           INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title`        TEXT    NOT NULL,
                        `description`  TEXT    NOT NULL,
                        `levelId`      INTEGER NOT NULL,
                        `categoryId`   INTEGER NOT NULL,
                        `contextStory` TEXT,
                        `sortOrder`    INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_conversation_chains_levelId` ON `conversation_chains` (`levelId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_conversation_chains_levelId_categoryId` ON `conversation_chains` (`levelId`, `categoryId`)"
                )

                // Tabela de exercícios dentro de chains
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chain_exercises` (
                        `chainId`      INTEGER NOT NULL,
                        `exerciseId`   INTEGER NOT NULL,
                        `orderInChain` INTEGER NOT NULL,
                        `speakerRole`  TEXT    NOT NULL,
                        PRIMARY KEY(`chainId`, `exerciseId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_chain_exercises_chainId` ON `chain_exercises` (`chainId`)"
                )

                // Tabela de progresso do usuário em chains
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chain_progress` (
                        `userId`                INTEGER NOT NULL,
                        `chainId`               INTEGER NOT NULL,
                        `currentExerciseIndex`  INTEGER NOT NULL DEFAULT 0,
                        `isCompleted`           INTEGER NOT NULL DEFAULT 0,
                        `completedAt`           INTEGER,
                        `startedAt`             INTEGER NOT NULL,
                        PRIMARY KEY(`userId`, `chainId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_chain_progress_userId` ON `chain_progress` (`userId`)"
                )
            }
        }

        /**
         * Migração da versão 5 para a versão 6.
         * 
         * Adiciona tabelas para v1.28.0 features:
         * - Flashcards com SRS separado
         * - Exercícios de ditado (Spaced Writing)
         * - Lições de gramática interativas
         * - Avaliação de pronúncia
         * - Sincronização com a nuvem
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ─── Flashcards ─────────────────────────────────────────────
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `flashcards` (
                        `id`              INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `userId`          INTEGER NOT NULL DEFAULT 1,
                        `word`            TEXT    NOT NULL,
                        `definition`      TEXT    NOT NULL,
                        `exampleSentence` TEXT    NOT NULL,
                        `context`         TEXT,
                        `sourceDrillId`   INTEGER,
                        `levelId`         INTEGER NOT NULL,
                        `partOfSpeech`    TEXT,
                        `pronunciation`   TEXT,
                        `audioUrl`        TEXT,
                        `imageUrl`        TEXT,
                        `stability`       REAL    NOT NULL DEFAULT 0.0,
                        `difficulty`      REAL    NOT NULL DEFAULT 5.0,
                        `elapsedDays`     INTEGER NOT NULL DEFAULT 0,
                        `scheduledDays`   INTEGER NOT NULL DEFAULT 0,
                        `reps`            INTEGER NOT NULL DEFAULT 0,
                        `lapses`          INTEGER NOT NULL DEFAULT 0,
                        `state`           TEXT    NOT NULL DEFAULT 'NEW',
                        `dueDate`         TEXT    NOT NULL,
                        `lastReviewDate`  TEXT,
                        `createdAt`       INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_flashcards_word` ON `flashcards` (`word`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcards_levelId` ON `flashcards` (`levelId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcards_userId_dueDate` ON `flashcards` (`userId`, `dueDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcards_userId_state` ON `flashcards` (`userId`, `state`)")

                // ─── Dictation Exercises ────────────────────────────────────
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `dictation_exercises` (
                        `id`            INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sentence`      TEXT    NOT NULL,
                        `translation`   TEXT,
                        `levelId`       INTEGER NOT NULL,
                        `sourceDrillId` INTEGER,
                        `difficulty`    INTEGER NOT NULL DEFAULT 1,
                        `keywords`      TEXT,
                        `grammarTopic`  TEXT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dictation_exercises_levelId` ON `dictation_exercises` (`levelId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dictation_exercises_difficulty` ON `dictation_exercises` (`difficulty`)")

                // ─── Dictation Progress ─────────────────────────────────────
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `dictation_progress` (
                        `userId`         INTEGER NOT NULL,
                        `exerciseId`     INTEGER NOT NULL,
                        `attempts`       INTEGER NOT NULL DEFAULT 0,
                        `correctCount`   INTEGER NOT NULL DEFAULT 0,
                        `bestScore`      INTEGER NOT NULL DEFAULT 0,
                        `lastScore`      INTEGER NOT NULL DEFAULT 0,
                        `stability`      REAL    NOT NULL DEFAULT 0.0,
                        `difficulty`     REAL    NOT NULL DEFAULT 5.0,
                        `elapsedDays`    INTEGER NOT NULL DEFAULT 0,
                        `scheduledDays`  INTEGER NOT NULL DEFAULT 0,
                        `reps`           INTEGER NOT NULL DEFAULT 0,
                        `lapses`         INTEGER NOT NULL DEFAULT 0,
                        `state`          TEXT    NOT NULL DEFAULT 'NEW',
                        `dueDate`        TEXT    NOT NULL,
                        `lastReviewDate` TEXT,
                        PRIMARY KEY(`userId`, `exerciseId`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dictation_progress_userId` ON `dictation_progress` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dictation_progress_userId_dueDate` ON `dictation_progress` (`userId`, `dueDate`)")

                // ─── Grammar Lessons ────────────────────────────────────────
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `grammar_lessons` (
                        `id`           INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `levelId`      INTEGER NOT NULL,
                        `grammarTopic` TEXT    NOT NULL,
                        `title`        TEXT    NOT NULL,
                        `explanation`  TEXT    NOT NULL,
                        `formula`      TEXT,
                        `sortOrder`    INTEGER NOT NULL DEFAULT 0,
                        `iconEmoji`    TEXT    NOT NULL DEFAULT '📚'
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_grammar_lessons_levelId` ON `grammar_lessons` (`levelId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_grammar_lessons_grammarTopic` ON `grammar_lessons` (`grammarTopic`)")

                // ─── Grammar Examples ───────────────────────────────────────
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `grammar_examples` (
                        `id`          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `lessonId`    INTEGER NOT NULL,
                        `sentence`    TEXT    NOT NULL,
                        `translation` TEXT    NOT NULL,
                        `highlight`   TEXT,
                        `note`        TEXT,
                        `isCorrect`   INTEGER NOT NULL DEFAULT 1,
                        `sortOrder`   INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`lessonId`) REFERENCES `grammar_lessons`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_grammar_examples_lessonId` ON `grammar_examples` (`lessonId`)")

                // ─── Grammar Progress ───────────────────────────────────────
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `grammar_progress` (
                        `userId`      INTEGER NOT NULL,
                        `lessonId`    INTEGER NOT NULL,
                        `isViewed`    INTEGER NOT NULL DEFAULT 0,
                        `isCompleted` INTEGER NOT NULL DEFAULT 0,
                        `viewedAt`    INTEGER,
                        `completedAt` INTEGER,
                        PRIMARY KEY(`userId`, `lessonId`),
                        FOREIGN KEY(`lessonId`) REFERENCES `grammar_lessons`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_grammar_progress_userId` ON `grammar_progress` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_grammar_progress_lessonId` ON `grammar_progress` (`lessonId`)")

                // ─── Pronunciation Results ──────────────────────────────────
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pronunciation_results` (
                        `id`               INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `userId`           INTEGER NOT NULL,
                        `exerciseId`       INTEGER NOT NULL,
                        `exerciseType`     TEXT    NOT NULL,
                        `expectedText`     TEXT    NOT NULL,
                        `recognizedText`   TEXT    NOT NULL,
                        `overallScore`     INTEGER NOT NULL,
                        `accuracyScore`    INTEGER NOT NULL,
                        `fluencyScore`     INTEGER NOT NULL,
                        `completenessScore` INTEGER NOT NULL,
                        `errorWords`       TEXT,
                        `omittedWords`     TEXT,
                        `insertedWords`    TEXT,
                        `audioDurationMs`  INTEGER,
                        `confidence`       REAL,
                        `timestamp`        INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pronunciation_results_userId` ON `pronunciation_results` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pronunciation_results_userId_timestamp` ON `pronunciation_results` (`userId`, `timestamp`)")

                // ─── Pronunciation Stats ────────────────────────────────────
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pronunciation_stats` (
                        `userId`        INTEGER NOT NULL,
                        `totalAttempts` INTEGER NOT NULL DEFAULT 0,
                        `averageScore`  REAL    NOT NULL DEFAULT 0,
                        `bestScore`     INTEGER NOT NULL DEFAULT 0,
                        `commonErrors`  TEXT,
                        `lastUpdated`   INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`userId`)
                    )
                """.trimIndent())

                // ─── Sync Metadata ──────────────────────────────────────────
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sync_metadata` (
                        `id`              INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `tableName`       TEXT    NOT NULL,
                        `localId`         INTEGER NOT NULL,
                        `remoteId`        TEXT,
                        `syncStatus`      TEXT    NOT NULL DEFAULT 'PENDING',
                        `localModifiedAt` INTEGER NOT NULL DEFAULT 0,
                        `lastSyncedAt`    INTEGER,
                        `contentHash`     TEXT,
                        `errorMessage`    TEXT,
                        `retryCount`      INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_metadata_tableName` ON `sync_metadata` (`tableName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_metadata_syncStatus` ON `sync_metadata` (`syncStatus`)")

                // ─── Sync Settings ──────────────────────────────────────────
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sync_settings` (
                        `userId`              INTEGER NOT NULL,
                        `isEnabled`           INTEGER NOT NULL DEFAULT 0,
                        `wifiOnly`            INTEGER NOT NULL DEFAULT 1,
                        `autoSync`            INTEGER NOT NULL DEFAULT 1,
                        `syncIntervalMinutes` INTEGER NOT NULL DEFAULT 30,
                        `cloudUserId`         TEXT,
                        `provider`            TEXT,
                        `authToken`           TEXT,
                        `lastFullSyncAt`      INTEGER,
                        `updatedAt`           INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`userId`)
                    )
                """.trimIndent())

                // ─── Sync Log ───────────────────────────────────────────────
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sync_log` (
                        `id`          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `operation`   TEXT    NOT NULL,
                        `tables`      TEXT    NOT NULL,
                        `recordCount` INTEGER NOT NULL,
                        `status`      TEXT    NOT NULL,
                        `details`     TEXT,
                        `durationMs`  INTEGER NOT NULL,
                        `timestamp`   INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_log_timestamp` ON `sync_log` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_log_operation` ON `sync_log` (`operation`)")
            }
        }

        /**
         * Migração da versão 6 para a versão 7.
         * 
         * Refatora a tabela dictation_exercises para o estilo Callan Method:
         * - Muda de levelId para stage (1-12)
         * - Muda de sentence (frases curtas) para text (parágrafos)
         * - Adiciona title, wordCount, grammarFocus, completed
         * - Remove sourceDrillId, difficulty, keywords
         * 
         * Como a estrutura muda completamente, dropamos e recriamos a tabela.
         * Os dados serão re-seedados pelo DatabaseSeeder.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop tabela antiga
                db.execSQL("DROP TABLE IF EXISTS `dictation_exercises`")
                
                // Criar nova tabela com schema Callan Method
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `dictation_exercises` (
                        `id`           INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `stage`        INTEGER NOT NULL,
                        `title`        TEXT    NOT NULL,
                        `text`         TEXT    NOT NULL,
                        `translation`  TEXT,
                        `wordCount`    INTEGER NOT NULL DEFAULT 0,
                        `grammarFocus` TEXT,
                        `completed`    INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                
                // Índices para a nova tabela
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dictation_exercises_stage` ON `dictation_exercises` (`stage`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dictation_exercises_completed` ON `dictation_exercises` (`completed`)")
                
                // Também precisamos resetar o progresso de ditado, já que os exerciseIds mudam
                db.execSQL("DELETE FROM `dictation_progress`")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Adiciona colunas para suporte ao modo ANSWER_FROM_QUESTION
                db.execSQL(
                    "ALTER TABLE `exercises` ADD COLUMN `prompt_text` TEXT"
                )
                db.execSQL(
                    "ALTER TABLE `exercises` ADD COLUMN `exercise_mode` TEXT NOT NULL DEFAULT 'QUESTION_FROM_ANSWER'"
                )
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Tabela de SRS cards
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `srs_cards` (
                        `id`               INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `user_id`          INTEGER NOT NULL,
                        `card_type`        TEXT    NOT NULL,
                        `ref_id`           INTEGER NOT NULL,
                        `stability`        REAL    NOT NULL DEFAULT 0.0,
                        `difficulty`       REAL    NOT NULL DEFAULT 5.0,
                        `elapsed_days`     INTEGER NOT NULL DEFAULT 0,
                        `scheduled_days`   INTEGER NOT NULL DEFAULT 0,
                        `reps`             INTEGER NOT NULL DEFAULT 0,
                        `lapses`           INTEGER NOT NULL DEFAULT 0,
                        `state`            TEXT    NOT NULL DEFAULT 'NEW',
                        `due_date`         TEXT    NOT NULL,
                        `last_review_date` TEXT
                    )
                    """.trimIndent()
                )

                // Índices de srs_cards
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_srs_cards_user_id` ON `srs_cards` (`user_id`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_srs_cards_ref_id` ON `srs_cards` (`ref_id`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_srs_cards_due_date` ON `srs_cards` (`due_date`)"
                )

                // Tabela de vocabulary cards
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `vocabulary_cards` (
                        `id`               INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `word`             TEXT    NOT NULL,
                        `definition`       TEXT    NOT NULL,
                        `example_sentence` TEXT    NOT NULL,
                        `level_id`         INTEGER NOT NULL,
                        `category_id`      INTEGER NOT NULL,
                        `audio_path`       TEXT,
                        FOREIGN KEY(`level_id`)    REFERENCES `levels`(`id`)     ON DELETE CASCADE,
                        FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                // Índices de vocabulary_cards
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_vocabulary_cards_level_id` ON `vocabulary_cards` (`level_id`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_vocabulary_cards_category_id` ON `vocabulary_cards` (`category_id`)"
                )
            }
        }
    }

    /** DAO para operações de leitura/escrita de níveis de dificuldade. */
    abstract fun levelDao(): LevelDao

    /** DAO para operações de leitura/escrita de categorias temáticas. */
    abstract fun categoryDao(): CategoryDao

    /** DAO para operações de leitura/escrita de exercícios. */
    abstract fun exerciseDao(): ExerciseDao

    /** DAO para operações de leitura/escrita de respostas aceitas. */
    abstract fun acceptedAnswerDao(): AcceptedAnswerDao

    /** DAO para operações de leitura/escrita de dicas progressivas. */
    abstract fun hintDao(): HintDao

    /** DAO para progresso do usuário por nível/categoria e registro de tentativas. */
    abstract fun progressDao(): ProgressDao

    /** DAO para leitura e atualização do streak diário. */
    abstract fun streakDao(): StreakDao

    /** DAO para conquistas desbloqueadas pelo usuário. */
    abstract fun achievementDao(): AchievementDao

    /** DAO dedicado ao perfil do usuário. */
    abstract fun userProfileDao(): UserProfileDao

    /** DAO para SRS cards com parâmetros FSRS-4.5. Requisito 16.2. */
    abstract fun srsDao(): SrsDao

    /** DAO para vocabulary cards do sistema SRS. Requisito 16.9. */
    abstract fun vocabularyDao(): VocabularyDao

    /** DAO para desafios diários. */
    abstract fun dailyChallengeDao(): DailyChallengeDao

    /** DAO para conversation chains. */
    abstract fun conversationChainDao(): ConversationChainDao

    // ═══════════════════════════════════════════════════════════════════════
    // v1.28.0 DAOs
    // ═══════════════════════════════════════════════════════════════════════

    /** DAO para flashcards de vocabulário. */
    abstract fun flashcardDao(): FlashcardDao

    /** DAO para exercícios de ditado. */
    abstract fun dictationDao(): DictationDao

    /** DAO para lições de gramática. */
    abstract fun grammarLessonDao(): GrammarLessonDao

    /** DAO para avaliação de pronúncia. */
    abstract fun pronunciationDao(): PronunciationDao

    /** DAO para sincronização com a nuvem. */
    abstract fun syncDao(): SyncDao
}
