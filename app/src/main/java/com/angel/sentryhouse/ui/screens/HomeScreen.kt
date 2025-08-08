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
import com.angel.sentryhouse.RetrofitClientAgua
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

    // ================== GAS ==================
    var lecturaGas by remember { mutableStateOf(0) }
    var lecturaGasCocina by remember { mutableStateOf(0) }
    var sensorActual by remember { mutableStateOf("Tanque") }

    val historialGasTanque = remember { mutableStateListOf<Float>() }
    val historialFugaTanque = remember { mutableStateListOf<Float>() }
    val historialGasCocina = remember { mutableStateListOf<Float>() }
    val historialFugaCocina = remember { mutableStateListOf<Float>() }

    var alertaEnviada by remember { mutableStateOf(false) }

    val valoresPorDefectoTanque = listOf(70f, 68f, 65f, 63f, 60f)
    val fugasPorDefectoTanque = listOf(0f, 2f, 5f, 7f, 10f)
    val valoresPorDefectoCocina = listOf(55f, 52f, 50f, 48f, 45f)
    val fugasPorDefectoCocina = listOf(3f, 4f, 6f, 8f, 10f)

    val (gasPrincipalActual, gasFugaActual) = remember(sensorActual, lecturaGas, lecturaGasCocina) {
        val lecturaActual = if (sensorActual == "Tanque") lecturaGas.toFloat() else lecturaGasCocina.toFloat()
        val (baseSinGas, maxValor) = if (sensorActual == "Tanque") 2500f to 4000f else 3200f to 4500f
        val diferencia = lecturaActual - baseSinGas
        val fuga = if (diferencia > 0) (diferencia / (maxValor - baseSinGas)) * 100f else 0f
        val principal = (100f - fuga).coerceAtLeast(0f)
        principal to fuga
    }

    // ================== AGUA ==================
    val historialAgua1 = remember { mutableStateListOf<Float>() }
    val historialAgua2 = remember { mutableStateListOf<Float>() }
    val historialAgua3 = remember { mutableStateListOf<Float>() }

    // =============== LECTURAS DE GAS Y AGUA =================
    LaunchedEffect(Unit) {
        val apiGas = RetrofitClient.getApi(context)
        val apiAgua = RetrofitClientAgua.getApi()

        while (true) {
            try {
                // GAS
                val gasResponse = apiGas.obtenerGas()
                val gasCocinaResponse = apiGas.obtenerGasCocina()
                lecturaGas = gasResponse.valor
                lecturaGasCocina = gasCocinaResponse.valor

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

                // AGUA
                val datosAgua = apiAgua.obtenerDatosAgua()
                if (datosAgua.isNotEmpty()) {
                    // ✅ Modificación: Recorre cada elemento de la lista y lo añade al historial
                    datosAgua.forEach { dato ->
                        if (historialAgua1.size >= 10) historialAgua1.removeAt(0)
                        historialAgua1.add(dato.sensor1)

                        if (historialAgua2.size >= 10) historialAgua2.removeAt(0)
                        historialAgua2.add(dato.sensor2)

                        if (historialAgua3.size >= 10) historialAgua3.removeAt(0)
                        historialAgua3.add(dato.sensor3)
                    }
                }


            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(5000)
        }
    }

    // Notificación de fuga de gas
    LaunchedEffect(gasFugaActual) {
        if (gasFugaActual > 20 && !alertaEnviada) {
            context.enviarNotificacionAlerta("⚠️ Fuga en ${sensorActual.uppercase()}: ${"%.1f".format(gasFugaActual)}%")
            alertaEnviada = true
        } else if (gasFugaActual <= 20 && alertaEnviada) {
            alertaEnviada = false
        }
    }

    // Función para generar modelo VICO
    fun generarModelo(datos: List<Float>) = entryModelOf(
        datos.mapIndexed { index, valor -> FloatEntry(index.toFloat(), valor) }
    )

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

    // ================== UI ==================
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

                // ======= Sección Agua =======
                Text("Consumo de Agua - Sensor 1", style = MaterialTheme.typography.titleMedium)
                ProvideChartStyle(m3ChartStyle()) {
                    Chart(
                        chart = lineChart(
                            lines = listOf(lineSpec(
                                lineColor = Color(0xFF42A5F5),
                                pointConnector = DefaultPointConnector(cubicStrength = 0.5f)
                            ))
                        ),
                        model = generarModelo(historialAgua1),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text("Consumo de Agua - Sensor 2", style = MaterialTheme.typography.titleMedium)
                ProvideChartStyle(m3ChartStyle()) {
                    Chart(
                        chart = lineChart(
                            lines = listOf(lineSpec(
                                lineColor = Color(0xFF66BB6A),
                                pointConnector = DefaultPointConnector(cubicStrength = 0.5f)
                            ))
                        ),
                        model = generarModelo(historialAgua2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text("Consumo de Agua - Sensor 3", style = MaterialTheme.typography.titleMedium)
                ProvideChartStyle(m3ChartStyle()) {
                    Chart(
                        chart = lineChart(
                            lines = listOf(lineSpec(
                                lineColor = Color(0xFFFFA726),
                                pointConnector = DefaultPointConnector(cubicStrength = 0.5f)
                            ))
                        ),
                        model = generarModelo(historialAgua3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ======= Sección Gas =======
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
