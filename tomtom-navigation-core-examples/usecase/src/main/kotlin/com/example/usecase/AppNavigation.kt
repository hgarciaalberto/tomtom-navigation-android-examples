package com.example.usecase

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.usecase.ui.MainScreen
import com.example.usecase.ui.SecondScreen

sealed interface Destinations {
    data class MainScreen(val root: String = "main") : Destinations
    data class SecondScreen(val root: String = "second") : Destinations

}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destinations.MainScreen().root,
    ) {

        composable(route = Destinations.MainScreen().root) {
            MainScreen(
                navController = navController,
            )
        }

        composable(route = Destinations.SecondScreen().root) {
            SecondScreen(
//                navController = navController,
            )
        }
    }
}
