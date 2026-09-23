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
import com.englishlearning.app.data.database.dao.ExerciseDao
import com.englishlearning.app.data.database.dao.HintDao
import com.englishlearning.app.data.database.dao.LevelDao
import com.englishlearning.app.data.database.dao.ProgressDao
import com.englishlearning.app.data.database.dao.SrsDao
import com.englishlearning.app.data.database.dao.StreakDao
import com.englishlearning.app.data.database.dao.UserProfileDao
import com.englishlearning.app.data.database.dao.VocabularyDao
import com.englishlearning.app.data.database.entity.AcceptedAnswerEntity
import com.englishlearning.app.data.database.entity.AchievementEntity
import com.englishlearning.app.data.database.entity.AttemptEntity
import com.englishlearning.app.data.database.entity.CategoryEntity
import com.englishlearning.app.data.database.entity.ExerciseEntity
import com.englishlearning.app.data.database.entity.HintEntity
import com.englishlearning.app.data.database.entity.LevelEntity
import com.englishlearning.app.data.database.entity.SrsCardEntity
import com.englishlearning.app.data.database.entity.StreakRecordEntity
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
        VocabularyCardEntity::class
    ],
    version = 4,
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
}
