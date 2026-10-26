package com.englishlearning.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade para controle de sincronização com a nuvem.
 * Rastreia quais dados foram sincronizados e quando.
 */
@Entity(
    tableName = "sync_metadata",
    indices = [
        Index(value = ["tableName"]),
        Index(value = ["syncStatus"]),
        Index(value = ["lastSyncedAt"])
    ]
)
data class SyncMetadataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Nome da tabela sincronizada */
    @ColumnInfo(name = "tableName")
    val tableName: String,
    
    /** ID do registro local */
    @ColumnInfo(name = "localId")
    val localId: Long,
    
    /** ID do registro remoto (null se não sincronizado) */
    @ColumnInfo(name = "remoteId")
    val remoteId: String? = null,
    
    /** Status: PENDING, SYNCED, CONFLICT, ERROR */
    @ColumnInfo(name = "syncStatus")
    val syncStatus: String = "PENDING",
    
    /** Timestamp da última modificação local */
    @ColumnInfo(name = "localModifiedAt")
    val localModifiedAt: Long = System.currentTimeMillis(),
    
    /** Timestamp da última sincronização bem-sucedida */
    @ColumnInfo(name = "lastSyncedAt")
    val lastSyncedAt: Long? = null,
    
    /** Hash do conteúdo para detecção de conflitos */
    @ColumnInfo(name = "contentHash")
    val contentHash: String? = null,
    
    /** Mensagem de erro (se houver) */
    @ColumnInfo(name = "errorMessage")
    val errorMessage: String? = null,
    
    /** Número de tentativas de sync */
    @ColumnInfo(name = "retryCount")
    val retryCount: Int = 0
)

/**
 * Configurações de sincronização do usuário.
 */
@Entity(
    tableName = "sync_settings",
    primaryKeys = ["userId"]
)
data class SyncSettingsEntity(
    @ColumnInfo(name = "userId")
    val userId: Long,
    
    /** Sync habilitado */
    @ColumnInfo(name = "isEnabled")
    val isEnabled: Boolean = false,
    
    /** Sync apenas em WiFi */
    @ColumnInfo(name = "wifiOnly")
    val wifiOnly: Boolean = true,
    
    /** Sync automático */
    @ColumnInfo(name = "autoSync")
    val autoSync: Boolean = true,
    
    /** Intervalo de sync em minutos */
    @ColumnInfo(name = "syncIntervalMinutes")
    val syncIntervalMinutes: Int = 30,
    
    /** ID do usuário no backend (Firebase UID ou similar) */
    @ColumnInfo(name = "cloudUserId")
    val cloudUserId: String? = null,
    
    /** Provider: FIREBASE, SUPABASE, CUSTOM */
    @ColumnInfo(name = "provider")
    val provider: String? = null,
    
    /** Token de autenticação (criptografado) */
    @ColumnInfo(name = "authToken")
    val authToken: String? = null,
    
    /** Timestamp do último sync completo */
    @ColumnInfo(name = "lastFullSyncAt")
    val lastFullSyncAt: Long? = null,
    
    /** Timestamp da última modificação das configurações */
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Log de operações de sync para debugging.
 */
@Entity(
    tableName = "sync_log",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["operation"]),
        Index(value = ["status"])
    ]
)
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Tipo de operação: UPLOAD, DOWNLOAD, FULL_SYNC, CONFLICT_RESOLVE */
    @ColumnInfo(name = "operation")
    val operation: String,
    
    /** Tabelas afetadas (JSON array) */
    @ColumnInfo(name = "tables")
    val tables: String,
    
    /** Número de registros afetados */
    @ColumnInfo(name = "recordCount")
    val recordCount: Int,
    
    /** Status: SUCCESS, PARTIAL, FAILED */
    @ColumnInfo(name = "status")
    val status: String,
    
    /** Detalhes/erro */
    @ColumnInfo(name = "details")
    val details: String? = null,
    
    /** Duração em milissegundos */
    @ColumnInfo(name = "durationMs")
    val durationMs: Long,
    
    /** Timestamp */
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
