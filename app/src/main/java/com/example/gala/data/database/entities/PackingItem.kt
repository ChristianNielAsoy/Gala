package com.example.gala.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "packing_items")
data class PackingItem(
    @PrimaryKey val itemId: String = UUID.randomUUID().toString(),
    val tripId: String,
    val name: String,
    val assignedTo: String? = null,
    val isPacked: Boolean = false
)
