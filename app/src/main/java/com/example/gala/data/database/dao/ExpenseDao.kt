package com.example.gala.data.database.dao

import androidx.room.*
import com.example.gala.data.database.entities.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY createdAt DESC")
    fun getExpensesForTrip(tripId: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE expenseId = :expenseId LIMIT 1")
    fun getExpense(expenseId: String): Flow<Expense?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)
}
