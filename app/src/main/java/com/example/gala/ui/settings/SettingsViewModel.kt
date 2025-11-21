package com.example.gala.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gala.data.settings.SettingsPreferences
import com.example.gala.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    val preferences: StateFlow<SettingsPreferences> =
        repository.preferencesFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsPreferences()
        )

    fun setExpenseUpdates(enabled: Boolean) {
        viewModelScope.launch { repository.setExpenseUpdates(enabled) }
    }

    fun setTripReminders(enabled: Boolean) {
        viewModelScope.launch { repository.setTripReminders(enabled) }
    }

    fun setSettlementReminders(enabled: Boolean) {
        viewModelScope.launch { repository.setSettlementReminders(enabled) }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { repository.setDarkMode(enabled) }
    }

    fun setDefaultCurrency(currency: String) {
        viewModelScope.launch { repository.setDefaultCurrency(currency) }
    }
}

class SettingsViewModelFactory(
    private val repository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
