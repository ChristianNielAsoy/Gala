package com.example.gala.ui.viewmodels.factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gala.data.repository.GalaRepository
import com.example.gala.ui.viewmodels.ExpenseEditorViewModel

class ExpenseEditorViewModelFactory(
    private val repo: GalaRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseEditorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseEditorViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
