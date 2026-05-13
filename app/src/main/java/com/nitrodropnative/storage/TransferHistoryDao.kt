package com.nitrodropnative.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferHistoryDao {
    @Query("SELECT * FROM transfer_history ORDER BY timestamp DESC")
    fun observeHistory(): Flow<List<TransferHistoryEntity>>

    @Query("SELECT * FROM transfer_history ORDER BY timestamp DESC LIMIT 1")
    suspend fun lastTransfer(): TransferHistoryEntity?

    @Query("SELECT COUNT(*) FROM transfer_history WHERE direction = 'SEND' AND status = 'Completed'")
    suspend fun totalFilesSent(): Int

    @Query("SELECT MAX(peakSpeed) FROM transfer_history")
    suspend fun fastestSpeed(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TransferHistoryEntity)

    @Query("DELETE FROM transfer_history")
    suspend fun clear()
}
