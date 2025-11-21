package com.example.gala.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "settlements")
data class SettlementRecord(
    @PrimaryKey val settlementId: String = UUID.randomUUID().toString(),
    val tripId: String,
    val fromMemberId: String,
    val toMemberId: String,
    val amount: Double
)
