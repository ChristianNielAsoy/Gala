package com.example.gala.data.database.entities

import androidx.room.Embedded
import androidx.room.Relation

data class ExpenseItemWithShares(
    @Embedded val item: ExpenseItem,

    @Relation(
        parentColumn = "itemId",
        entityColumn = "itemId"
    )
    val shares: List<ExpenseItemShare>
)
