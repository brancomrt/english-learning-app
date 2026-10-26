package com.englishlearning.app.domain.model

/**
 * Modelos de domínio para Sincronização na Nuvem.
 */

/**
 * Configurações de sincronização.
 */
data class SyncSettings(
    val isEnabled: Boolean = false,
    val wifiOnly: Boolean = true,
    val autoSync: Boolean = true,
    val syncIntervalMinutes: Int = 30,
    val cloudUserId: String? = null,
    val provider: CloudProvider? = null,
    val lastFullSyncAt: Long? = null
)

/**
 * Provedor de nuvem suportado.
 */
enum class CloudProvider(val label: String, val emoji: String) {
    FIREBASE("Firebase", "🔥"),
    SUPABASE("Supabase", "⚡"),
    CUSTOM("Servidor Próprio", "🖥️")
}

/**
 * Status de sincronização.
 */
enum class SyncStatus(val label: String, val emoji: String) {
    IDLE("Pronto", "✓"),
    SYNCING("Sincronizando...", "🔄"),
    PENDING("Pendente", "⏳"),
    ERROR("Erro", "❌"),
    OFFLINE("Offline", "📴"),
    CONFLICT("Conflito", "⚠️")
}

/**
 * Resultado de uma operação de sincronização.
 */
data class SyncResult(
    val status: SyncResultStatus,
    val uploadedCount: Int = 0,
    val downloadedCount: Int = 0,
    val conflictCount: Int = 0,
    val errorMessage: String? = null,
    val duration: Long = 0
)

enum class SyncResultStatus {
    SUCCESS,
    PARTIAL,
    FAILED,
    CONFLICT,
    NO_CHANGES
}

/**
 * Metadados de sincronização de um registro.
 */
data class SyncMetadata(
    val tableName: String,
    val localId: Long,
    val remoteId: String? = null,
    val status: SyncItemStatus,
    val localModifiedAt: Long,
    val lastSyncedAt: Long? = null,
    val contentHash: String? = null,
    val errorMessage: String? = null,
    val retryCount: Int = 0
)

enum class SyncItemStatus {
    PENDING,
    SYNCED,
    CONFLICT,
    ERROR
}

/**
 * Conflito de sincronização.
 */
data class SyncConflict(
    val tableName: String,
    val localId: Long,
    val remoteId: String,
    val localData: Map<String, Any>,
    val remoteData: Map<String, Any>,
    val localModifiedAt: Long,
    val remoteModifiedAt: Long
)

/**
 * Resolução de conflito.
 */
enum class ConflictResolution(val label: String) {
    USE_LOCAL("Usar dados locais"),
    USE_REMOTE("Usar dados da nuvem"),
    MERGE("Mesclar"),
    SKIP("Ignorar")
}

/**
 * Log de sincronização.
 */
data class SyncLog(
    val id: Long = 0,
    val operation: SyncOperation,
    val tables: List<String>,
    val recordCount: Int,
    val status: SyncResultStatus,
    val details: String? = null,
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

enum class SyncOperation {
    UPLOAD,
    DOWNLOAD,
    FULL_SYNC,
    CONFLICT_RESOLVE,
    INITIAL_SYNC
}

/**
 * Estado geral de sincronização.
 */
data class SyncState(
    val status: SyncStatus = SyncStatus.IDLE,
    val pendingCount: Int = 0,
    val lastSyncAt: Long? = null,
    val nextSyncAt: Long? = null,
    val currentProgress: Float? = null,
    val currentOperation: String? = null
)

/**
 * Dados exportáveis para backup.
 */
data class BackupData(
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val userId: Long,
    val progress: UserProgressBackup,
    val srsCards: List<SrsCardBackup>,
    val flashcards: List<FlashcardBackup>,
    val settings: SettingsBackup
)

data class UserProgressBackup(
    val streak: Int,
    val totalXp: Int,
    val level: Int,
    val achievements: List<String>,
    val completedLessons: List<Int>
)

data class SrsCardBackup(
    val exerciseId: Int,
    val stability: Double,
    val difficulty: Double,
    val state: String,
    val dueDate: String,
    val reps: Int,
    val lapses: Int
)

data class FlashcardBackup(
    val word: String,
    val stability: Double,
    val difficulty: Double,
    val state: String,
    val dueDate: String
)

data class SettingsBackup(
    val dailyGoal: Int,
    val reminderTime: String?,
    val soundEnabled: Boolean,
    val ttsSpeed: Float
)

/**
 * Informações da conta na nuvem.
 */
data class CloudAccount(
    val provider: CloudProvider,
    val userId: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isVerified: Boolean = false,
    val createdAt: Long,
    val lastLoginAt: Long
)

/**
 * Opções de autenticação.
 */
sealed class AuthOption(val label: String, val emoji: String) {
    data object Google : AuthOption("Google", "🔵")
    data object Apple : AuthOption("Apple", "🍎")
    data object Email : AuthOption("Email", "📧")
    data object Anonymous : AuthOption("Anônimo", "👤")
}
