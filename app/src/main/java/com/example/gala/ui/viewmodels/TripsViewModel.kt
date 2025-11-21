package com.example.gala.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gala.data.database.entities.Trip
import com.example.gala.data.repository.GalaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TripsViewModel(private val repo: GalaRepository) : ViewModel() {

    val trips: StateFlow<List<Trip>> =
        repo.getAllTrips().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun addTrip(trip: Trip) {
        viewModelScope.launch {
            repo.saveTrip(trip)
        }
    }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch {
            repo.deleteTrip(trip)
        }
    }
}
