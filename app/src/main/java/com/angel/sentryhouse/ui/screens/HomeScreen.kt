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
import com.angel.sentryhouse.RetrofitClient
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
    val context = LocalContext.current

    // Variables que almacenan las lecturas actuales de los sensores
    var lecturaGas by remember { mutableStateOf(0) }
    var lecturaGasCocina by remember { mutableStateOf(0) }

    // Control para cambiar entre sensor de "Tanque" o "Cocina"
    var sensorActual by remember { mutableStateOf("Tanque") }

    // Historial de valores de gas y fugas para graficar
    val historialGasTanque = remember { mutableStateListOf<Float>() }
    val historialFugaTanque = remember { mutableStateListOf<Float>() }
    val historialGasCocina = remember { mutableStateListOf<Float>() }
    val historialFugaCocina = remember { mutableStateListOf<Float>() }

    // Para controlar si ya se envió una notificación
    var alertaEnviada by remember { mutableStateOf(false) }

    // Valores por defecto para la gráfica
    val valoresPorDefectoTanque = listOf(70f, 68f, 65f, 63f, 60f)
    val fugasPorDefectoTanque = listOf(0f, 2f, 5f, 7f, 10f)
    val valoresPorDefectoCocina = listOf(55f, 52f, 50f, 48f, 45f)
    val fugasPorDefectoCocina = listOf(3f, 4f, 6f, 8f, 10f)

    // Cálculo de porcentajes actualizados
    val (gasPrincipalActual, gasFugaActual) = remember(sensorActual, lecturaGas, lecturaGasCocina) {
        val lecturaActual = if (sensorActual == "Tanque") lecturaGas.toFloat() else lecturaGasCocina.toFloat()
        val (baseSinGas, maxValor) = if (sensorActual == "Tanque") 2500f to 4000f else 3200f to 4500f

        val diferencia = lecturaActual - baseSinGas
        val fuga = if (diferencia > 0) (diferencia / (maxValor - baseSinGas)) * 100f else 0f
        val principal = (100f - fuga).coerceAtLeast(0f)

        principal to fuga
    }

    // ⏱️ Lógica para obtener lecturas del servidor cada 5 segundos
    LaunchedEffect(Unit) {
        val api = RetrofitClient.getApi(context)
        while (true) {
            try {
                // Se hacen llamadas a la API para ambos sensores
                val gasResponse = api.obtenerGas()
                val gasCocinaResponse = api.obtenerGasCocina()

                // Actualizamos variables con la lectura más reciente
                lecturaGas = gasResponse.valor
                lecturaGasCocina = gasCocinaResponse.valor

                // Actualización de históricos
                if (sensorActual == "Tanque") {
                    if (historialGasTanque.size >= 10) historialGasTanque.removeAt(0)
                    historialGasTanque.add(gasPrincipalActual)

                    if (historialFugaTanque.size >= 10) historialFugaTanque.removeAt(0)
                    historialFugaTanque.add(gasFugaActual)
                } else {
                    if (historialGasCocina.size >= 10) historialGasCocina.removeAt(0)
                    historialGasCocina.add(gasPrincipalActual)

                    if (historialFugaCocina.size >= 10) historialFugaCocina.removeAt(0)
                    historialFugaCocina.add(gasFugaActual)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                println("❌ Error al obtener datos: ${e.message}")
            }

            delay(5000) // Espera 5 segundos antes de la siguiente lectura
        }
    }

    // 🔔 Enviar notificación si la fuga supera cierto umbral
    LaunchedEffect(gasFugaActual) {
        if (gasFugaActual > 20 && !alertaEnviada) {
            context.enviarNotificacionAlerta("⚠️ Fuga en ${sensorActual.uppercase()}: ${"%.1f".format(gasFugaActual)}%")
            alertaEnviada = true
        } else if (gasFugaActual <= 20 && alertaEnviada) {
            alertaEnviada = false
        }
    }

    // 📈 Datos simulados para la gráfica de agua
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

    // 📉 Construcción del modelo de gráfica de gas dinámico
    val gasEntries by remember(sensorActual) {
        derivedStateOf {
            val (principalEntries, fugaEntries) = if (sensorActual == "Tanque") {
                valoresPorDefectoTanque.mapIndexed { index, valor -> FloatEntry(index.toFloat(), valor) } to
                        fugasPorDefectoTanque.mapIndexed { index, valor -> FloatEntry(index.toFloat(), valor) }
            } else {
                valoresPorDefectoCocina.mapIndexed { index, valor -> FloatEntry(index.toFloat(), valor) } to
                        fugasPorDefectoCocina.mapIndexed { index, valor -> FloatEntry(index.toFloat(), valor) }
            }

            entryModelOf(principalEntries, fugaEntries)
        }
    }

    // 🧭 Contenedor con menú lateral
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
                // 👋 Título
                Text("Bienvenido", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(24.dp))

                // 💧 Sección de Agua
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

                // 🔥 Sección de Gas
                Text("Consumo de Gas", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Botón para seleccionar sensor "Tanque"
                    Button(
                        onClick = { sensorActual = "Tanque" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sensorActual == "Tanque") Color(0xFFFF7043) else MaterialTheme.colorScheme.primary
                        )
                    ) { Text("Ver Tanque") }

                    // Botón para seleccionar sensor "Cocina"
                    Button(
                        onClick = { sensorActual = "Cocina" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sensorActual == "Cocina") Color(0xFFFF7043) else MaterialTheme.colorScheme.primary
                        )
                    ) { Text("Ver Cocina") }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 📊 Gráfica del consumo de gas
                ProvideChartStyle(m3ChartStyle()) {
                    Chart(
                        chart = lineChart(
                            lines = listOf(
                                lineSpec(
                                    lineColor = Color(0xFFFF7043), // Consumo normal
                                    pointConnector = DefaultPointConnector(cubicStrength = 0.5f)
                                ),
                                lineSpec(
                                    lineColor = Color(0xFFFFAB91), // Fuga
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