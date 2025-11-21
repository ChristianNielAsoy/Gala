package com.example.gala.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gala.data.database.entities.Trip
import com.example.gala.data.repository.GalaRepository
import com.example.gala.ui.viewmodels.DashboardViewModel
import com.example.gala.ui.viewmodels.factories.DashboardViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DashboardScreen(
    repository: GalaRepository,
    onOpenTrip: (String) -> Unit
) {
    val vm: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(repository)
    )

    val trips by vm.trips.collectAsState()

    DashboardContent(
        trips = trips,
        onOpenTrip = onOpenTrip
    )
}

@Composable
private fun DashboardContent(
    trips: List<Trip>,
    onOpenTrip: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Gala Dashboard",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Track shared adventures and keep spending balanced.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            TripOverviewCard(trips = trips)

            Spacer(Modifier.height(32.dp))

            TripCalendar(trips = trips, onOpenTrip = onOpenTrip)

            Spacer(Modifier.height(32.dp))

            if (trips.isNotEmpty()) {
                Text(
                    text = "Quick access",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(8.dp))

                trips.take(3).forEach { trip ->
                    QuickTripRow(trip = trip, onOpenTrip = onOpenTrip)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun TripOverviewCard(trips: List<Trip>) {
    val totalTrips = trips.size
    val nextTrip = trips.minByOrNull { it.startDate }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Trips created: $totalTrips",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(4.dp))
            if (nextTrip != null) {
                Text(
                    text = "Next trip: ${nextTrip.title} (${nextTrip.startDate})",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "No trips yet. Create one from the Trips tab.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun TripCalendar(
    trips: List<Trip>,
    onOpenTrip: (String) -> Unit
) {
    val month = rememberMonth(trips)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = month.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                month.weekdayLabels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            for (row in 0 until 6) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (col in 0 until 7) {
                        val cell = month.cells[row * 7 + col]
                        CalendarCell(cell = cell, onOpenTrip = onOpenTrip)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.CalendarCell(
    cell: CalendarDay,
    onOpenTrip: (String) -> Unit
) {
    val textColor = if (cell.inCurrentMonth) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(vertical = 4.dp)
            .clickable(enabled = cell.tripIds.isNotEmpty()) {
                // If multiple trips overlap the date, open the first for now.
                cell.tripIds.firstOrNull()?.let(onOpenTrip)
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = cell.dayLabel,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(2.dp))
        if (cell.tripIds.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .fillMaxWidth(0.4f)
                    .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
            )
        } else {
            Spacer(Modifier.height(6.dp))
        }
    }
}

private data class CalendarDay(
    val dayLabel: String,
    val inCurrentMonth: Boolean,
    val tripIds: List<String>
)

private data class CalendarMonth(
    val title: String,
    val weekdayLabels: List<String>,
    val cells: List<CalendarDay>
)

@Composable
private fun rememberMonth(trips: List<Trip>): CalendarMonth {
    return androidx.compose.runtime.remember(trips) {
        buildCalendarMonth(trips)
    }
}

private fun buildCalendarMonth(trips: List<Trip>): CalendarMonth {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val monthTitle = monthFormat.format(calendar.time)
    val weekdayLabels = (0 until 7).map { index ->
        val temp = calendar.clone() as Calendar
        val firstDay = temp.firstDayOfWeek
        val targetDay = (firstDay + index - 1) % 7 + 1
        temp.set(Calendar.DAY_OF_WEEK, targetDay)
        weekdayFormat.format(temp.time)
    }

    val currentMonth = calendar.get(Calendar.MONTH)
    val startOffset = ((calendar.get(Calendar.DAY_OF_WEEK) - calendar.firstDayOfWeek) + 7) % 7
    val tempCal = calendar.clone() as Calendar
    tempCal.add(Calendar.DAY_OF_MONTH, -startOffset)

    val cells = mutableListOf<CalendarDay>()
    repeat(42) {
        val inMonth = tempCal.get(Calendar.MONTH) == currentMonth
        val dateKey = dayFormat.format(tempCal.time)
        val tripsForDay = trips.filter { it.includesDate(dateKey) }.map { it.tripId }
        cells += CalendarDay(
            dayLabel = tempCal.get(Calendar.DAY_OF_MONTH).toString(),
            inCurrentMonth = inMonth,
            tripIds = tripsForDay
        )
        tempCal.add(Calendar.DAY_OF_MONTH, 1)
    }

    return CalendarMonth(
        title = monthTitle,
        weekdayLabels = weekdayLabels,
        cells = cells
    )
}

private fun Trip.includesDate(date: String): Boolean {
    if (startDate.isBlank() || endDate.isBlank()) return false
    return date >= startDate && date <= endDate
}

private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
private val weekdayFormat = SimpleDateFormat("EE", Locale.getDefault())
@Composable
private fun QuickTripRow(
    trip: Trip,
    onOpenTrip: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenTrip(trip.tripId) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = trip.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            trip.destination?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${trip.startDate} to ${trip.endDate}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
