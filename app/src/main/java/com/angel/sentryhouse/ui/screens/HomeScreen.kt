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
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.compose.style.ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.core.chart.DefaultPointConnector


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

    // Datos para las gráficas
    val aguaEntries = remember {
        entryModelOf(
            listOf(
                FloatEntry(0f, 60f),
                FloatEntry(1f, 62f),
                FloatEntry(2f, 58f),
                FloatEntry(3f, 61f),
                FloatEntry(4f, 59f)
            )
        )
    }

    val gasEntries = remember(lecturaGas) {
        entryModelOf(
            listOf(
                FloatEntry(0f, 10f),
                FloatEntry(1f, 12f),
                FloatEntry(2f, 15f),
                FloatEntry(3f, gasPrincipal.coerceAtLeast(0f))
            ),
            listOf(
                FloatEntry(0f, 0f),
                FloatEntry(1f, 0f),
                FloatEntry(2f, 0f),
                FloatEntry(3f, 0f),
                FloatEntry(4f, gasFuga.coerceAtLeast(0f))
            )
        )
    }

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
                ProvideChartStyle(chartStyle = m3ChartStyle()) {
                    Chart(
                        chart = lineChart(
                            lines = listOf(
                                lineSpec(
                                    lineColor = Color(0xFF42A5F5),
                                    pointConnector = DefaultPointConnector(cubicStrength = 0.5f)
                                )
                            )
                        ),
                        model = aguaEntries,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
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

                // ---------- Gas ----------
                Text("Consumo de Gas", style = MaterialTheme.typography.titleMedium)
                ProvideChartStyle(chartStyle = m3ChartStyle()) {
                    Chart(
                        chart = lineChart(
                            lines = listOf(
                                lineSpec(
                                    lineColor = Color(0xFFFF7043),
                                    pointConnector = DefaultPointConnector(cubicStrength = 0.5f)
                                ),
                                lineSpec(
                                    lineColor = Color(0xFFFFAB91),
                                    pointConnector = DefaultPointConnector(cubicStrength = 0.5f)
                                )
                            )
                        ),
                        model = gasEntries,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }

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