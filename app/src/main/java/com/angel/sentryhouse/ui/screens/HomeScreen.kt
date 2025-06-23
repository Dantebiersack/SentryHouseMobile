package com.angel.sentryhouse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.angel.sentryhouse.ui.components.DrawerContent
import com.github.tehras.charts.piechart.PieChart
import com.github.tehras.charts.piechart.PieChartData
import com.github.tehras.charts.piechart.PieChartData.Slice
import com.github.tehras.charts.piechart.animation.simpleChartAnimation
import com.github.tehras.charts.piechart.renderer.SimpleSliceDrawer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, onToggleTheme: () -> Unit) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Datos simulados
    val aguaData = PieChartData(
        slices = listOf(
            Slice(60f, Color(0xFF42A5F5)),
            Slice(40f, Color(0xFF90CAF9))
        )
    )

    val gasData = PieChartData(
        slices = listOf(
            Slice(75f, Color(0xFFFF7043)),
            Slice(25f, Color(0xFFFFAB91))
        )
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                DrawerContent(navController, drawerState, scope)
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("SentryHouse") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú")
                        }
                    },
                    actions = {
                        IconButton(onClick = onToggleTheme) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Cambiar tema")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text("Bienvenido", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(24.dp))

                Text("Consumo de Agua", style = MaterialTheme.typography.titleMedium)
                PieChart(
                    pieChartData = aguaData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    animation = simpleChartAnimation(),
                    sliceDrawer = SimpleSliceDrawer()
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text("Consumo de Gas", style = MaterialTheme.typography.titleMedium)
                PieChart(
                    pieChartData = gasData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    animation = simpleChartAnimation(),
                    sliceDrawer = SimpleSliceDrawer()
                )
            }
        }
    }
}
