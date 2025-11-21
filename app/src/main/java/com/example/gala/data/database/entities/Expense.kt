package com.example.gala.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val expenseId: String = UUID.randomUUID().toString(),
    val tripId: String,
    val description: String,
    val category: String? = null,
    val amount: Double,
    val paidBy: String,
    val date: String,
    val createdAt: Long = System.currentTimeMillis()
)
