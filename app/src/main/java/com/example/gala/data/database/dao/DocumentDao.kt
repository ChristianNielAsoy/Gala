package com.example.gala.data.database.dao

import androidx.room.*
import com.example.gala.data.database.entities.Document
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents WHERE tripId = :tripId")
    fun getDocumentsForTrip(tripId: String): Flow<List<Document>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: Document)

    @Delete
    suspend fun deleteDocument(document: Document)
}
