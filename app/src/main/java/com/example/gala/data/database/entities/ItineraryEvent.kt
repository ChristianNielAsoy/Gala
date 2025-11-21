package com.example.gala.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "itinerary_events")
data class ItineraryEvent(
    @PrimaryKey val eventId: String = UUID.randomUUID().toString(),
    val tripId: String,
    val dayIndex: Int,
    val time: String? = null,
    val title: String,
    val notes: String? = null,
    val phase: String = "during",
    val linkedExpenseId: String? = null
)
