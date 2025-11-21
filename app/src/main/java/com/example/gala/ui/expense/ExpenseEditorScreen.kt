package com.example.gala.ui.expenseeditor

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gala.data.repository.GalaRepository
import com.example.gala.ui.viewmodels.ExpenseEditorViewModel
import com.example.gala.ui.viewmodels.factories.ExpenseEditorViewModelFactory

@Suppress("UNUSED_PARAMETER")
@Composable
fun ExpenseEditorScreen(
    repository: GalaRepository,
    tripId: String,
    expenseId: String? = null,
    templateTitle: String? = null,
    onSaved: () -> Unit
) {
    val vm: ExpenseEditorViewModel = viewModel(
        factory = ExpenseEditorViewModelFactory(repository)
    )

    ExpenseEditorUI(
        tripId = tripId,
        templateTitle = templateTitle,
        onSaveExpense = { expense, items, itemShares, finalShares ->
            vm.saveComplete(
                expense = expense,
                items = items,
                itemShares = itemShares,
                finalShares = finalShares
            )
            onSaved()
        }
    )
}
