package com.example.gala.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.gala.data.repository.GalaRepository
import com.example.gala.ui.dashboard.DashboardScreen
import com.example.gala.ui.expenseeditor.ExpenseEditorScreen
import com.example.gala.ui.expenses.ExpenseListScreen
import com.example.gala.ui.settings.SettingsScreen
import com.example.gala.ui.tripdetails.TripDetailsScreen
import com.example.gala.ui.trips.TripsScreen

@Composable
fun GalaNavHost(
    navController: NavHostController,
    repository: GalaRepository,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoute.Dashboard.route,
        modifier = modifier
    ) {
        // DASHBOARD
        composable(NavRoute.Dashboard.route) {
            DashboardScreen(
                repository = repository,
                onOpenTrip = { tripId ->
                    navController.navigate(NavRoute.TripDetails.create(tripId))
                }
            )
        }

        composable(NavRoute.Trips.route) {
            TripsScreen(
                repository = repository,
                onOpenTrip = { tripId ->
                    navController.navigate(NavRoute.TripDetails.create(tripId))
                }
            )
        }

        composable(NavRoute.Settings.route) {
            SettingsScreen()
        }

        // TRIP DETAILS
        composable(
            route = NavRoute.TripDetails.route,
            arguments = listOf(
                navArgument("tripId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId")!!

            TripDetailsScreen(
                repository = repository,
                tripId = tripId,
                onBack = { navController.popBackStack() },
                onAddExpense = { id, templateTitle ->
                    navController.navigate(
                        NavRoute.ExpenseEditor.create(
                            tripId = id,
                            templateTitle = templateTitle
                        )
                    )
                }
            )
        }

        // EXPENSE LIST
        composable(
            route = NavRoute.ExpenseList.route,
            arguments = listOf(
                navArgument("tripId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId")!!

            ExpenseListScreen(
                repository = repository,
                tripId = tripId,
                onOpenExpense = { expenseId ->
                    navController.navigate(
                        NavRoute.ExpenseEditor.create(tripId, expenseId)
                    )
                }
            )
        }

        // EXPENSE EDITOR
        composable(
            route = NavRoute.ExpenseEditor.route,
            arguments = listOf(
                navArgument("tripId") { type = NavType.StringType },
                navArgument("expenseId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("templateTitle") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId")!!
            val expenseId = backStackEntry.arguments?.getString("expenseId")
            val templateTitle = backStackEntry.arguments?.getString("templateTitle")

            ExpenseEditorScreen(
                repository = repository,
                tripId = tripId,
                expenseId = expenseId,
                templateTitle = templateTitle,
                onSaved = {
                    navController.popBackStack() // go back after save
                }
            )
        }
    }
}
