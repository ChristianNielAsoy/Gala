package com.example.gala.data.database.entities

import androidx.room.Entity

@Entity(
    tableName = "trip_members",
    primaryKeys = ["tripId", "memberId"]
)
data class TripMember(
    val tripId: String,
    val memberId: String,
    val role: String? = "member"
)
