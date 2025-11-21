package com.example.gala.data.database.dao

import androidx.room.*
import com.example.gala.data.database.entities.ActivityLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {

    @Query("SELECT * FROM activity_logs WHERE tripId = :tripId ORDER BY timestamp DESC")
    fun getLogsForTrip(tripId: String): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLog)
}
