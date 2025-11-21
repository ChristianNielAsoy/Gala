package com.example.gala.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings"
)

data class SettingsPreferences(
    val expenseUpdates: Boolean = true,
    val tripReminders: Boolean = true,
    val settlementReminders: Boolean = false,
    val darkMode: Boolean = false,
    val defaultCurrency: String = "PHP (₱)"
)

class SettingsRepository(private val context: Context) {

    val preferencesFlow: Flow<SettingsPreferences> = context.settingsDataStore.data.map { prefs ->
        SettingsPreferences(
            expenseUpdates = prefs[Keys.EXPENSE_UPDATES] ?: true,
            tripReminders = prefs[Keys.TRIP_REMINDERS] ?: true,
            settlementReminders = prefs[Keys.SETTLEMENT_REMINDERS] ?: false,
            darkMode = prefs[Keys.DARK_MODE] ?: false,
            defaultCurrency = prefs[Keys.DEFAULT_CURRENCY] ?: "PHP (₱)"
        )
    }

    suspend fun setExpenseUpdates(enabled: Boolean) {
        updatePreference(Keys.EXPENSE_UPDATES, enabled)
    }

    suspend fun setTripReminders(enabled: Boolean) {
        updatePreference(Keys.TRIP_REMINDERS, enabled)
    }

    suspend fun setSettlementReminders(enabled: Boolean) {
        updatePreference(Keys.SETTLEMENT_REMINDERS, enabled)
    }

    suspend fun setDarkMode(enabled: Boolean) {
        updatePreference(Keys.DARK_MODE, enabled)
    }

    suspend fun setDefaultCurrency(currency: String) {
        updatePreference(Keys.DEFAULT_CURRENCY, currency)
    }

    private suspend fun <T> updatePreference(key: Preferences.Key<T>, value: T) {
        context.settingsDataStore.edit { prefs ->
            prefs[key] = value
        }
    }

    private object Keys {
        val EXPENSE_UPDATES = booleanPreferencesKey("expense_updates")
        val TRIP_REMINDERS = booleanPreferencesKey("trip_reminders")
        val SETTLEMENT_REMINDERS = booleanPreferencesKey("settlement_reminders")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
    }
}
