package com.example.gala.data.database.dao

import androidx.room.*
import com.example.gala.data.database.entities.TripMember
import kotlinx.coroutines.flow.Flow

@Dao
interface TripMemberDao {

    @Query("SELECT * FROM trip_members WHERE tripId = :tripId")
    fun getMembersForTrip(tripId: String): Flow<List<TripMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTripMember(member: TripMember)

    @Delete
    suspend fun deleteTripMember(member: TripMember)
}
