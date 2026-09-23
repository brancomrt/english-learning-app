package com.englishlearning.app.ui.lesson

import android.content.SharedPreferences
import com.englishlearning.app.domain.model.QuestionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Testes unitários para [LessonViewModel].
 *
 * Usa uma implementação in-memory de [SharedPreferences] para evitar dependência
 * de Robolectric ou instrumentação Android, conforme suportado pelo projeto.
 *
 * Requisitos validados: 15.1, 15.2, 15.3, 15.7.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LessonViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var viewModel: LessonViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePrefs = FakeSharedPreferences()
        // Create ViewModel without Android context by using the internal prefsProvider override.
        viewModel = createTestViewModel(fakePrefs)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── shouldShowLesson ─────────────────────────────────────────────────────

    /**
     * Requisito 15.1 / 15.2: se a chave ainda não existe, a lição deve ser exibida.
     */
    @Test
    fun `shouldShowLesson returns true when key is absent`() {
        assertTrue(viewModel.shouldShowLesson(categoryId = 42))
    }

    /**
     * Requisito 15.3: se a lição já foi marcada como vista, não deve ser exibida novamente.
     */
    @Test
    fun `shouldShowLesson returns false after markLessonSeen is called`() {
        viewModel.markLessonSeen(categoryId = 1)
        assertFalse(viewModel.shouldShowLesson(categoryId = 1))
    }

    /**
     * Verificação de isolamento: marcar uma categoria como vista não afeta outras.
     */
    @Test
    fun `shouldShowLesson returns true for a different categoryId after marking another`() {
        viewModel.markLessonSeen(categoryId = 1)
        assertTrue(viewModel.shouldShowLesson(categoryId = 2))
    }

    // ── markLessonSeen ───────────────────────────────────────────────────────

    /**
     * Requisito 15.7: a chave `lesson_seen_{categoryId}` deve ser persistida como true.
     */
    @Test
    fun `markLessonSeen stores lesson_seen_{categoryId} = true in SharedPreferences`() {
        val categoryId = 5
        viewModel.markLessonSeen(categoryId)
        assertTrue(fakePrefs.getBoolean("lesson_seen_$categoryId", false))
    }

    /**
     * Chave exata no SharedPreferences deve seguir o padrão `lesson_seen_{categoryId}`.
     */
    @Test
    fun `markLessonSeen writes the correct key for each categoryId`() {
        listOf(0, 1, 99, 100, Int.MAX_VALUE).forEach { id ->
            val freshPrefs = FakeSharedPreferences()
            val vm = createTestViewModel(freshPrefs)
            vm.markLessonSeen(id)
            assertTrue(
                "Esperado lesson_seen_$id = true",
                freshPrefs.getBoolean("lesson_seen_$id", false)
            )
            assertFalse(
                "Nenhuma outra chave deve ter sido escrita",
                freshPrefs.getBoolean("lesson_seen_${id + 1}", false)
            )
        }
    }

    /**
     * Chamar markLessonSeen duas vezes é idempotente.
     */
    @Test
    fun `markLessonSeen is idempotent`() {
        viewModel.markLessonSeen(categoryId = 7)
        viewModel.markLessonSeen(categoryId = 7)
        assertFalse(viewModel.shouldShowLesson(categoryId = 7))
    }

    // ── getLessonContent ─────────────────────────────────────────────────────

    /**
     * Requisito 15.10: todos os 13 QuestionTypes devem retornar LessonContent não-nulo.
     */
    @Test
    fun `getLessonContent returns non-null for all 13 QuestionTypes`() {
        QuestionType.values().forEach { type ->
            assertNotNull(
                "getLessonContent($type) não deveria retornar null",
                viewModel.getLessonContent(type)
            )
        }
    }

    /**
     * O questionType do LessonContent deve corresponder ao tipo solicitado.
     */
    @Test
    fun `getLessonContent returns LessonContent with matching questionType`() {
        QuestionType.values().forEach { type ->
            val content = viewModel.getLessonContent(type)!!
            assertEquals(
                "questionType do LessonContent não corresponde para $type",
                type,
                content.questionType
            )
        }
    }

    /**
     * Cada LessonContent deve ter exemplos e fórmula não-vazios.
     */
    @Test
    fun `getLessonContent returns content with non-empty fields for all types`() {
        QuestionType.values().forEach { type ->
            val content = viewModel.getLessonContent(type)!!
            assertTrue("ruleExplanationPt de $type está vazio", content.ruleExplanationPt.isNotBlank())
            assertTrue("structuralFormula de $type está vazia", content.structuralFormula.isNotBlank())
            assertTrue("examples de $type está vazia", content.examples.isNotEmpty())
        }
    }

    // ── loadLesson / uiState ─────────────────────────────────────────────────

    /**
     * Estado inicial deve ter valores padrão.
     */
    @Test
    fun `initial uiState has default values`() {
        val state = viewModel.uiState.value
        assertNull(state.lessonContent)
        assertEquals(0, state.categoryId)
        assertNull(state.questionType)
    }

    /**
     * loadLesson deve atualizar categoryId, questionType e lessonContent no uiState.
     */
    @Test
    fun `loadLesson updates uiState with categoryId, questionType and lessonContent`() = runTest {
        val categoryId = 3
        val questionType = QuestionType.WH_WHAT

        viewModel.loadLesson(categoryId, questionType)

        val state = viewModel.uiState.value
        assertEquals(categoryId, state.categoryId)
        assertEquals(questionType, state.questionType)
        assertNotNull(state.lessonContent)
        assertEquals(questionType, state.lessonContent!!.questionType)
    }

    /**
     * loadLesson chamado para todos os 13 QuestionTypes deve sempre popular lessonContent.
     */
    @Test
    fun `loadLesson populates lessonContent for all 13 QuestionTypes`() = runTest {
        QuestionType.values().forEachIndexed { idx, type ->
            viewModel.loadLesson(categoryId = idx, questionType = type)
            val state = viewModel.uiState.value
            assertNotNull("lessonContent deve ser não-nulo para $type", state.lessonContent)
            assertEquals(type, state.lessonContent!!.questionType)
            assertEquals(idx, state.categoryId)
        }
    }

    /**
     * Successive calls to loadLesson should replace the previous state.
     */
    @Test
    fun `loadLesson overwrites previous state on subsequent calls`() = runTest {
        viewModel.loadLesson(categoryId = 1, questionType = QuestionType.YES_NO)
        viewModel.loadLesson(categoryId = 2, questionType = QuestionType.TAG)

        val state = viewModel.uiState.value
        assertEquals(2, state.categoryId)
        assertEquals(QuestionType.TAG, state.questionType)
        assertEquals(QuestionType.TAG, state.lessonContent!!.questionType)
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    /**
     * Creates a [LessonViewModel] wired to [sharedPreferences] via the internal [prefsProvider].
     * The primary Hilt constructor is never called in tests; we create the object directly and
     * override [prefsProvider] before any method is invoked.
     */
    private fun createTestViewModel(sharedPreferences: SharedPreferences): LessonViewModel {
        // Hilt uses the @Inject constructor which requires Context.
        // For unit tests we use a Kotlin trick: call the constructor via reflection-free
        // subclassing workaround — instead, we simply wire the prefsProvider lambda after
        // creation using the internal visibility exposed for testing.
        //
        // Because LessonViewModel holds `prefsProvider` as an internal `var`, we can swap it
        // before any test method calls prefs.
        val vm = TestLessonViewModel(sharedPreferences)
        return vm
    }
}

