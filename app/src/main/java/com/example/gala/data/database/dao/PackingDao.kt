package com.example.gala.data.database.dao

import androidx.room.*
import com.example.gala.data.database.entities.PackingItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PackingDao {

    @Query("SELECT * FROM packing_items WHERE tripId = :tripId")
    fun getPackingList(tripId: String): Flow<List<PackingItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PackingItem)

    @Delete
    suspend fun deleteItem(item: PackingItem)
}
