package com.example.gala

import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gala.data.repository.GalaRepository
import com.example.gala.navigation.GalaNavHost
import com.example.gala.navigation.NavRoute

class MainActivity : ComponentActivity() {

    private lateinit var container: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize dependency container
        container = AppContainer(this)
        val repo = container.repository
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                GalaApp(
                    navController = navController,
                    repository = repo
                )
            }
        }
    }
}

@Composable
private fun GalaApp(
    navController: NavHostController,
    repository: GalaRepository
) {
    val bottomItems = listOf(
        BottomNavItem(NavRoute.Dashboard, Icons.Filled.Home, "Dashboard"),
        BottomNavItem(NavRoute.Trips, Icons.Filled.List, "Trips"),
        BottomNavItem(NavRoute.Settings, Icons.Filled.Settings, "Settings")
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = bottomItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        val selected =
                            currentDestination?.hierarchy?.any { it.route == item.route.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        GalaNavHost(
            navController = navController,
            repository = repository,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

private data class BottomNavItem(
    val route: NavRoute,
    val icon: ImageVector,
    val label: String
)