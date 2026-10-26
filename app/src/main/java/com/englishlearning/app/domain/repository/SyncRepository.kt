package com.englishlearning.app.domain.repository

import com.englishlearning.app.domain.model.BackupData
import com.englishlearning.app.domain.model.CloudAccount
import com.englishlearning.app.domain.model.SyncConflict
import com.englishlearning.app.domain.model.SyncResult
import com.englishlearning.app.domain.model.SyncSettings
import com.englishlearning.app.domain.model.SyncState
import kotlinx.coroutines.flow.Flow

/**
 * Repository para sincronização com a nuvem.
 */
interface SyncRepository {
    
    /**
     * Obtém configurações de sincronização.
     */
    suspend fun getSettings(userId: Long): SyncSettings
    
    /**
     * Salva configurações de sincronização.
     */
    suspend fun saveSettings(userId: Long, settings: SyncSettings)
    
    /**
     * Habilita ou desabilita sincronização.
     */
    suspend fun setEnabled(userId: Long, enabled: Boolean)
    
    /**
     * Autentica com provedor de nuvem.
     */
    suspend fun authenticate(provider: com.englishlearning.app.domain.model.CloudProvider, credential: String): CloudAccount
    
    /**
     * Desconecta da nuvem.
     */
    suspend fun signOut(userId: Long)
    
    /**
     * Obtém conta conectada.
     */
    suspend fun getAccount(userId: Long): CloudAccount?
    
    /**
     * Executa sincronização completa.
     */
    suspend fun syncAll(userId: Long): SyncResult
    
    /**
     * Sincroniza apenas alterações pendentes.
     */
    suspend fun syncPending(userId: Long): SyncResult
    
    /**
     * Faz download de dados da nuvem.
     */
    suspend fun downloadFromCloud(userId: Long): SyncResult
    
    /**
     * Faz upload de dados para a nuvem.
     */
    suspend fun uploadToCloud(userId: Long): SyncResult
    
    /**
     * Obtém conflitos pendentes.
     */
    suspend fun getConflicts(userId: Long): List<SyncConflict>
    
    /**
     * Resolve conflito.
     */
    suspend fun resolveConflict(
        userId: Long, 
        conflict: SyncConflict, 
        resolution: com.englishlearning.app.domain.model.ConflictResolution
    )
    
    /**
     * Exporta dados para backup local.
     */
    suspend fun exportBackup(userId: Long): BackupData
    
    /**
     * Importa dados de backup local.
     */
    suspend fun importBackup(userId: Long, backup: BackupData)
    
    /**
     * Flow de estado de sincronização.
     */
    fun getSyncStateFlow(userId: Long): Flow<SyncState>
    
    /**
     * Flow de contagem de pendências.
     */
    fun getPendingCountFlow(): Flow<Int>
}
