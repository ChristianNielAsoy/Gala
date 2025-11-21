package com.example.gala.data.database.entities

import androidx.room.Entity

@Entity(
    tableName = "expense_shares",
    primaryKeys = ["expenseId", "memberId"]
)
data class ExpenseShare(
    val expenseId: String,
    val memberId: String,
    val finalAmount: Double
)
