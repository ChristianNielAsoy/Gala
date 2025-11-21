package com.example.gala.data.database.dao

import androidx.room.*
import com.example.gala.data.database.entities.SettlementRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SettlementDao {

    @Query("SELECT * FROM settlements WHERE tripId = :tripId")
    fun getSettlementForTrip(tripId: String): Flow<List<SettlementRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<SettlementRecord>)

    @Query("DELETE FROM settlements WHERE tripId = :tripId")
    suspend fun deleteRecordsForTrip(tripId: String)
}
