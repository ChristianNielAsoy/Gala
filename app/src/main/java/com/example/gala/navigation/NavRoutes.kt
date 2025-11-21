package com.example.gala.navigation

import android.net.Uri

sealed class NavRoute(val route: String) {
    object Dashboard : NavRoute("dashboard")

    object TripDetails : NavRoute("trip/{tripId}") {
        fun create(tripId: String) = "trip/$tripId"
    }

    object ExpenseList : NavRoute("trip/{tripId}/expenses") {
        fun create(tripId: String) = "trip/$tripId/expenses"
    }

    object ExpenseEditor : NavRoute("trip/{tripId}/expense?expenseId={expenseId}&templateTitle={templateTitle}") {
        fun create(tripId: String, expenseId: String? = null, templateTitle: String? = null): String {
            val params = mutableListOf<String>()
            expenseId?.let { params += "expenseId=$it" }
            templateTitle?.let { params += "templateTitle=${Uri.encode(it)}" }
            val query = if (params.isNotEmpty()) params.joinToString("&", prefix = "?") else ""
            return "trip/$tripId/expense$query"
        }
    }

    object Trips : NavRoute("trips")

    object People : NavRoute("people")

    object Settings : NavRoute("settings")
}
