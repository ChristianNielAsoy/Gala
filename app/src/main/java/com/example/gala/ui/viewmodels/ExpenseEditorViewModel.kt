package com.example.gala.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gala.data.repository.GalaRepository
import com.example.gala.data.database.entities.Expense
import com.example.gala.data.database.entities.ExpenseItem
import com.example.gala.data.database.entities.ExpenseItemShare
import com.example.gala.data.database.entities.ExpenseShare
import kotlinx.coroutines.launch

/**
 * ViewModel for editing/creating expenses
 * Handles all expense-related operations including items and shares
 */
class ExpenseEditorViewModel(
    private val repo: GalaRepository
) : ViewModel() {

    /**
     * Save the base expense entity
     */
    fun saveBaseExpense(expense: Expense) {
        viewModelScope.launch {
            repo.insertExpense(expense)
        }
    }

    /**
     * Save expense items for an expense
     */
    fun saveItems(items: List<ExpenseItem>) {
        viewModelScope.launch {
            if (items.isNotEmpty()) {
                repo.insertItems(items)
            }
        }
    }

    /**
     * Save per-item shares (who consumed what item)
     */
    fun saveItemShares(shares: List<ExpenseItemShare>) {
        viewModelScope.launch {
            if (shares.isNotEmpty()) {
                repo.insertShares(shares)
            }
        }
    }

    /**
     * Save final expense shares (the calculated amounts per person)
     */
    fun saveFinalShares(shares: List<ExpenseShare>) {
        viewModelScope.launch {
            if (shares.isNotEmpty()) {
                repo.saveFinalShares(shares)
            }
        }
    }

    /**
     * Complete save operation - saves expense with all related data
     * This is the recommended method for saving a complete expense
     */
    fun saveComplete(
        expense: Expense,
        items: List<ExpenseItem> = emptyList(),
        itemShares: List<ExpenseItemShare> = emptyList(),
        finalShares: List<ExpenseShare> = emptyList()
    ) {
        viewModelScope.launch {
            // Convert itemShares list to map grouped by itemId
            val itemSharesMap = itemShares.groupBy { it.itemId }

            repo.saveBaseExpense(
                expense = expense,
                items = items,
                itemShares = itemSharesMap,
                finalShares = finalShares
            )
        }
    }

    /**
     * Delete items for an expense (for updating)
     */
    fun deleteItems(expenseId: String) {
        viewModelScope.launch {
            repo.deleteItems(expenseId)
        }
    }

    /**
     * Delete shares for items (for updating)
     */
    fun deleteShares(itemIds: List<String>) {
        viewModelScope.launch {
            repo.deleteShares(itemIds)
        }
    }

    /**
     * Clear final shares for an expense
     */
    fun clearFinalShares(expenseId: String) {
        viewModelScope.launch {
            repo.clearFinalShares(expenseId)
        }
    }
}