package com.example.gala.data.database.dao

import androidx.room.*
import com.example.gala.data.database.entities.ExpenseItem

@Dao
interface ExpenseItemDao {

    @Query("SELECT * FROM expense_items WHERE expenseId = :expenseId")
    suspend fun getItemsForExpense(expenseId: String): List<ExpenseItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ExpenseItem>)

    @Query("DELETE FROM expense_items WHERE expenseId = :expenseId")
    suspend fun deleteItemsForExpense(expenseId: String)
}
