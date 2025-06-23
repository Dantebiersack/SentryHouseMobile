package com.angel.sentryhouse.ui.components


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun DrawerContent(navController: NavController, drawerState: DrawerState, scope: CoroutineScope) {
    Column (modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)
        .padding(16.dp)) {
        Text("Menú", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        NavigationDrawerItem(
            label = { Text("Home") },
            selected = false,
            onClick = {
                navController.navigate("home")
                scope.launch { drawerState.close() }
            }
        )

        NavigationDrawerItem(
            label = { Text("Ver Agua") },
            selected = false,
            onClick = {
                navController.navigate("agua")
                scope.launch { drawerState.close() }
            }
        )

        NavigationDrawerItem(
            label = { Text("Ver Gas") },
            selected = false,
            onClick = {
                navController.navigate("gas")
                scope.launch { drawerState.close() }
            }
        )
    }
}
