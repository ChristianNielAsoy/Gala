package com.example.gala.ui.tripdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gala.data.database.entities.Expense
import com.example.gala.data.database.entities.ItineraryEvent
import com.example.gala.data.database.entities.PackingItem
import com.example.gala.data.database.entities.Trip
import com.example.gala.data.database.entities.TripMember
import com.example.gala.data.repository.GalaRepository
import com.example.gala.data.settings.SettingsPreferences
import com.example.gala.data.settings.SettingsRepository
import com.example.gala.ui.settings.SettingsViewModel
import com.example.gala.ui.settings.SettingsViewModelFactory
import com.example.gala.ui.viewmodels.TripDetailsViewModel
import com.example.gala.ui.viewmodels.factories.TripDetailsViewModelFactory
import kotlin.math.abs

@Composable
fun TripDetailsScreen(
    repository: GalaRepository,
    tripId: String,
    onBack: () -> Unit = {},
    onAddExpense: (String, String?) -> Unit = { _, _ -> }
) {
    val vm: TripDetailsViewModel = viewModel(
        factory = TripDetailsViewModelFactory(repository, tripId)
    )
    val context = LocalContext.current.applicationContext
    val settingsVm: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(SettingsRepository(context))
    )

    val trip by vm.trip.collectAsState()
    val members by vm.members.collectAsState()
    val expenses by vm.expenses.collectAsState()
    val settlement by vm.settlement.collectAsState()
    val itinerary by vm.itinerary.collectAsState()
    val packing by vm.packing.collectAsState()
    val settings by settingsVm.preferences.collectAsState()

    TripDetailsUI(
        trip = trip,
        members = members,
        expenses = expenses,
        settlement = settlement,
        itinerary = itinerary,
        packing = packing,
        settings = settings,
        onAddExpense = onAddExpense,
        onBack = onBack,
        onAddPlanEvent = vm::addPlanEvent,
        onDeletePlanEvent = vm::deletePlanEvent,
        onAddPackingItem = vm::addPackingItem,
        onTogglePackingItem = vm::togglePackingItem,
        onDeletePackingItem = vm::deletePackingItem
    )
}

@Composable
private fun TripDetailsUI(
    trip: Trip?,
    members: List<TripMember>,
    expenses: List<Expense>,
    settlement: Map<String, Double>,
    itinerary: List<ItineraryEvent>,
    packing: List<PackingItem>,
    settings: SettingsPreferences,
    onAddExpense: (String, String?) -> Unit,
    onBack: () -> Unit,
    onAddPlanEvent: (
        phase: String,
        dayIndex: Int,
        time: String?,
        title: String,
        notes: String?
    ) -> Unit,
    onDeletePlanEvent: (ItineraryEvent) -> Unit,
    onAddPackingItem: (String, String?) -> Unit,
    onTogglePackingItem: (PackingItem, Boolean) -> Unit,
    onDeletePackingItem: (PackingItem) -> Unit
) {
    if (trip == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currencyLabel = trip.currency.ifBlank { "PHP" }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text(text = "Back")
                }
                Text(
                    text = trip.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { onAddExpense(trip.tripId, null) }) {
                    Text(text = "Add Expense")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TripSummaryCard(trip = trip, expenses = expenses, currencyLabel = currencyLabel)
            }
            item {
                PlanSection(
                    itinerary = itinerary,
                    packing = packing,
                    onAddPlanEvent = onAddPlanEvent,
                    onDeletePlanEvent = onDeletePlanEvent,
                    onAddPackingItem = onAddPackingItem,
                    onTogglePackingItem = onTogglePackingItem,
                    onDeletePackingItem = onDeletePackingItem,
                    onCreateExpense = { event -> onAddExpense(trip.tripId, event.title) }
                )
            }
            item {
                MembersSection(members = members)
            }
            item {
                ExpensesSection(expenses = expenses, currencyLabel = currencyLabel)
            }
            item {
                SettlementSection(settlement = settlement, currencyLabel = currencyLabel)
            }
        }
    }
}

