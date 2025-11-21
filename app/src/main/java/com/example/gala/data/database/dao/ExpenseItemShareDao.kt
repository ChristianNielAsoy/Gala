package com.example.gala.data.database.dao

import androidx.room.*
import com.example.gala.data.database.entities.ExpenseItemShare

@Dao
interface ExpenseItemShareDao {

    @Query("SELECT * FROM expense_item_shares WHERE itemId = :itemId")
    suspend fun getSharesForItem(itemId: String): List<ExpenseItemShare>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShares(shares: List<ExpenseItemShare>)

    @Query("DELETE FROM expense_item_shares WHERE itemId IN (:itemIds)")
    suspend fun deleteSharesForItems(itemIds: List<String>)
}
