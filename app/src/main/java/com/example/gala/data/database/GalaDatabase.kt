package com.example.gala.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.gala.data.database.dao.*
import com.example.gala.data.database.entities.*

@Database(
    entities = [
        Trip::class,
        TripMember::class,
        Member::class,

        Expense::class,
        ExpenseItem::class,
        ExpenseItemShare::class,
        ExpenseShare::class,

        ActivityLog::class,
        Document::class,
        ItineraryEvent::class,
        PackingItem::class,
        SettlementRecord::class
    ],
    version = 2,
    exportSchema = false
)
abstract class GalaDatabase : RoomDatabase() {

    // TRIP + MEMBERS
    abstract fun tripDao(): TripDao
    abstract fun tripMemberDao(): TripMemberDao
    abstract fun memberDao(): MemberDao

    // EXPENSES
    abstract fun expenseDao(): ExpenseDao
    abstract fun expenseItemDao(): ExpenseItemDao
    abstract fun expenseItemShareDao(): ExpenseItemShareDao
    abstract fun expenseShareDao(): ExpenseShareDao

    // OTHER MODULES
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun documentDao(): DocumentDao
    abstract fun itineraryDao(): ItineraryDao
    abstract fun packingDao(): PackingDao
    abstract fun settlementDao(): SettlementDao
}