// ---------------------------------------------------------------------------
// Test infrastructure
// ---------------------------------------------------------------------------

/**
 * Subclass of [LessonViewModel] for unit tests.
 *
 * Kotlin doesn't allow calling a no-arg super when there is no no-arg primary constructor,
 * so we provide a minimal [FakeContext] that is never used (prefsProvider is overridden immediately).
 */
private class TestLessonViewModel(
    sharedPreferences: SharedPreferences
) : LessonViewModel(FakeContext()) {
    init {
        prefsProvider = { sharedPreferences }
    }
}

/**
 * A minimal fake [android.content.Context] that satisfies the compiler without requiring
 * Robolectric. Its methods are never called because [prefsProvider] is overridden in
 * [TestLessonViewModel.init] before any SharedPreferences access.
 */
private class FakeContext : android.content.Context() {
    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences =
        throw UnsupportedOperationException("Use prefsProvider override in tests")

    // region — abstract stubs (Android Context requires all abstract members implemented)
    override fun getAssets() = throw UnsupportedOperationException()
    override fun getResources() = throw UnsupportedOperationException()
    override fun getPackageManager() = throw UnsupportedOperationException()
    override fun getContentResolver() = throw UnsupportedOperationException()
    override fun getMainLooper() = throw UnsupportedOperationException()
    override fun getApplicationContext() = throw UnsupportedOperationException()
    override fun setTheme(resid: Int) = throw UnsupportedOperationException()
    override fun getTheme() = throw UnsupportedOperationException()
    override fun getClassLoader() = throw UnsupportedOperationException()
    override fun getPackageName() = throw UnsupportedOperationException()
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
    override fun openOrCreateDatabase(name: String?, mode: Int, factory: android.database.sqlite.SQLiteDatabase.CursorFactory?) = throw UnsupportedOperationException()
    override fun openOrCreateDatabase(name: String?, mode: Int, factory: android.database.sqlite.SQLiteDatabase.CursorFactory?, errorHandler: android.database.DatabaseErrorHandler?) = throw UnsupportedOperationException()
    override fun moveDatabaseFrom(sourceContext: android.content.Context?, name: String?) = throw UnsupportedOperationException()
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
    override fun startActivity(intent: android.content.Intent?, options: android.os.Bundle?) = throw UnsupportedOperationException()
    override fun startActivities(intents: Array<out android.content.Intent>?) = throw UnsupportedOperationException()
    override fun startActivities(intents: Array<out android.content.Intent>?, options: android.os.Bundle?) = throw UnsupportedOperationException()
    override fun startIntentSender(intent: android.content.IntentSender?, fillInIntent: android.content.Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int) = throw UnsupportedOperationException()
    override fun startIntentSender(intent: android.content.IntentSender?, fillInIntent: android.content.Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int, options: android.os.Bundle?) = throw UnsupportedOperationException()
    override fun sendBroadcast(intent: android.content.Intent?) = throw UnsupportedOperationException()
    override fun sendBroadcast(intent: android.content.Intent?, receiverPermission: String?) = throw UnsupportedOperationException()
    override fun sendOrderedBroadcast(intent: android.content.Intent?, receiverPermission: String?) = throw UnsupportedOperationException()
    override fun sendOrderedBroadcast(intent: android.content.Intent, receiverPermission: String?, resultReceiver: android.content.BroadcastReceiver?, scheduler: android.os.Handler?, initialCode: Int, initialData: String?, initialExtras: android.os.Bundle?) = throw UnsupportedOperationException()
    override fun sendBroadcastAsUser(intent: android.content.Intent, user: android.os.UserHandle) = throw UnsupportedOperationException()
    override fun sendBroadcastAsUser(intent: android.content.Intent, user: android.os.UserHandle, receiverPermission: String?) = throw UnsupportedOperationException()
    override fun moveSharedPreferencesFrom(sourceContext: android.content.Context?, name: String?) = throw UnsupportedOperationException()
    override fun deleteSharedPreferences(name: String?) = throw UnsupportedOperationException()
    override fun sendOrderedBroadcastAsUser(intent: android.content.Intent?, user: android.os.UserHandle?, receiverPermission: String?, resultReceiver: android.content.BroadcastReceiver?, scheduler: android.os.Handler?, initialCode: Int, initialData: String?, initialExtras: android.os.Bundle?) = throw UnsupportedOperationException()
    override fun sendStickyBroadcast(intent: android.content.Intent?) = throw UnsupportedOperationException()
    override fun sendStickyBroadcast(intent: android.content.Intent, options: android.os.Bundle?) = throw UnsupportedOperationException()
    override fun sendStickyOrderedBroadcast(intent: android.content.Intent?, resultReceiver: android.content.BroadcastReceiver?, scheduler: android.os.Handler?, initialCode: Int, initialData: String?, initialExtras: android.os.Bundle?) = throw UnsupportedOperationException()
    override fun removeStickyBroadcast(intent: android.content.Intent?) = throw UnsupportedOperationException()
    override fun sendStickyBroadcastAsUser(intent: android.content.Intent?, user: android.os.UserHandle?) = throw UnsupportedOperationException()
    override fun sendStickyOrderedBroadcastAsUser(intent: android.content.Intent?, user: android.os.UserHandle?, resultReceiver: android.content.BroadcastReceiver?, scheduler: android.os.Handler?, initialCode: Int, initialData: String?, initialExtras: android.os.Bundle?) = throw UnsupportedOperationException()
    override fun removeStickyBroadcastAsUser(intent: android.content.Intent?, user: android.os.UserHandle?) = throw UnsupportedOperationException()
    override fun registerReceiver(receiver: android.content.BroadcastReceiver?, filter: android.content.IntentFilter?): android.content.Intent? = throw UnsupportedOperationException()
    override fun registerReceiver(receiver: android.content.BroadcastReceiver?, filter: android.content.IntentFilter?, flags: Int): android.content.Intent? = throw UnsupportedOperationException()
    override fun registerReceiver(receiver: android.content.BroadcastReceiver?, filter: android.content.IntentFilter?, broadcastPermission: String?, scheduler: android.os.Handler?): android.content.Intent? = throw UnsupportedOperationException()
    override fun registerReceiver(receiver: android.content.BroadcastReceiver?, filter: android.content.IntentFilter?, broadcastPermission: String?, scheduler: android.os.Handler?, flags: Int): android.content.Intent? = throw UnsupportedOperationException()
    override fun unregisterReceiver(receiver: android.content.BroadcastReceiver?) = throw UnsupportedOperationException()
    override fun startService(service: android.content.Intent?) = throw UnsupportedOperationException()
    override fun startForegroundService(service: android.content.Intent?) = throw UnsupportedOperationException()
    override fun stopService(service: android.content.Intent?) = throw UnsupportedOperationException()
    override fun bindService(service: android.content.Intent, conn: android.content.ServiceConnection, flags: Int) = throw UnsupportedOperationException()
    override fun unbindService(conn: android.content.ServiceConnection) = throw UnsupportedOperationException()
    override fun startInstrumentation(className: android.content.ComponentName, profileFile: String?, arguments: android.os.Bundle?) = throw UnsupportedOperationException()
    override fun getSystemService(name: String): Any? = throw UnsupportedOperationException()
    override fun getSystemServiceName(serviceClass: Class<*>): String? = throw UnsupportedOperationException()
    override fun checkPermission(permission: String, pid: Int, uid: Int) = throw UnsupportedOperationException()
    override fun checkCallingPermission(permission: String) = throw UnsupportedOperationException()
    override fun checkCallingOrSelfPermission(permission: String) = throw UnsupportedOperationException()
    override fun checkSelfPermission(permission: String) = throw UnsupportedOperationException()
    override fun enforcePermission(permission: String, pid: Int, uid: Int, message: String?) = throw UnsupportedOperationException()
    override fun enforceCallingPermission(permission: String, message: String?) = throw UnsupportedOperationException()
    override fun enforceCallingOrSelfPermission(permission: String, message: String?) = throw UnsupportedOperationException()
    override fun grantUriPermission(toPackage: String?, uri: android.net.Uri?, modeFlags: Int) = throw UnsupportedOperationException()
    override fun revokeUriPermission(uri: android.net.Uri?, modeFlags: Int) = throw UnsupportedOperationException()
    override fun revokeUriPermission(toPackage: String?, uri: android.net.Uri?, modeFlags: Int) = throw UnsupportedOperationException()
    override fun checkUriPermission(uri: android.net.Uri?, pid: Int, uid: Int, modeFlags: Int) = throw UnsupportedOperationException()
    override fun checkCallingUriPermission(uri: android.net.Uri?, modeFlags: Int) = throw UnsupportedOperationException()
    override fun checkCallingOrSelfUriPermission(uri: android.net.Uri?, modeFlags: Int) = throw UnsupportedOperationException()
    override fun checkUriPermission(uri: android.net.Uri?, readPermission: String?, writePermission: String?, pid: Int, uid: Int, modeFlags: Int) = throw UnsupportedOperationException()
    override fun enforceUriPermission(uri: android.net.Uri?, pid: Int, uid: Int, modeFlags: Int, message: String?) = throw UnsupportedOperationException()
    override fun enforceCallingUriPermission(uri: android.net.Uri?, modeFlags: Int, message: String?) = throw UnsupportedOperationException()
    override fun enforceCallingOrSelfUriPermission(uri: android.net.Uri?, modeFlags: Int, message: String?) = throw UnsupportedOperationException()
    override fun enforceUriPermission(uri: android.net.Uri?, readPermission: String?, writePermission: String?, pid: Int, uid: Int, modeFlags: Int, message: String?) = throw UnsupportedOperationException()
    override fun createPackageContext(packageName: String?, flags: Int) = throw UnsupportedOperationException()
    override fun createContextForSplit(splitName: String?) = throw UnsupportedOperationException()
    override fun createConfigurationContext(overrideConfiguration: android.content.res.Configuration) = throw UnsupportedOperationException()
    override fun createDisplayContext(display: android.view.Display) = throw UnsupportedOperationException()
    override fun createDeviceProtectedStorageContext() = throw UnsupportedOperationException()
    override fun isDeviceProtectedStorage() = throw UnsupportedOperationException()
    // endregion
}

/**
 * In-memory [SharedPreferences] implementation for unit tests.
 *
 * Avoids the need for Android instrumentation or Robolectric.
 */
private class FakeSharedPreferences : SharedPreferences {

    private val store = mutableMapOf<String, Any?>()
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

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.add(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.remove(listener)
    }

    private class FakeEditor(
        private val store: MutableMap<String, Any?>,
        private val listeners: Set<SharedPreferences.OnSharedPreferenceChangeListener>,
        private val prefs: SharedPreferences
    ) : SharedPreferences.Editor {

        private val pending = mutableMapOf<String, Any?>()
        private val removes = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String, value: String?) = apply { pending[key] = value }
        override fun putStringSet(key: String, values: Set<String>?) = apply { pending[key] = values }
        override fun putInt(key: String, value: Int) = apply { pending[key] = value }
        override fun putLong(key: String, value: Long) = apply { pending[key] = value }
        override fun putFloat(key: String, value: Float) = apply { pending[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { pending[key] = value }
        override fun remove(key: String) = apply { removes.add(key) }
        override fun clear() = apply { clearAll = true }

        override fun commit(): Boolean {
            applyChanges()
            return true
        }

        override fun apply() {
            applyChanges()
        }

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
