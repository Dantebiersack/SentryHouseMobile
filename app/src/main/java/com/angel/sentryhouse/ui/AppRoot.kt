package com.angel.sentryhouse.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.angel.sentryhouse.ui.navigation.NavGraph

@Composable
fun AppRoot(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val navController = rememberNavController()
    NavGraph(
        navController = navController,
        isDarkTheme = isDarkTheme,
        onToggleTheme = onToggleTheme
    )
}
