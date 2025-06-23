package com.angel.sentryhouse.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.angel.sentryhouse.ui.screens.*

@Composable
fun NavGraph(
    navController: NavHostController,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(navController = navController, startDestination = "login", modifier = modifier) {
        composable("login") {
            LoginScreen(navController)
        }
        composable("home") {
            HomeScreen(navController = navController, onToggleTheme = onToggleTheme)
        }

        composable("agua") {
            AguaScreen(navController = navController)
        }
        composable("gas") {
            GasScreen(navController = navController)
        }
    }
}
