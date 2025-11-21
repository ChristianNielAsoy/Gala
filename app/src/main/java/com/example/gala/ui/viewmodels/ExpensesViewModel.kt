package com.example.gala.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.example.gala.data.repository.GalaRepository
import com.example.gala.data.database.entities.Expense
import kotlinx.coroutines.flow.Flow

class ExpensesViewModel(
    private val repo: GalaRepository
) : ViewModel() {

    fun expensesForTrip(tripId: String): Flow<List<Expense>> {
        return repo.getExpensesForTrip(tripId)
    }
}
