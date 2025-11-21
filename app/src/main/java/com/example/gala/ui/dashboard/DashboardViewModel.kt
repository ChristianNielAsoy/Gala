package com.example.gala.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gala.data.database.entities.Trip
import com.example.gala.data.repository.GalaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(private val repo: GalaRepository) : ViewModel() {

    val trips: StateFlow<List<Trip>> = repo.getAllTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * FIXED: Changed to accept Trip object instead of separate parameters
     * This matches how DashboardScreen calls it
     */
    fun addTrip(trip: Trip) {
        viewModelScope.launch {
            repo.saveTrip(trip)
        }
    }

    /**
     * Delete a trip
     */
    fun deleteTrip(trip: Trip) {
        viewModelScope.launch {
            repo.deleteTrip(trip)
        }
    }
}