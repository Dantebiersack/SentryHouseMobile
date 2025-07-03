package com.angel.sentryhouse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.angel.sentryhouse.RetrofitClient.getApi
import com.angel.sentryhouse.ui.components.DrawerContent
import com.github.tehras.charts.piechart.PieChart
import com.github.tehras.charts.piechart.PieChartData
import com.github.tehras.charts.piechart.PieChartData.Slice
import com.github.tehras.charts.piechart.animation.simpleChartAnimation
import com.github.tehras.charts.piechart.renderer.SimpleSliceDrawer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, onToggleTheme: () -> Unit) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val aguaPrincipal = 60f
    val aguaFuga = 40f
    var lecturaGas by remember { mutableStateOf(0) }
    val context = LocalContext.current

    // Actualiza lectura del gas cada 5 segundos
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val api = getApi(context)
                val response = api.obtenerGas()
                lecturaGas = response.valor
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(5000)
        }
    }
// Procesamiento de lectura del gas en porcentaje
    val gasLectura = lecturaGas.toFloat()
    val gasFuga = (gasLectura / 1023f) * 100f
    val gasPrincipal = 100f - gasFuga
    // Datos para las gráficas circulares
    val gasData = PieChartData(
        slices = listOf(
            Slice(gasPrincipal, Color(0xFFFF7043)),
            Slice(gasFuga, Color(0xFFFFAB91))
        )
    )
    val aguaData = PieChartData(
        slices = listOf(
            Slice(aguaPrincipal, Color(0xFF42A5F5)),
            Slice(aguaFuga, Color(0xFF90CAF9))
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

                // ---------- Agua ----------
                Text("Consumo de Agua", style = MaterialTheme.typography.titleMedium)
                PieChart(
                    pieChartData = aguaData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    animation = simpleChartAnimation(),
                    sliceDrawer = SimpleSliceDrawer()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Consumo normal: $aguaPrincipal%", color = Color(0xFF42A5F5))
                Text("• Posible fuga: $aguaFuga%", color = Color(0xFF90CAF9))
                if (aguaFuga > 30) {
                    Text(
                        "⚠️ Posible fuga detectada",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                //  Visualización Gas
                Text("Consumo de Gas", style = MaterialTheme.typography.titleMedium)
                PieChart(
                    pieChartData = gasData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    animation = simpleChartAnimation(),
                    sliceDrawer = SimpleSliceDrawer()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Consumo normal: ${"%.2f".format(gasPrincipal)}%", color = Color(0xFFFF7043))
                Text("• Posible fuga: ${"%.2f".format(gasFuga)}%", color = Color(0xFFFFAB91))
                if (gasFuga > 20) {
                    Text(
                        "⚠️ Posible fuga detectada",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
