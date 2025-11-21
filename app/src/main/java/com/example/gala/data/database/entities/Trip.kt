package com.example.gala.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey val tripId: String = UUID.randomUUID().toString(),
    val title: String,
    val destination: String? = null,
    val startDate: String,
    val endDate: String,
    val currency: String = "PHP",
    val createdAt: Long = System.currentTimeMillis()
)
