package com.example.gala.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey val logId: String = UUID.randomUUID().toString(),
    val tripId: String,
    val type: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
