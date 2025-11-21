package com.example.gala.ui.viewmodels.factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gala.data.repository.GalaRepository
import com.example.gala.ui.viewmodels.TripDetailsViewModel

class TripDetailsViewModelFactory(
    private val repo: GalaRepository,
    private val tripId: String
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TripDetailsViewModel(repo, tripId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
