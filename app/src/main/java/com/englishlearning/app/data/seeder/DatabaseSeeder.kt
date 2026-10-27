package com.englishlearning.app.data.seeder

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.englishlearning.app.data.database.EnglishLearningDatabase
import com.englishlearning.app.data.database.entity.AcceptedAnswerEntity
import com.englishlearning.app.data.database.entity.CategoryEntity
import com.englishlearning.app.data.database.entity.DictationExerciseEntity
import com.englishlearning.app.data.database.entity.ExerciseEntity
import com.englishlearning.app.data.database.entity.GrammarExampleEntity
import com.englishlearning.app.data.database.entity.GrammarLessonEntity
import com.englishlearning.app.data.database.entity.HintEntity
import com.englishlearning.app.data.database.entity.LevelEntity
import com.englishlearning.app.data.database.entity.VocabularyCardEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Responsável por popular o banco de dados Room com conteúdo dos assets JSON
 * na primeira inicialização do aplicativo.
 *
 * Comportamento:
 * - Verifica a flag `db_seeded` no SharedPreferences antes de executar.
 * - Lê `assets/content/levels.json` e popula a tabela levels.
 * - Lê `assets/content/categories.json` e popula a tabela categories.
 * - Lê `assets/content/exercises/level_1.json`, `level_2.json`, etc. e popula exercises,
 *   accepted_answers e hints.
 * - Lê `assets/content/vocabulary/level_N_vocab.json` e popula vocabulary_cards.
 * - Em caso de arquivo JSON malformado, registra o erro e continua (req 11.4).
 * - Ao concluir com sucesso, marca `db_seeded = true` no SharedPreferences.
 *
 * Requisito 9.2: popular Room com conteúdo JSON na primeira inicialização.
 * Requisito 11.1: app lê arquivos JSON dos assets via DatabaseSeeder.
 * Requisito 11.2: serialização via kotlinx.serialization.
 * Requisito 11.4: arquivos malformados são logados e pulados.
 * Requisito 11.5: suporte a múltiplos arquivos por nível.
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: EnglishLearningDatabase
) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Executa o seeding apenas se ainda não foi realizado (flag `db_seeded` ausente ou false).
     * Idempotente: chamadas subsequentes retornam imediatamente.
     *
     * Requisito 11.1: primeira inicialização popula o banco.
     */
    suspend fun seedIfNeeded() {
        val isFullySeeded = prefs.getBoolean(KEY_DB_SEEDED, false)
        
        if (!isFullySeeded) {
            Log.i(TAG, "Starting database seeding...")
            try {
                seedLevels()
                seedCategories()
                seedExercises()
                seedVocabulary()
                seedGrammarLessons()
                seedDictationExercises()
                prefs.edit().putBoolean(KEY_DB_SEEDED, true).apply()
                Log.i(TAG, "Database seeding completed successfully.")
            } catch (e: Exception) {
                // Seeding failure is non-fatal: the app will retry on the next launch.
                Log.e(TAG, "Database seeding failed — will retry on next launch.", e)
            }
        } else {
            Log.d(TAG, "Database already seeded — checking for missing data.")
            // Verifica se a tabela de dictation_exercises precisa ser re-seedada
            // (pode acontecer após migração que dropa a tabela)
            checkAndSeedDictationIfEmpty()
        }
    }
    
    /**
     * Verifica se a tabela de exercícios de ditado está vazia e seed se necessário.
     * Isso é útil após migrações que dropam e recriam a tabela.
     */
    private suspend fun checkAndSeedDictationIfEmpty() {
        try {
            val count = db.dictationDao().getTotalExerciseCount()
            if (count == 0) {
                Log.i(TAG, "Dictation exercises table is empty — reseeding...")
                seedDictationExercises()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking/seeding dictation exercises: ${e.message}", e)
        }
    }

    // -----------------------------------------------------------------------
    // Internal seeding helpers
    // -----------------------------------------------------------------------

    /**
     * Lê `assets/content/levels.json` e insere os níveis no banco.
     * Requisito 6.1.
     */
    private suspend fun seedLevels() {
        val levelsJson = readAsset(LEVELS_ASSET_PATH) ?: run {
            Log.w(TAG, "levels.json not found in assets — skipping levels seeding.")
            return
        }

        val levels = try {
            json.decodeFromString<List<LevelJsonModel>>(levelsJson)
        } catch (e: SerializationException) {
            // Req 11.4: malformed file → log and continue
            Log.e(TAG, "Failed to parse levels.json: ${e.message}", e)
            return
        }

        val entities = levels.map { it.toEntity() }
        db.levelDao().insertAll(entities)
        Log.d(TAG, "Seeded ${entities.size} levels.")
    }

    /**
     * Lê `assets/content/categories.json` e insere as categorias no banco.
     * Requisito 7.1.
     */
    private suspend fun seedCategories() {
        val categoriesJson = readAsset(CATEGORIES_ASSET_PATH) ?: run {
            Log.w(TAG, "categories.json not found in assets — skipping categories seeding.")
            return
        }

        val categories = try {
            json.decodeFromString<List<CategoryJsonModel>>(categoriesJson)
        } catch (e: SerializationException) {
            // Req 11.4: malformed file → log and continue
            Log.e(TAG, "Failed to parse categories.json: ${e.message}", e)
            return
        }

        val entities = categories.map { it.toEntity() }
        db.categoryDao().insertAll(entities)
        Log.d(TAG, "Seeded ${entities.size} categories.")
    }

    /**
     * Lê arquivos de exercícios de `assets/content/exercises/` para cada nível
     * disponível (level_1.json, level_2.json, …).
     *
     * Requisito 11.5: suporte a múltiplos arquivos JSON organizados por nível.
     * Requisito 11.4: arquivo malformado → log e continua com os demais.
     */
    private suspend fun seedExercises() {
        val exerciseFiles = listExerciseAssets()

        if (exerciseFiles.isEmpty()) {
            Log.w(TAG, "No exercise files found in $EXERCISES_ASSET_DIR.")
            return
        }

        for (fileName in exerciseFiles) {
            val assetPath = "$EXERCISES_ASSET_DIR/$fileName"
            seedExercisesFromFile(assetPath)
        }
    }

    /**
     * Processa um único arquivo de exercícios.
     * Em caso de erro de serialização, registra e retorna sem lançar exceção (req 11.4).
     */
    private suspend fun seedExercisesFromFile(assetPath: String) {
        val fileContent = readAsset(assetPath) ?: run {
            Log.w(TAG, "Exercise file not readable: $assetPath — skipping.")
            return
        }

        val exercises: List<ExerciseJsonModel> = try {
            json.decodeFromString(fileContent)
        } catch (e: SerializationException) {
            // Req 11.4: malformed JSON → log and continue with remaining files
            Log.e(TAG, "Failed to parse exercise file '$assetPath': ${e.message}", e)
            return
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid data in exercise file '$assetPath': ${e.message}", e)
            return
        }

        val exerciseEntities = exercises.map { it.toEntity() }
        db.exerciseDao().insertAll(exerciseEntities)

        // Insert accepted answers, using the exercise's JSON id to link them.
        val answerEntities = exercises.flatMap { exercise ->
            exercise.acceptedAnswers.map { it.toEntity(exerciseId = exercise.id) }
        }
        if (answerEntities.isNotEmpty()) {
            db.acceptedAnswerDao().insertAll(answerEntities)
        }

        // Insert hints, using the exercise's JSON id to link them.
        val hintEntities = exercises.flatMap { exercise ->
            exercise.hints.map { it.toEntity(exerciseId = exercise.id) }
        }
        if (hintEntities.isNotEmpty()) {
            db.hintDao().insertAll(hintEntities)
        }

        Log.d(TAG, "Seeded ${exerciseEntities.size} exercises from '$assetPath'.")
    }

    // -----------------------------------------------------------------------
    // Vocabulary seeding
    // -----------------------------------------------------------------------

    /**
     * Lê arquivos de vocabulary de `assets/content/vocabulary/` para cada nível
     * disponível (level_N_vocab.json) e insere os VocabularyCardEntity no banco.
     *
     * Verifica se já existem vocabulary cards antes de inserir para evitar duplicatas,
     * usando o mesmo padrão de idempotência já aplicado aos exercícios (flag KEY_DB_SEEDED).
     *
     * Requisito 16.9: campos word, definition (PT-BR), exampleSentence, levelId, categoryId.
     * Requisito 16.10: VocabularyCards disponíveis para o sistema SRS.
     */
    private suspend fun seedVocabulary() {
        val vocabFiles = listVocabularyAssets()

        if (vocabFiles.isEmpty()) {
            Log.w(TAG, "No vocabulary files found in $VOCABULARY_ASSET_DIR — skipping vocabulary seeding.")
            return
        }

        for (fileName in vocabFiles) {
            val assetPath = "$VOCABULARY_ASSET_DIR/$fileName"
            seedVocabularyFromFile(assetPath)
        }
    }

    /**
     * Processa um único arquivo de vocabulary cards.
     * Em caso de erro de serialização, registra e retorna sem lançar exceção (req 11.4).
     */
    private suspend fun seedVocabularyFromFile(assetPath: String) {
        val fileContent = readAsset(assetPath) ?: run {
            Log.w(TAG, "Vocabulary file not readable: $assetPath — skipping.")
            return
        }

        val vocabFile: VocabularyFileJsonModel = try {
            json.decodeFromString(fileContent)
        } catch (e: SerializationException) {
            // Req 11.4: malformed JSON → log and continue with remaining files
            Log.e(TAG, "Failed to parse vocabulary file '$assetPath': ${e.message}", e)
            return
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid data in vocabulary file '$assetPath': ${e.message}", e)
            return
        }

        val entities = vocabFile.cards.map { it.toEntity() }
        if (entities.isNotEmpty()) {
            db.vocabularyDao().insertAll(entities)
            Log.d(TAG, "Seeded ${entities.size} vocabulary cards from '$assetPath'.")
        }
    }

    /**
     * Lista os arquivos de vocabulary disponíveis em `assets/content/vocabulary/`.
     * Retorna apenas arquivos com padrão `level_N_vocab.json`.
     */
    private fun listVocabularyAssets(): List<String> {
        return try {
            context.assets.list(VOCABULARY_ASSET_DIR)
                ?.filter { it.matches(VOCAB_FILE_PATTERN) }
                ?.sorted()
                ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Could not list vocabulary assets directory '$VOCABULARY_ASSET_DIR': ${e.message}")
            emptyList()
        }
    }

    // -----------------------------------------------------------------------
    // Grammar Lessons seeding (v1.28.0)
    // -----------------------------------------------------------------------

    /**
     * Lê `assets/content/grammar_lessons.json` e insere as lições de gramática no banco.
     */
    private suspend fun seedGrammarLessons() {
        val grammarJson = readAsset(GRAMMAR_LESSONS_ASSET_PATH) ?: run {
            Log.w(TAG, "grammar_lessons.json not found in assets — skipping grammar seeding.")
            return
        }

        val lessons: List<GrammarLessonJsonModel> = try {
            json.decodeFromString(grammarJson)
        } catch (e: SerializationException) {
            Log.e(TAG, "Failed to parse grammar_lessons.json: ${e.message}", e)
            return
        }

        // Insert lessons
        val lessonEntities = lessons.map { lesson ->
            GrammarLessonEntity(
                id = lesson.id,
                levelId = lesson.levelId,
                grammarTopic = lesson.grammarTopic,
                title = lesson.title,
                explanation = lesson.explanation,
                formula = lesson.formula,
                sortOrder = lesson.sortOrder,
                iconEmoji = lesson.iconEmoji
            )
        }
        db.grammarLessonDao().insertLessons(lessonEntities)

        // Insert examples for each lesson
        var exampleId = 1L
        for (lesson in lessons) {
            val exampleEntities = lesson.examples.mapIndexed { index, example ->
                GrammarExampleEntity(
                    id = exampleId++,
                    lessonId = lesson.id,
                    sentence = example.sentence,
                    translation = example.translation,
                    highlight = example.highlight,
                    note = example.note,
                    isCorrect = example.isCorrect,
                    sortOrder = index
                )
            }
            if (exampleEntities.isNotEmpty()) {
                db.grammarLessonDao().insertExamples(exampleEntities)
            }
        }

        Log.d(TAG, "Seeded ${lessonEntities.size} grammar lessons.")
    }

    // -----------------------------------------------------------------------
    // Dictation Exercises seeding (v1.30.0) - Callan Method Style
    // -----------------------------------------------------------------------

    /**
     * Lê `assets/content/dictation_exercises.json` e insere os exercícios de ditado no banco.
     * Formato Callan: textos completos organizados por Stage (1-12).
     */
    private suspend fun seedDictationExercises() {
        val dictationJson = readAsset(DICTATION_EXERCISES_ASSET_PATH) ?: run {
            Log.w(TAG, "dictation_exercises.json not found in assets — skipping dictation seeding.")
            return
        }

        val exercises: List<DictationExerciseJsonModel> = try {
            json.decodeFromString(dictationJson)
        } catch (e: SerializationException) {
            Log.e(TAG, "Failed to parse dictation_exercises.json: ${e.message}", e)
            return
        }

        val entities = exercises.map { exercise ->
            DictationExerciseEntity(
                id = exercise.id,
                stage = exercise.stage,
                title = exercise.title,
                text = exercise.text,
                segments = exercise.segments,
                translation = exercise.translation,
                wordCount = exercise.wordCount,
                grammarFocus = exercise.grammarFocus,
                completed = false
            )
        }
        db.dictationDao().insertExercises(entities)

        Log.d(TAG, "Seeded ${entities.size} dictation exercises (Callan style).")
    }

    // -----------------------------------------------------------------------
    // Asset helpers
    // -----------------------------------------------------------------------

    /**
     * Lê o conteúdo de um asset pelo caminho relativo.
     * Retorna null se o arquivo não existir ou não puder ser lido.
     */
    private fun readAsset(path: String): String? {
        return try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.w(TAG, "Asset not found or unreadable: $path")
            null
        }
    }

    /**
     * Lista os arquivos de exercícios disponíveis em `assets/content/exercises/`.
     * Retorna apenas arquivos com padrão `level_N.json`.
     * Requisito 11.5.
     */
    private fun listExerciseAssets(): List<String> {
        return try {
            context.assets.list(EXERCISES_ASSET_DIR)
                ?.filter { it.matches(LEVEL_FILE_PATTERN) }
                ?.sorted()
                ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Could not list exercise assets directory '$EXERCISES_ASSET_DIR': ${e.message}")
            emptyList()
        }
    }

    // -----------------------------------------------------------------------
    // Mapping extensions
    // -----------------------------------------------------------------------

    private fun LevelJsonModel.toEntity() = LevelEntity(
        id = id,
        nameKey = nameKey,
        descriptionKey = descriptionKey,
        minScore = minScore,
        maxScore = maxScore,
        isUnlocked = isUnlocked,
        sortOrder = sortOrder
    )

    private fun CategoryJsonModel.toEntity() = CategoryEntity(
        id = id,
        levelId = levelId,
        nameKey = nameKey,
        descriptionKey = descriptionKey,
        icon = icon,
        themeColorHex = themeColorHex,
        sortOrder = sortOrder
    )

    private fun ExerciseJsonModel.toEntity() = ExerciseEntity(
        id = id,
        levelId = levelId,
        categoryId = categoryId,
        answerText = answerText,
        promptText = promptText,
        contextText = contextText,
        questionType = questionType,
        exerciseMode = exerciseMode,
        grammarFocus = grammarFocus,
        difficultyScore = difficultyScore,
        xpReward = xpReward,
        sortOrder = sortOrder
    )

    private fun AcceptedAnswerJsonModel.toEntity(exerciseId: Long) = AcceptedAnswerEntity(
        id = if (id != 0L) id else 0L,
        exerciseId = exerciseId,
        questionText = questionText,
        isPrimary = isPrimary,
        matchThreshold = matchThreshold
    )

    private fun HintJsonModel.toEntity(exerciseId: Long) = HintEntity(
        id = if (id != 0L) id else 0L,
        exerciseId = exerciseId,
        hintOrder = hintOrder,
        hintText = hintText,
        hintType = hintType
    )

    private fun VocabularyJsonModel.toEntity() = VocabularyCardEntity(
        // id is auto-generated by Room
        word = word,
        definition = definition,
        exampleSentence = exampleSentence,
        levelId = levelId,
        categoryId = categoryId,
        audioPath = audioPath,
        imageUrl = imageUrl
    )

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    companion object {
        private const val TAG = "DatabaseSeeder"
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_DB_SEEDED = "db_seeded"
        private const val LEVELS_ASSET_PATH = "content/levels.json"
        private const val CATEGORIES_ASSET_PATH = "content/categories.json"
        private const val EXERCISES_ASSET_DIR = "content/exercises"
        private const val VOCABULARY_ASSET_DIR = "content/vocabulary"
        private const val GRAMMAR_LESSONS_ASSET_PATH = "content/grammar_lessons.json"
        private const val DICTATION_EXERCISES_ASSET_PATH = "content/dictation_exercises.json"
        private val LEVEL_FILE_PATTERN = Regex("^level_\\d+\\.json$")
        private val VOCAB_FILE_PATTERN = Regex("^level_\\d+_vocab\\.json$")
    }
}
