package com.example.gala.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "expense_items")
data class ExpenseItem(
    @PrimaryKey val itemId: String = UUID.randomUUID().toString(),
    val expenseId: String,
    val name: String,
    val amount: Double,
    val isLibre: Boolean = false,
    val paidBy: String? = null
)
