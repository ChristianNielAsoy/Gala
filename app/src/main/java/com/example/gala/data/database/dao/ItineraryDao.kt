package com.example.gala.data.database.dao

import androidx.room.*
import com.example.gala.data.database.entities.ItineraryEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface ItineraryDao {

    @Query("SELECT * FROM itinerary_events WHERE tripId = :tripId ORDER BY dayIndex, time")
    fun getItinerary(tripId: String): Flow<List<ItineraryEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: ItineraryEvent)

    @Delete
    suspend fun deleteEvent(event: ItineraryEvent)
}
