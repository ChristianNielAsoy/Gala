// FIXED: Changed package to match navigation and folder structure
package com.example.gala.ui.expenses

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gala.data.repository.GalaRepository
import com.example.gala.ui.viewmodels.ExpensesViewModel
import com.example.gala.ui.viewmodels.factories.ExpensesViewModelFactory

@Composable
fun ExpenseListScreen(
    repository: GalaRepository,
    tripId: String,
    onOpenExpense: (String) -> Unit
) {
    // Inject ViewModel
    val vm: ExpensesViewModel = viewModel(
        factory = ExpensesViewModelFactory(repository)
    )

    // Correct Compose state collection
    val expenses by vm.expensesForTrip(tripId)
        .collectAsState(initial = emptyList())

    ExpenseListUI(
        expenses = expenses,
        onOpenExpense = onOpenExpense
    )
}