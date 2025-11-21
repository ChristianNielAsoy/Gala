package com.example.gala.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gala.data.settings.SettingsPreferences
import com.example.gala.data.settings.SettingsRepository

@Composable
fun SettingsScreen() {
    val context = LocalContext.current.applicationContext
    val vm: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(SettingsRepository(context))
    )
    val prefs by vm.preferences.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        ProfileCard()
        NotificationsCard(
            prefs = prefs,
            onExpenseUpdates = vm::setExpenseUpdates,
            onTripReminders = vm::setTripReminders,
            onSettlementReminders = vm::setSettlementReminders
        )
        PreferencesCard(
            prefs = prefs,
            onDarkMode = vm::setDarkMode,
            onCurrencyChanged = vm::setDefaultCurrency
        )
        LegalCard()
    }
}

@Composable
private fun ProfileCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Profile & Team",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Manage your account details and invite your travel buddies.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = { /* TODO hook up profile screen */ }) {
                Text("Edit profile")
            }
            TextButton(onClick = { /* TODO share invite */ }) {
                Text("Invite teammates")
            }
        }
    }
}

@Composable
private fun NotificationsCard(
    prefs: SettingsPreferences,
    onExpenseUpdates: (Boolean) -> Unit,
    onTripReminders: (Boolean) -> Unit,
    onSettlementReminders: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            SettingsToggle(
                label = "Expense updates",
                description = "Get notified when a teammate logs a new expense.",
                checked = prefs.expenseUpdates,
                onCheckedChange = onExpenseUpdates
            )
            SettingsToggle(
                label = "Trip reminders",
                description = "Remind me a day before a trip starts.",
                checked = prefs.tripReminders,
                onCheckedChange = onTripReminders
            )
            SettingsToggle(
                label = "Settlement alerts",
                description = "Alert me when someone owes or settles up.",
                checked = prefs.settlementReminders,
                onCheckedChange = onSettlementReminders
            )
        }
    }
}

@Composable
private fun PreferencesCard(
    prefs: SettingsPreferences,
    onDarkMode: (Boolean) -> Unit,
    onCurrencyChanged: (String) -> Unit
) {
    val currencyMenuExpanded = remember { mutableStateOf(false) }
    val currencies = listOf("PHP (₱)", "USD ($)", "EUR (€)")

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "App Preferences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            SettingsToggle(
                label = "Dark mode",
                description = "Use a dark color scheme across the app.",
                checked = prefs.darkMode,
                onCheckedChange = onDarkMode
            )
            Column {
                Text(
                    text = "Default currency",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = prefs.defaultCurrency,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { currencyMenuExpanded.value = true }) {
                    Text("Change currency")
                }
                DropdownMenu(
                    expanded = currencyMenuExpanded.value,
                    onDismissRequest = { currencyMenuExpanded.value = false }
                ) {
                    currencies.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                currencyMenuExpanded.value = false
                                onCurrencyChanged(option)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegalCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "About Gala",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Version 1.0.0 (prototype)",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(onClick = { /* TODO open terms */ }) {
                    Text("Terms of Service")
                }
                TextButton(onClick = { /* TODO open privacy */ }) {
                    Text("Privacy Policy")
                }
            }
        }
    }
}

@Composable
private fun SettingsToggle(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
