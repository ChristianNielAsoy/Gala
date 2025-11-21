package com.example.gala.data.database.dao

import androidx.room.*
import com.example.gala.data.database.entities.ExpenseShare

@Dao
interface ExpenseShareDao {

    @Query("SELECT * FROM expense_shares WHERE expenseId = :expenseId")
    suspend fun getFinalShares(expenseId: String): List<ExpenseShare>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShares(shares: List<ExpenseShare>)

    @Query("DELETE FROM expense_shares WHERE expenseId = :expenseId")
    suspend fun clearShares(expenseId: String)
}
