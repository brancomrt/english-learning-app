package com.englishlearning.app.testutil

import android.content.SharedPreferences
import com.englishlearning.app.domain.model.*
import com.englishlearning.app.domain.repository.ContentRepository
import com.englishlearning.app.domain.repository.ProgressRepository
import com.englishlearning.app.domain.repository.SrsRepository
import com.englishlearning.app.domain.validation.QuestionValidationEngine
import com.englishlearning.app.domain.gamification.SrsRating
import com.englishlearning.app.domain.gamification.SrsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

// ─────────────────────────────────────────────────────────────────────────────
// FakeSharedPreferences — in-memory SharedPreferences sem Android runtime
// ─────────────────────────────────────────────────────────────────────────────

class FakeSharedPreferences : SharedPreferences {
    val store = mutableMapOf<String, Any?>()
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): Map<String, *> = store.toMap()
    override fun getString(key: String, defValue: String?): String? =
        store.getOrDefault(key, defValue) as? String ?: defValue
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
        @Suppress("UNCHECKED_CAST")
        store.getOrDefault(key, defValues) as? Set<String> ?: defValues
    override fun getInt(key: String, defValue: Int): Int =
        (store.getOrDefault(key, defValue) as? Int) ?: defValue
    override fun getLong(key: String, defValue: Long): Long =
        (store.getOrDefault(key, defValue) as? Long) ?: defValue
    override fun getFloat(key: String, defValue: Float): Float =
        (store.getOrDefault(key, defValue) as? Float) ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        (store.getOrDefault(key, defValue) as? Boolean) ?: defValue
    override fun contains(key: String): Boolean = store.containsKey(key)
    override fun edit(): SharedPreferences.Editor = FakeEditor(store, listeners, this)
    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) { listeners.add(listener) }
    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) { listeners.remove(listener) }

    private class FakeEditor(
        private val store: MutableMap<String, Any?>,
        private val listeners: Set<SharedPreferences.OnSharedPreferenceChangeListener>,
        private val prefs: SharedPreferences
    ) : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private val removes = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String, value: String?)   = apply { pending[key] = value }
        override fun putStringSet(key: String, values: Set<String>?) = apply { pending[key] = values }
        override fun putInt(key: String, value: Int)           = apply { pending[key] = value }
        override fun putLong(key: String, value: Long)         = apply { pending[key] = value }
        override fun putFloat(key: String, value: Float)       = apply { pending[key] = value }
        override fun putBoolean(key: String, value: Boolean)   = apply { pending[key] = value }
        override fun remove(key: String)                       = apply { removes.add(key) }
        override fun clear()                                   = apply { clearAll = true }
        override fun commit(): Boolean { applyChanges(); return true }
        override fun apply() { applyChanges() }

        private fun applyChanges() {
            if (clearAll) store.clear()
            removes.forEach { store.remove(it) }
            store.putAll(pending)
            pending.keys.forEach { key ->
                listeners.forEach { it.onSharedPreferenceChanged(prefs, key) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FakeContentRepository
// ─────────────────────────────────────────────────────────────────────────────

class FakeContentRepository : ContentRepository {
    var exercisesByLevel: Map<Int, List<Exercise>> = emptyMap()
    var exercisesByLevelAndCategory: Map<Pair<Int, Int>, List<Exercise>> = emptyMap()
    val callLog = mutableListOf<String>()

    override suspend fun getExercisesByLevel(levelId: Int): List<Exercise> {
        callLog.add("getByLevel($levelId)")
        return exercisesByLevel[levelId] ?: emptyList()
    }

    override suspend fun getExercisesByCategory(categoryId: Int): List<Exercise> {
        callLog.add("getByCategory($categoryId)")
        return emptyList()
    }

    override suspend fun getExercisesByLevelAndCategory(
        levelId: Int, categoryId: Int
    ): List<Exercise> {
        callLog.add("getByLevelAndCategory($levelId, $categoryId)")
        return exercisesByLevelAndCategory[levelId to categoryId] ?: emptyList()
    }

    override suspend fun getExerciseById(id: Long): Exercise? = null
    override suspend fun markExerciseCompleted(exerciseId: Long, userId: Long) {}
    override suspend fun getCategories(): List<Category> = emptyList()
    override suspend fun unlockLevel(levelId: Int) {}
    override suspend fun getLevels(): List<Level> = emptyList()
}

// ─────────────────────────────────────────────────────────────────────────────
// FakeProgressRepository
// ─────────────────────────────────────────────────────────────────────────────

class FakeProgressRepository : ProgressRepository {
    var updateProgressCallCount = 0
    var updateStreakCallCount = 0
    var lastAttemptResult: AttemptResult? = null
    var lastUserId: Long? = null

    override suspend fun updateProgress(userId: Long, attemptResult: AttemptResult) {
        updateProgressCallCount++
        lastUserId = userId
        lastAttemptResult = attemptResult
    }

    override suspend fun updateStreak(userId: Long): StreakRecord {
        updateStreakCallCount++
        return StreakRecord(userId = userId, currentStreak = 1, longestStreak = 1)
    }

    override suspend fun checkAchievements(userId: Long): List<Achievement> = emptyList()

    override suspend fun getLevelProgress(
        userId: Long, levelId: Int, categoryId: Int
    ): LevelProgress = LevelProgress(
        userId = userId, levelId = levelId, categoryId = categoryId,
        exercisesCompleted = 0, totalAttempts = 0, correctAnswers = 0, totalXpEarned = 0
    )

    override fun getOverallStats(userId: Long): Flow<UserStats> = flowOf(
        UserStats(
            userId = userId, totalXp = 0, totalExercisesCompleted = 0,
            totalCorrectAnswers = 0, currentStreak = 0, longestStreak = 0,
            currentLevelId = 1
        )
    )

    override fun observeAchievements(userId: Long): Flow<List<Achievement>> = flowOf(emptyList())
    override suspend fun markAchievementsAsSeen(userId: Long) {}
    override fun observeStreak(userId: Long): Flow<StreakRecord?> = flowOf(null)
}

// ─────────────────────────────────────────────────────────────────────────────
// FakeSrsRepository
// ─────────────────────────────────────────────────────────────────────────────

class FakeSrsRepository : SrsRepository {
    data class CardCreation(val userId: Long, val cardType: SrsCardType, val refId: Long)

    val cardsCreated = mutableListOf<CardCreation>()

    override suspend fun getCardsForReview(
        userId: Long, today: LocalDate
    ): List<SrsCard> = emptyList()

    override suspend fun getDueCardCount(userId: Long, today: LocalDate): Int = 0

    override suspend fun getOrCreateCard(
        userId: Long, cardType: SrsCardType, refId: Long
    ): SrsCard {
        cardsCreated.add(CardCreation(userId, cardType, refId))
        return SrsCard(
            id = cardsCreated.size.toLong(),
            userId = userId, cardType = cardType, refId = refId,
            state = SrsState.NEW, dueDate = today()
        )
    }

    override suspend fun updateCardAfterReview(card: SrsCard, rating: SrsRating): SrsCard = card
    override suspend fun getVocabularyCards(levelId: Int, categoryId: Int): List<VocabularyCard> =
        emptyList()
    override suspend fun getVocabularyCardById(id: Long): VocabularyCard? = null
    override fun observeDueCardCount(userId: Long): Flow<Int> = flowOf(0)

    private fun today() = LocalDate.now()
}

// ─────────────────────────────────────────────────────────────────────────────
// FakeValidationEngine
// ─────────────────────────────────────────────────────────────────────────────

class FakeValidationEngine(
    private val fixedResult: ValidationResult = ValidationResult(
        status = ValidationStatus.CORRECT,
        score = 100,
        matchedAnswer = null,
        errorType = null,
        corrections = emptyList(),
        hints = emptyList()
    )
) : QuestionValidationEngine {
    var lastUserQuestion: String? = null
    var lastExercise: Exercise? = null
    var callCount = 0

    override fun validate(userQuestion: String, exercise: Exercise): ValidationResult {
        callCount++
        lastUserQuestion = userQuestion
        lastExercise = exercise
        return fixedResult
    }

    override fun generateHints(userQuestion: String, exercise: Exercise): List<Hint> =
        emptyList()

    override fun scoreAnswer(userQuestion: String, exercise: Exercise): Int =
        fixedResult.score
}

// ─────────────────────────────────────────────────────────────────────────────
// Builder helpers
// ─────────────────────────────────────────────────────────────────────────────

fun makeExercise(
    id: Long = 1L,
    levelId: Int = 1,
    categoryId: Int = 1,
    questionType: QuestionType = QuestionType.WH_WHAT,
    difficultyScore: Int = 3,
    xpReward: Int = 10
) = Exercise(
    id = id, levelId = levelId, categoryId = categoryId,
    answerText = "It is a test.", contextText = null,
    questionType = questionType, grammarFocus = "test",
    difficultyScore = difficultyScore, xpReward = xpReward,
    acceptedAnswers = listOf(
        AcceptedAnswer(
            id = id * 10, exerciseId = id,
            questionText = "What is this?",
            isPrimary = true, matchThreshold = 0.85f
        )
    ),
    hints = emptyList()
)

fun makeAttemptResult(
    exerciseId: Long = 1L,
    userId: Long = 1L,
    status: ValidationStatus = ValidationStatus.CORRECT,
    score: Int = 100,
    xpEarned: Int = 100
) = AttemptResult(
    exerciseId = exerciseId,
    userId = userId,
    userAnswer = "What is this?",
    validationResult = ValidationResult(
        status = status, score = score,
        matchedAnswer = null, errorType = null,
        corrections = emptyList(), hints = emptyList()
    ),
    xpEarned = xpEarned
)

// ─────────────────────────────────────────────────────────────────────────────
// FakeContext — contexto mínimo que entrega um SharedPreferences específico
// ─────────────────────────────────────────────────────────────────────────────

class FakeContext(private val prefs: SharedPreferences) : android.content.Context() {
    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = prefs
    override fun getAssets() = throw UnsupportedOperationException()
    override fun getResources() = throw UnsupportedOperationException()
    override fun getPackageManager() = throw UnsupportedOperationException()
    override fun getContentResolver() = throw UnsupportedOperationException()
    override fun getMainLooper() = throw UnsupportedOperationException()
    override fun getApplicationContext() = throw UnsupportedOperationException()
    override fun setTheme(resid: Int) = throw UnsupportedOperationException()
    override fun getTheme() = throw UnsupportedOperationException()
    override fun getClassLoader() = throw UnsupportedOperationException()
    override fun getPackageName() = "com.englishlearning.app.test"
    override fun getApplicationInfo() = throw UnsupportedOperationException()
    override fun getPackageResourcePath() = throw UnsupportedOperationException()
    override fun getPackageCodePath() = throw UnsupportedOperationException()
    override fun getFilesDir() = throw UnsupportedOperationException()
    override fun getNoBackupFilesDir() = throw UnsupportedOperationException()
    override fun getExternalFilesDir(type: String?) = throw UnsupportedOperationException()
    override fun getExternalFilesDirs(type: String?) = throw UnsupportedOperationException()
    override fun getObbDir() = throw UnsupportedOperationException()
    override fun getObbDirs() = throw UnsupportedOperationException()
    override fun getCacheDir() = throw UnsupportedOperationException()
    override fun getCodeCacheDir() = throw UnsupportedOperationException()
    override fun getExternalCacheDir() = throw UnsupportedOperationException()
    override fun getExternalCacheDirs() = throw UnsupportedOperationException()
    override fun getExternalMediaDirs() = throw UnsupportedOperationException()
    override fun getDir(name: String?, mode: Int) = throw UnsupportedOperationException()
    override fun openFileInput(name: String?) = throw UnsupportedOperationException()
    override fun openFileOutput(name: String?, mode: Int) = throw UnsupportedOperationException()
    override fun deleteFile(name: String?) = throw UnsupportedOperationException()
    override fun getFileStreamPath(name: String?) = throw UnsupportedOperationException()
    override fun fileList() = throw UnsupportedOperationException()
    override fun getDataDir() = throw UnsupportedOperationException()
    override fun openOrCreateDatabase(n: String?, m: Int, f: android.database.sqlite.SQLiteDatabase.CursorFactory?) = throw UnsupportedOperationException()
    override fun openOrCreateDatabase(n: String?, m: Int, f: android.database.sqlite.SQLiteDatabase.CursorFactory?, e: android.database.DatabaseErrorHandler?) = throw UnsupportedOperationException()
    override fun moveDatabaseFrom(ctx: android.content.Context?, n: String?) = throw UnsupportedOperationException()
    override fun deleteDatabase(name: String?) = throw UnsupportedOperationException()
    override fun getDatabasePath(name: String?) = throw UnsupportedOperationException()
    override fun databaseList() = throw UnsupportedOperationException()
    override fun getWallpaper() = throw UnsupportedOperationException()
    override fun peekWallpaper() = throw UnsupportedOperationException()
    override fun getWallpaperDesiredMinimumWidth() = throw UnsupportedOperationException()
    override fun getWallpaperDesiredMinimumHeight() = throw UnsupportedOperationException()
    override fun setWallpaper(bitmap: android.graphics.Bitmap?) = throw UnsupportedOperationException()
    override fun setWallpaper(data: java.io.InputStream?) = throw UnsupportedOperationException()
    override fun clearWallpaper() = throw UnsupportedOperationException()
    override fun startActivity(intent: android.content.Intent?) = throw UnsupportedOperationException()
    override fun startActivity(intent: android.content.Intent?, opts: android.os.Bundle?) = throw UnsupportedOperationException()
    override fun startActivities(intents: Array<out android.content.Intent>?) = throw UnsupportedOperationException()
    override fun startActivities(intents: Array<out android.content.Intent>?, opts: android.os.Bundle?) = throw UnsupportedOperationException()
    override fun startIntentSender(intent: android.content.IntentSender?, fill: android.content.Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int) = throw UnsupportedOperationException()
    override fun startIntentSender(intent: android.content.IntentSender?, fill: android.content.Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int, opts: android.os.Bundle?) = throw UnsupportedOperationException()
    override fun sendBroadcast(intent: android.content.Intent?) = throw UnsupportedOperationException()
    override fun sendBroadcast(intent: android.content.Intent?, perm: String?) = throw UnsupportedOperationException()
    override fun sendOrderedBroadcast(intent: android.content.Intent?, perm: String?) = throw UnsupportedOperationException()
    override fun sendOrderedBroadcast(intent: android.content.Intent, perm: String?, r: android.content.BroadcastReceiver?, h: android.os.Handler?, c: Int, d: String?, e: android.os.Bundle?) = throw UnsupportedOperationException()
    override fun sendBroadcastAsUser(i: android.content.Intent, u: android.os.UserHandle) = throw UnsupportedOperationException()
    override fun sendBroadcastAsUser(i: android.content.Intent, u: android.os.UserHandle, p: String?) = throw UnsupportedOperationException()
    override fun sendOrderedBroadcastAsUser(i: android.content.Intent?, u: android.os.UserHandle?, p: String?, r: android.content.BroadcastReceiver?, h: android.os.Handler?, c: Int, d: String?, e: android.os.Bundle?) = throw UnsupportedOperationException()
    override fun sendStickyBroadcast(intent: android.content.Intent?) = throw UnsupportedOperationException()
    override fun sendStickyBroadcast(i: android.content.Intent, o: android.os.Bundle?) = throw UnsupportedOperationException()
    override fun sendStickyOrderedBroadcast(i: android.content.Intent?, r: android.content.BroadcastReceiver?, h: android.os.Handler?, c: Int, d: String?, e: android.os.Bundle?) = throw UnsupportedOperationException()
    override fun removeStickyBroadcast(intent: android.content.Intent?) = throw UnsupportedOperationException()
    override fun sendStickyBroadcastAsUser(i: android.content.Intent?, u: android.os.UserHandle?) = throw UnsupportedOperationException()
    override fun sendStickyOrderedBroadcastAsUser(i: android.content.Intent?, u: android.os.UserHandle?, r: android.content.BroadcastReceiver?, h: android.os.Handler?, c: Int, d: String?, e: android.os.Bundle?) = throw UnsupportedOperationException()
    override fun removeStickyBroadcastAsUser(i: android.content.Intent?, u: android.os.UserHandle?) = throw UnsupportedOperationException()
    override fun registerReceiver(r: android.content.BroadcastReceiver?, f: android.content.IntentFilter?): android.content.Intent? = throw UnsupportedOperationException()
    override fun registerReceiver(r: android.content.BroadcastReceiver?, f: android.content.IntentFilter?, fl: Int): android.content.Intent? = throw UnsupportedOperationException()
    override fun registerReceiver(r: android.content.BroadcastReceiver?, f: android.content.IntentFilter?, p: String?, h: android.os.Handler?): android.content.Intent? = throw UnsupportedOperationException()
    override fun registerReceiver(r: android.content.BroadcastReceiver?, f: android.content.IntentFilter?, p: String?, h: android.os.Handler?, fl: Int): android.content.Intent? = throw UnsupportedOperationException()
    override fun unregisterReceiver(r: android.content.BroadcastReceiver?) = throw UnsupportedOperationException()
    override fun startService(service: android.content.Intent?) = throw UnsupportedOperationException()
    override fun startForegroundService(service: android.content.Intent?) = throw UnsupportedOperationException()
    override fun stopService(service: android.content.Intent?) = throw UnsupportedOperationException()
    override fun bindService(s: android.content.Intent, c: android.content.ServiceConnection, fl: Int) = throw UnsupportedOperationException()
    override fun unbindService(c: android.content.ServiceConnection) = throw UnsupportedOperationException()
    override fun startInstrumentation(c: android.content.ComponentName, p: String?, a: android.os.Bundle?) = throw UnsupportedOperationException()
    override fun getSystemService(name: String): Any? = throw UnsupportedOperationException()
    override fun getSystemServiceName(c: Class<*>): String? = throw UnsupportedOperationException()
    override fun checkPermission(p: String, pid: Int, uid: Int) = throw UnsupportedOperationException()
    override fun checkCallingPermission(p: String) = throw UnsupportedOperationException()
    override fun checkCallingOrSelfPermission(p: String) = throw UnsupportedOperationException()
    override fun checkSelfPermission(p: String) = throw UnsupportedOperationException()
    override fun enforcePermission(p: String, pid: Int, uid: Int, m: String?) = throw UnsupportedOperationException()
    override fun enforceCallingPermission(p: String, m: String?) = throw UnsupportedOperationException()
    override fun enforceCallingOrSelfPermission(p: String, m: String?) = throw UnsupportedOperationException()
    override fun grantUriPermission(p: String?, u: android.net.Uri?, f: Int) = throw UnsupportedOperationException()
    override fun revokeUriPermission(u: android.net.Uri?, f: Int) = throw UnsupportedOperationException()
    override fun revokeUriPermission(p: String?, u: android.net.Uri?, f: Int) = throw UnsupportedOperationException()
    override fun checkUriPermission(u: android.net.Uri?, pid: Int, uid: Int, f: Int) = throw UnsupportedOperationException()
    override fun checkCallingUriPermission(u: android.net.Uri?, f: Int) = throw UnsupportedOperationException()
    override fun checkCallingOrSelfUriPermission(u: android.net.Uri?, f: Int) = throw UnsupportedOperationException()
    override fun checkUriPermission(u: android.net.Uri?, r: String?, w: String?, pid: Int, uid: Int, f: Int) = throw UnsupportedOperationException()
    override fun enforceUriPermission(u: android.net.Uri?, pid: Int, uid: Int, f: Int, m: String?) = throw UnsupportedOperationException()
    override fun enforceCallingUriPermission(u: android.net.Uri?, f: Int, m: String?) = throw UnsupportedOperationException()
    override fun enforceCallingOrSelfUriPermission(u: android.net.Uri?, f: Int, m: String?) = throw UnsupportedOperationException()
    override fun enforceUriPermission(u: android.net.Uri?, r: String?, w: String?, pid: Int, uid: Int, f: Int, m: String?) = throw UnsupportedOperationException()
    override fun createPackageContext(pkg: String?, fl: Int) = throw UnsupportedOperationException()
    override fun createContextForSplit(n: String?) = throw UnsupportedOperationException()
    override fun createConfigurationContext(c: android.content.res.Configuration) = throw UnsupportedOperationException()
    override fun createDisplayContext(d: android.view.Display) = throw UnsupportedOperationException()
    override fun createDeviceProtectedStorageContext() = throw UnsupportedOperationException()
    override fun isDeviceProtectedStorage() = throw UnsupportedOperationException()
    override fun moveSharedPreferencesFrom(ctx: android.content.Context?, n: String?) = throw UnsupportedOperationException()
    override fun deleteSharedPreferences(n: String?) = throw UnsupportedOperationException()
}
