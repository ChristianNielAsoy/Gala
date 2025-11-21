package com.example.gala.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gala.data.database.entities.Expense
import com.example.gala.data.database.entities.ExpenseShare
import com.example.gala.data.database.entities.ItineraryEvent
import com.example.gala.data.database.entities.PackingItem
import com.example.gala.data.database.entities.Trip
import com.example.gala.data.database.entities.TripMember
import com.example.gala.data.repository.GalaRepository
import com.example.gala.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel that exposes reactive trip data plus helpers for expense management.
 */
class TripDetailsViewModel(
    private val repo: GalaRepository,
    private val tripId: String
) : ViewModel() {

    // Trip information
    val trip: StateFlow<Trip?> = repo.getTrip(tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    // Members assigned to this trip
    val members: StateFlow<List<TripMember>> = repo.getMembersForTrip(tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    // Expenses for the trip
    val expenses: StateFlow<List<Expense>> = repo.getExpensesForTrip(tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val itinerary: StateFlow<List<ItineraryEvent>> = repo.getItinerary(tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val packing: StateFlow<List<PackingItem>> = repo.getPackingList(tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    // Derived settlement map keyed by memberId
    val settlement: StateFlow<Map<String, Double>> = expenses
        .mapLatest { calculateSettlement(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())

    private suspend fun calculateSettlement(expenseList: List<Expense>): Map<String, Double> {
        val balances = mutableMapOf<String, Double>()

        expenseList.forEach { expense ->
            val shares = repo.getFinalShares(expense.expenseId)

            balances[expense.paidBy] = (balances[expense.paidBy] ?: 0.0) + expense.amount

            shares.forEach { share ->
                balances[share.memberId] =
                    (balances[share.memberId] ?: 0.0) - share.finalAmount
            }
        }

        return balances
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repo.deleteExpense(expense)
        }
    }

    fun addEqualExpense(
        description: String,
        amount: Double,
        paidBy: String,
        participants: List<String>
    ) {
        viewModelScope.launch {
            val expense = Expense(
                tripId = tripId,
                description = description,
                amount = amount,
                paidBy = paidBy,
                date = currentDate()
            )

            repo.insertExpense(expense)

            val perPerson = amount / participants.size.coerceAtLeast(1)
            val shares = participants.map { memberId ->
                ExpenseShare(
                    expenseId = expense.expenseId,
                    memberId = memberId,
                    finalAmount = perPerson
                )
            }
            repo.saveFinalShares(shares)
        }
    }

    fun addCustomExpense(
        description: String,
        totalAmount: Double,
        paidBy: String,
        shares: Map<String, Double>
    ) {
        viewModelScope.launch {
            val expense = Expense(
                tripId = tripId,
                description = description,
                amount = totalAmount,
                paidBy = paidBy,
                date = currentDate()
            )

            repo.insertExpense(expense)

            val expenseShares = shares.map { (memberId, amount) ->
                ExpenseShare(
                    expenseId = expense.expenseId,
                    memberId = memberId,
                    finalAmount = amount
                )
            }
            repo.saveFinalShares(expenseShares)
        }
    }

    private fun currentDate(): String = DateUtils.currentDateIso()

    fun addPlanEvent(
        phase: String,
        dayIndex: Int,
        time: String?,
        title: String,
        notes: String?
    ) {
        viewModelScope.launch {
            val event = ItineraryEvent(
                tripId = tripId,
                dayIndex = dayIndex,
                time = time,
                title = title,
                notes = notes,
                phase = phase
            )
            repo.insertEvent(event)
        }
    }

    fun deletePlanEvent(event: ItineraryEvent) {
        viewModelScope.launch {
            repo.deleteEvent(event)
        }
    }

    fun addPackingItem(name: String, assignedTo: String?) {
        viewModelScope.launch {
            val item = PackingItem(
                tripId = tripId,
                name = name,
                assignedTo = assignedTo
            )
            repo.insertItem(item)
        }
    }

    fun togglePackingItem(item: PackingItem, isPacked: Boolean) {
        viewModelScope.launch {
            repo.insertItem(item.copy(isPacked = isPacked))
        }
    }

    fun deletePackingItem(item: PackingItem) {
        viewModelScope.launch {
            repo.deleteItem(item)
        }
    }
}
