package com.example.gala.data.database.entities

import androidx.room.Embedded
import androidx.room.Relation

data class FullExpense(
    @Embedded val expense: Expense,

    @Relation(
        entity = ExpenseItem::class,
        parentColumn = "expenseId",
        entityColumn = "expenseId"
    )
    val items: List<ExpenseItemWithShares>
)
