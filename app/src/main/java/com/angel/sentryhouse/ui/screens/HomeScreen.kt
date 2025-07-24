package com.angel.sentryhouse.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.angel.sentryhouse.enviarNotificacionAlerta
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.chart.DefaultPointConnector
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, onToggleTheme: () -> Unit) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
// 👇 Agrega estas nuevas variables arriba, con el resto de tus estados:
    var prevLecturaGas by remember { mutableStateOf(0) }
    var prevLecturaGasCocina by remember { mutableStateOf(0) }

    var lecturaGas by remember { mutableStateOf(0) }
    var lecturaGasCocina by remember { mutableStateOf(0) }
    var sensorActual by remember { mutableStateOf("Tanque") }
    var alertaEnviada by remember(sensorActual) { mutableStateOf(false) }

    val context = LocalContext.current

    // Valor base calibrado (ajusta según tus pruebas reales)
    val valorBaseSinGas = 250f

    // Obtener lecturas periódicamente
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
        val urlCocina = prefs.getString("url_cocina", "").orEmpty()

        while (true) {
            try {
                val api = getApi(context)
                val nuevoGas = api.obtenerGas().valor
                val nuevoGasCocina = if (urlCocina.isNotBlank()) api.obtenerGasCocina().valor else 0

                // --- Notificación por cruce de umbral en CUARTO (tanque)
                if (prevLecturaGas < 2800 && nuevoGas >= 2800) {
                    context.enviarNotificacionAlerta("⚠️ Alerta: Gas en el CUARTO ha subido a ${nuevoGas} ppm")
                }

                // --- Notificación por cruce de umbral en COCINA
                if (prevLecturaGasCocina < 3600 && nuevoGasCocina >= 3600) {
                    context.enviarNotificacionAlerta("⚠️ Alerta: Gas en la COCINA ha subido a ${nuevoGasCocina} ppm")
                }

                // Actualiza las variables para la siguiente comparación
                prevLecturaGas = nuevoGas
                prevLecturaGasCocina = nuevoGasCocina
                lecturaGas = nuevoGas
                lecturaGasCocina = nuevoGasCocina

            } catch (e: Exception) {
                e.printStackTrace()
            }

            delay(5000)
        }
    }


    // Lectura seleccionada según el sensor activo
    val (baseSinGas, maxValor) = if (sensorActual == "Tanque") {
        2500f to 4000f
    } else {
        3200f to 4500f
    }

    val lecturaActual = if (sensorActual == "Tanque") lecturaGas.toFloat() else lecturaGasCocina.toFloat()
    val diferencia = lecturaActual - baseSinGas
    val gasFugaActual = if (diferencia > 0) (diferencia / (maxValor - baseSinGas)) * 100f else 0f
    val gasPrincipalActual = (100f - gasFugaActual).coerceAtLeast(0f)


    // Notificación
    LaunchedEffect(gasFugaActual) {
        if (gasFugaActual > 20 && !alertaEnviada) {
            context.enviarNotificacionAlerta("⚠️ Fuga en ${sensorActual.uppercase()}: ${"%.1f".format(gasFugaActual)}%")
            alertaEnviada = true
        }
        if (gasFugaActual <= 20 && alertaEnviada) {
            alertaEnviada = false
        }
    }

    // Gráficas de ejemplo
    val aguaPrincipal = 60f
    val aguaFuga = 40f
    val aguaEntries = entryModelOf(
        listOf(
            FloatEntry(0f, 60f),
            FloatEntry(1f, 62f),
            FloatEntry(2f, 58f),
            FloatEntry(3f, 61f),
            FloatEntry(4f, 59f)
        )
    )

    val gasEntries = entryModelOf(
        listOf(
            FloatEntry(0f, 10f),
            FloatEntry(1f, 12f),
            FloatEntry(2f, 15f),
            FloatEntry(3f, gasPrincipalActual.coerceAtLeast(0f))
        ),
        listOf(
            FloatEntry(0f, 0f),
            FloatEntry(1f, 0f),
            FloatEntry(2f, 0f),
            FloatEntry(3f, gasFugaActual.coerceAtLeast(0f))
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
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Bienvenido", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(24.dp))

                // Agua
                Text("Consumo de Agua", style = MaterialTheme.typography.titleMedium)
                ProvideChartStyle(m3ChartStyle()) {
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
                    Text("⚠️ Posible fuga detectada", color = Color.Red)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Gas
                Text("Consumo de Gas", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { sensorActual = "Tanque" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sensorActual == "Tanque") Color(0xFFFF7043) else MaterialTheme.colorScheme.primary
                        )
                    ) { Text("Ver Tanque") }

                    Button(
                        onClick = { sensorActual = "Cocina" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sensorActual == "Cocina") Color(0xFFFF7043) else MaterialTheme.colorScheme.primary
                        )
                    ) { Text("Ver Cocina") }
                }

                Spacer(modifier = Modifier.height(16.dp))
                ProvideChartStyle(m3ChartStyle()) {
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
                Text("• Consumo normal: ${"%.2f".format(gasPrincipalActual)}%", color = Color(0xFFFF7043))
                Text("• Posible fuga: ${"%.2f".format(gasFugaActual)}%", color = Color(0xFFFFAB91))
                if (gasFugaActual > 20) {
                    Text("⚠️ Posible fuga detectada", color = Color.Red)
                }
            }
        }
    }
}