@Composable
private fun TripSummaryCard(trip: Trip, expenses: List<Expense>, currencyLabel: String) {
    val total = expenses.sumOf { it.amount }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = trip.destination?.let { "${trip.title} - $it" } ?: trip.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(text = "${trip.startDate} to ${trip.endDate}")
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Total expenses: $currencyLabel ${"%.2f".format(total)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PlanSection(
    itinerary: List<ItineraryEvent>,
    packing: List<PackingItem>,
    onAddPlanEvent: (
        phase: String,
        dayIndex: Int,
        time: String?,
        title: String,
        notes: String?
    ) -> Unit,
    onDeletePlanEvent: (ItineraryEvent) -> Unit,
    onAddPackingItem: (String, String?) -> Unit,
    onTogglePackingItem: (PackingItem, Boolean) -> Unit,
    onDeletePackingItem: (PackingItem) -> Unit,
    onCreateExpense: (ItineraryEvent) -> Unit
) {
    val phases = listOf(
        "before" to "Before the trip",
        "during" to "During the trip",
        "after" to "After the trip"
    )

    val titleState = rememberSaveable { mutableStateOf("") }
    val notesState = rememberSaveable { mutableStateOf("") }
    val dayIndexState = rememberSaveable { mutableStateOf("0") }
    val timeState = rememberSaveable { mutableStateOf("") }
    val phaseState = rememberSaveable { mutableStateOf("before") }

    val packingName = rememberSaveable { mutableStateOf("") }
    val packingAssignee = rememberSaveable { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Plan & Itinerary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            phases.forEach { (key, label) ->
                val events = itinerary.filter { it.phase == key }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (events.isEmpty()) {
                        Text(
                            text = "No entries yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        events.sortedBy { it.dayIndex }.forEach { event ->
                            PlanEventRow(
                                event = event,
                                onDelete = { onDeletePlanEvent(event) },
                                onCreateExpense = onCreateExpense
                            )
                        }
                    }
                }
            }

            Text(
                text = "Add plan entry",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = titleState.value,
                    onValueChange = { titleState.value = it },
                    label = { Text("Title") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = dayIndexState.value,
                    onValueChange = { dayIndexState.value = it },
                    label = { Text("Day index") },
                    modifier = Modifier.weight(0.6f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = timeState.value,
                    onValueChange = { timeState.value = it },
                    label = { Text("Time (optional)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = phaseState.value,
                    onValueChange = { phaseState.value = it },
                    label = { Text("Phase (before/during/after)") },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = notesState.value,
                onValueChange = { notesState.value = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth()
            )

            TextButton(
                onClick = {
                    if (titleState.value.isBlank()) return@TextButton
                    val dayIndex = dayIndexState.value.toIntOrNull() ?: 0
                    onAddPlanEvent(
                        phaseState.value.ifBlank { "during" },
                        dayIndex,
                        timeState.value.ifBlank { null },
                        titleState.value.trim(),
                        notesState.value.ifBlank { null }
                    )
                    titleState.value = ""
                    notesState.value = ""
                    timeState.value = ""
                    dayIndexState.value = "0"
                    phaseState.value = "before"
                }
            ) {
                Text("Save plan entry")
            }

            HorizontalDivider()

            Text(
                text = "Packing checklist",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            if (packing.isEmpty()) {
                Text(
                    text = "No packing items yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                packing.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = item.isPacked,
                                onCheckedChange = { onTogglePackingItem(item, it) }
                            )
                            Column {
                                Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
                                item.assignedTo?.let {
                                    Text(
                                        text = "Assigned to $it",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        TextButton(onClick = { onDeletePackingItem(item) }) {
                            Text("Remove")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = packingName.value,
                    onValueChange = { packingName.value = it },
                    label = { Text("Item name") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = packingAssignee.value,
                    onValueChange = { packingAssignee.value = it },
                    label = { Text("Assigned to") },
                    modifier = Modifier.weight(1f)
                )
            }

            TextButton(
                onClick = {
                    onAddPackingItem(
                        packingName.value,
                        packingAssignee.value.ifBlank { null }
                    )
                    packingName.value = ""
                    packingAssignee.value = ""
                }
            ) {
                Text("Add packing item")
            }
        }
    }
}

@Composable
private fun PlanEventRow(
    event: ItineraryEvent,
    onDelete: () -> Unit,
    onCreateExpense: (ItineraryEvent) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            event.time?.let {
                Text(
                    text = "Time: $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "Day index: ${event.dayIndex}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            event.notes?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { onCreateExpense(event) }) {
                    Text("Create expense")
                }
                TextButton(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun MembersSection(members: List<TripMember>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Members",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            if (members.isEmpty()) {
                Text(text = "No members added yet.")
            } else {
                members.forEach { member ->
                    Text(
                        text = "${member.memberId} (${member.role ?: "member"})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpensesSection(expenses: List<Expense>, currencyLabel: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Expenses",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            if (expenses.isEmpty()) {
                Text(text = "No expenses recorded.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    expenses.forEach { expense ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = expense.description,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            expense.category?.let { category ->
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Text(
                                text = "$currencyLabel ${"%.2f".format(expense.amount)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Paid by ${expense.paidBy} on ${expense.date}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettlementSection(settlement: Map<String, Double>, currencyLabel: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Settlement",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            if (settlement.isEmpty()) {
                Text(text = "No settlement data yet.")
            } else {
                settlement.entries.forEach { (memberId, balance) ->
                    val status = when {
                        balance > 0 -> "receives"
                        balance < 0 -> "owes"
                        else -> "is settled"
                    }
                    val amount = abs(balance)
                    Text(
                        text = "$memberId $status $currencyLabel ${"%.2f".format(amount)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}