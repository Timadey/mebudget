package com.mebudget.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface SyncMetadataDao {

    @Query("SELECT * FROM sync_metadata WHERE entityType = :entityType AND localId = :localId LIMIT 1")
    suspend fun getByLocalId(entityType: String, localId: Long): SyncMetadataEntity?

    @Query("SELECT * FROM sync_metadata WHERE entityType = :entityType AND remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(entityType: String, remoteId: String): SyncMetadataEntity?

    @Query("SELECT * FROM sync_metadata WHERE entityType = :entityType")
    suspend fun getByEntityType(entityType: String): List<SyncMetadataEntity>

    @Query("SELECT * FROM sync_metadata")
    suspend fun getAll(): List<SyncMetadataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: SyncMetadataEntity): Long

    @Update
    suspend fun update(metadata: SyncMetadataEntity)

    @Delete
    suspend fun delete(metadata: SyncMetadataEntity)

    @Query("DELETE FROM sync_metadata")
    suspend fun clearAll()
}
