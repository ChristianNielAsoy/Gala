package com.example.gala.data.repository

import androidx.room.withTransaction
import com.example.gala.data.database.GalaDatabase
import com.example.gala.data.database.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class GalaRepository(private val db: GalaDatabase) {

    // ==================== TRIP OPERATIONS ====================

    fun getAllTrips(): Flow<List<Trip>> = db.tripDao().getAllTrips()

    fun getTrip(tripId: String): Flow<Trip?> = db.tripDao().getTrip(tripId)

    suspend fun saveTrip(trip: Trip) {
        db.tripDao().insertTrip(trip)
    }

    suspend fun deleteTrip(trip: Trip) {
        db.tripDao().deleteTrip(trip)
    }

    // ==================== MEMBER OPERATIONS ====================

    fun getAllMembers(): Flow<List<Member>> = db.memberDao().getAllMembers()

    fun getMember(memberId: String): Flow<Member?> = db.memberDao().getMember(memberId)

    suspend fun insertMember(member: Member) {
        db.memberDao().insertMember(member)
    }

    // ==================== TRIP MEMBER OPERATIONS ====================

    fun getMembersForTrip(tripId: String): Flow<List<TripMember>> =
        db.tripMemberDao().getMembersForTrip(tripId)

    suspend fun addMemberToTrip(tripMember: TripMember) {
        db.tripMemberDao().insertTripMember(tripMember)
    }

    suspend fun removeMemberFromTrip(tripMember: TripMember) {
        db.tripMemberDao().deleteTripMember(tripMember)
    }

    // ==================== EXPENSE OPERATIONS ====================

    fun getExpensesForTrip(tripId: String): Flow<List<Expense>> =
        db.expenseDao().getExpensesForTrip(tripId)

    fun getExpense(expenseId: String): Flow<Expense?> =
        db.expenseDao().getExpense(expenseId)

    suspend fun insertExpense(expense: Expense) {
        db.expenseDao().insertExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense) {
        // Delete related data first
        val items = db.expenseItemDao().getItemsForExpense(expense.expenseId)
        val itemIds = items.map { it.itemId }

        if (itemIds.isNotEmpty()) {
            db.expenseItemShareDao().deleteSharesForItems(itemIds)
        }

        db.expenseItemDao().deleteItemsForExpense(expense.expenseId)
        db.expenseShareDao().clearShares(expense.expenseId)
        db.expenseDao().deleteExpense(expense)
    }

    // ==================== EXPENSE ITEM OPERATIONS ====================

    suspend fun getItemsForExpense(expenseId: String): List<ExpenseItem> =
        db.expenseItemDao().getItemsForExpense(expenseId)

    suspend fun insertItems(items: List<ExpenseItem>) {
        db.expenseItemDao().insertItems(items)
    }

    suspend fun deleteItems(expenseId: String) {
        db.expenseItemDao().deleteItemsForExpense(expenseId)
    }

    // ==================== EXPENSE ITEM SHARE OPERATIONS ====================

    suspend fun getSharesForItem(itemId: String): List<ExpenseItemShare> =
        db.expenseItemShareDao().getSharesForItem(itemId)

    suspend fun insertShares(shares: List<ExpenseItemShare>) {
        db.expenseItemShareDao().insertShares(shares)
    }

    suspend fun deleteShares(itemIds: List<String>) {
        db.expenseItemShareDao().deleteSharesForItems(itemIds)
    }

    // ==================== EXPENSE SHARE OPERATIONS ====================

    suspend fun getFinalShares(expenseId: String): List<ExpenseShare> =
        db.expenseShareDao().getFinalShares(expenseId)

    suspend fun saveFinalShares(shares: List<ExpenseShare>) {
        db.expenseShareDao().insertShares(shares)
    }

    suspend fun clearFinalShares(expenseId: String) {
        db.expenseShareDao().clearShares(expenseId)
    }

    suspend fun replaceShares(expenseId: String, newShares: List<ExpenseShare>) {
        db.expenseShareDao().clearShares(expenseId)
        db.expenseShareDao().insertShares(newShares)
    }

    // ==================== COMPLETE EXPENSE SAVE ====================

    suspend fun saveBaseExpense(
        expense: Expense,
        items: List<ExpenseItem> = emptyList(),
        itemShares: Map<String, List<ExpenseItemShare>> = emptyMap(),
        finalShares: List<ExpenseShare> = emptyList()
    ) {
        db.withTransaction {
            db.expenseDao().insertExpense(expense)

            if (items.isNotEmpty()) {
                db.expenseItemDao().insertItems(items)
            }

            if (itemShares.isNotEmpty()) {
                itemShares.values
                    .filter { it.isNotEmpty() }
                    .forEach { db.expenseItemShareDao().insertShares(it) }
            }

            if (finalShares.isNotEmpty()) {
                db.expenseShareDao().insertShares(finalShares)
            }
        }
    }

    // ==================== ACTIVITY LOGS ====================

    fun getLogsForTrip(tripId: String): Flow<List<ActivityLog>> =
        db.activityLogDao().getLogsForTrip(tripId)

    suspend fun saveLog(log: ActivityLog) {
        db.activityLogDao().insertLog(log)
    }

    // ==================== DOCUMENTS ====================

    fun getDocumentsForTrip(tripId: String): Flow<List<Document>> =
        db.documentDao().getDocumentsForTrip(tripId)

    suspend fun insertDocument(document: Document) {
        db.documentDao().insertDocument(document)
    }

    suspend fun deleteDocument(document: Document) {
        db.documentDao().deleteDocument(document)
    }

    // ==================== ITINERARY ====================

    fun getItinerary(tripId: String): Flow<List<ItineraryEvent>> =
        db.itineraryDao().getItinerary(tripId)

    suspend fun insertEvent(event: ItineraryEvent) {
        db.itineraryDao().insertEvent(event)
    }

    suspend fun deleteEvent(event: ItineraryEvent) {
        db.itineraryDao().deleteEvent(event)
    }

    // ==================== PACKING LIST ====================

    fun getPackingList(tripId: String): Flow<List<PackingItem>> =
        db.packingDao().getPackingList(tripId)

    suspend fun insertItem(item: PackingItem) {
        db.packingDao().insertItem(item)
    }

    suspend fun deleteItem(item: PackingItem) {
        db.packingDao().deleteItem(item)
    }

    // ==================== SETTLEMENT OPERATIONS ====================

    fun getSettlementForTrip(tripId: String): Flow<List<SettlementRecord>> =
        db.settlementDao().getSettlementForTrip(tripId)

    suspend fun saveSettlementRecords(records: List<SettlementRecord>) {
        db.settlementDao().insertRecords(records)
    }

    suspend fun clearSettlement(tripId: String) {
        db.settlementDao().deleteRecordsForTrip(tripId)
    }

    // ==================== BUSINESS LOGIC HELPERS ====================

    /**
     * Calculate settlement for a trip based on all expenses
     * Returns simplified list of who owes whom
     */
    suspend fun calculateTripSettlement(tripId: String): List<SettlementRecord> {
        val balances = mutableMapOf<String, Double>()

        val expenses = db.expenseDao().getExpensesForTrip(tripId).first()
        expenses.forEach { expense ->
            val shares = db.expenseShareDao().getFinalShares(expense.expenseId)

            balances[expense.paidBy] =
                (balances[expense.paidBy] ?: 0.0) + expense.amount

            shares.forEach { share ->
                balances[share.memberId] =
                    (balances[share.memberId] ?: 0.0) - share.finalAmount
            }
        }

        return simplifySettlements(balances, tripId)
    }

    private fun simplifySettlements(
        balances: Map<String, Double>,
        tripId: String
    ): List<SettlementRecord> {
        val settlements = mutableListOf<SettlementRecord>()

        val creditors = balances.filter { it.value > 0.01 }.toMutableMap()
        val debtors = balances.filter { it.value < -0.01 }.toMutableMap()

        while (creditors.isNotEmpty() && debtors.isNotEmpty()) {
            val creditor = creditors.entries.first()
            val debtor = debtors.entries.first()

            val amount = minOf(creditor.value, -debtor.value)

            settlements.add(
                SettlementRecord(
                    tripId = tripId,
                    fromMemberId = debtor.key,
                    toMemberId = creditor.key,
                    amount = amount
                )
            )

            val newCreditorBalance = creditor.value - amount
            val newDebtorBalance = debtor.value + amount

            if (newCreditorBalance < 0.01) {
                creditors.remove(creditor.key)
            } else {
                creditors[creditor.key] = newCreditorBalance
            }

            if (newDebtorBalance > -0.01) {
                debtors.remove(debtor.key)
            } else {
                debtors[debtor.key] = newDebtorBalance
            }
        }

        return settlements
    }

    /**
     * Get trip with calculated total expenses
     */
    fun getTripWithTotals(tripId: String): Flow<Pair<Trip?, Double>> {
        return combine(
            getTrip(tripId),
            db.expenseDao().getExpensesForTrip(tripId)
        ) { trip, expenses ->
            trip to expenses.sumOf { it.amount }
        }
    }

    /**
     * Get member balance for a specific trip
     */
    suspend fun getMemberBalance(tripId: String, memberId: String): Double {
        val expenses = db.expenseDao().getExpensesForTrip(tripId).first()
        var balance = 0.0

        expenses.forEach { expense ->
            if (expense.paidBy == memberId) {
                balance += expense.amount
            }

            val shares = db.expenseShareDao().getFinalShares(expense.expenseId)
            val memberShare = shares.find { it.memberId == memberId }
            if (memberShare != null) {
                balance -= memberShare.finalAmount
            }
        }

        return balance
    }
}
