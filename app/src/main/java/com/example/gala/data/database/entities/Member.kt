package com.example.gala.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "members")
data class Member(
    @PrimaryKey val memberId: String = UUID.randomUUID().toString(),
    val name: String,
    val avatarColor: String? = null,
    val photoPath: String? = null
)
