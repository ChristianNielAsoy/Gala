package com.example.gala.data.database.entities

import androidx.room.Embedded
import androidx.room.Relation

data class ExpenseWithItems(
    @Embedded val expense: Expense,

    @Relation(
        parentColumn = "expenseId",
        entityColumn = "expenseId"
    )
    val items: List<ExpenseItem>
)
