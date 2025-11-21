package com.example.gala.data.database.dao

import androidx.room.*
import com.example.gala.data.database.entities.Trip
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Query("SELECT * FROM trips ORDER BY startDate")
    fun getAllTrips(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE tripId = :tripId LIMIT 1")
    fun getTrip(tripId: String): Flow<Trip?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip)

    @Delete
    suspend fun deleteTrip(trip: Trip)
}
