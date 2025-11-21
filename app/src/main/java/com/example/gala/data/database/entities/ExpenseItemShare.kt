package com.example.gala.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "expense_item_shares")
data class ExpenseItemShare(
    @PrimaryKey val shareId: String = UUID.randomUUID().toString(),

    val itemId: String,      // FK → expense_items.itemId
    val memberId: String,    // FK → members.memberId
    val shareAmount: Double  // How much this person pays for this item
)
