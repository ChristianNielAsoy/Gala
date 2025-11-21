package com.example.gala.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey val documentId: String = UUID.randomUUID().toString(),
    val tripId: String,
    val expenseId: String? = null,
    val filePath: String,
    val type: String
)
