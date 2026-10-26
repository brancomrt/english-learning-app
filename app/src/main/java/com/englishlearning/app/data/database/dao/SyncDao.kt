package com.englishlearning.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.englishlearning.app.data.database.entity.SyncLogEntity
import com.englishlearning.app.data.database.entity.SyncMetadataEntity
import com.englishlearning.app.data.database.entity.SyncSettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações de sincronização com a nuvem.
 */
@Dao
interface SyncDao {
    
    // ═══════════════════════════════════════════════════════════════════════
    // Sync Metadata
    // ═══════════════════════════════════════════════════════════════════════
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncMetadata(metadata: SyncMetadataEntity): Long
    
    @Update
    suspend fun updateSyncMetadata(metadata: SyncMetadataEntity)
    
    @Query("SELECT * FROM sync_metadata WHERE tableName = :table AND localId = :localId")
    suspend fun getSyncMetadata(table: String, localId: Long): SyncMetadataEntity?
    
    @Query("SELECT * FROM sync_metadata WHERE syncStatus = :status")
    suspend fun getPendingSync(status: String = "PENDING"): List<SyncMetadataEntity>
    
    @Query("SELECT * FROM sync_metadata WHERE syncStatus IN ('PENDING', 'ERROR') AND retryCount < :maxRetries")
    suspend fun getItemsToSync(maxRetries: Int = 3): List<SyncMetadataEntity>
    
    @Query("UPDATE sync_metadata SET syncStatus = :status, lastSyncedAt = :syncedAt, remoteId = :remoteId WHERE id = :id")
    suspend fun markAsSynced(id: Long, status: String = "SYNCED", syncedAt: Long = System.currentTimeMillis(), remoteId: String?)
    
    @Query("UPDATE sync_metadata SET syncStatus = 'ERROR', errorMessage = :error, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun markAsError(id: Long, error: String)
    
    @Query("DELETE FROM sync_metadata WHERE tableName = :table AND localId = :localId")
    suspend fun deleteSyncMetadata(table: String, localId: Long)
    
    @Query("SELECT COUNT(*) FROM sync_metadata WHERE syncStatus = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>
    
    // ═══════════════════════════════════════════════════════════════════════
    // Sync Settings
    // ═══════════════════════════════════════════════════════════════════════
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSyncSettings(settings: SyncSettingsEntity)
    
    @Query("SELECT * FROM sync_settings WHERE userId = :userId")
    suspend fun getSyncSettings(userId: Long): SyncSettingsEntity?
    
    @Query("SELECT * FROM sync_settings WHERE userId = :userId")
    fun getSyncSettingsFlow(userId: Long): Flow<SyncSettingsEntity?>
    
    @Query("UPDATE sync_settings SET isEnabled = :enabled, updatedAt = :timestamp WHERE userId = :userId")
    suspend fun setSyncEnabled(userId: Long, enabled: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE sync_settings SET cloudUserId = :cloudId, provider = :provider, authToken = :token, updatedAt = :timestamp WHERE userId = :userId")
    suspend fun setCloudCredentials(userId: Long, cloudId: String, provider: String, token: String?, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE sync_settings SET lastFullSyncAt = :timestamp WHERE userId = :userId")
    suspend fun updateLastFullSync(userId: Long, timestamp: Long = System.currentTimeMillis())
    
    // ═══════════════════════════════════════════════════════════════════════
    // Sync Log
    // ═══════════════════════════════════════════════════════════════════════
    
    @Insert
    suspend fun insertSyncLog(log: SyncLogEntity): Long
    
    @Query("SELECT * FROM sync_log ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 50): List<SyncLogEntity>
    
    @Query("SELECT * FROM sync_log WHERE status = 'FAILED' ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getFailedLogs(limit: Int = 20): List<SyncLogEntity>
    
    @Query("DELETE FROM sync_log WHERE timestamp < :before")
    suspend fun deleteOldLogs(before: Long)
    
    @Query("SELECT COUNT(*) FROM sync_log WHERE status = 'SUCCESS' AND timestamp > :since")
    suspend fun getSuccessfulSyncCount(since: Long): Int
}
