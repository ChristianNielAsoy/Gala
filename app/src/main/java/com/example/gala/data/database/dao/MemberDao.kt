package com.example.gala.data.database.dao

import androidx.room.*
import com.example.gala.data.database.entities.Member
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {

    @Query("SELECT * FROM members ORDER BY name")
    fun getAllMembers(): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE memberId = :id LIMIT 1")
    fun getMember(id: String): Flow<Member?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member)
}
