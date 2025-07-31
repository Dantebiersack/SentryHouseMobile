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
    val historialGasTanque = remember { mutableStateListOf<Float>() }
    val historialFugaTanque = remember { mutableStateListOf<Float>() }
    val historialGasCocina = remember { mutableStateListOf<Float>() }
    val historialFugaCocina = remember { mutableStateListOf<Float>() }
    val context = LocalContext.current

    // Valor base calibrado (ajusta según tus pruebas reales)
    val valorBaseSinGas = 250f

    // Obtener lecturas periódicamente
    LaunchedEffect(Unit) {
        val api = RetrofitClient.getApi(context)

        while (true) {
            try {
                val gasResponse = api.obtenerGas()
                val gasCocinaResponse = api.obtenerGasCocina()

                lecturaGas = gasResponse.valor
                lecturaGasCocina = gasCocinaResponse.valor

                // Calcular para TANQUE
                val baseSinGasTanque = 2500f
                val maxValorTanque = 4000f
                val diferenciaTanque = lecturaGas.toFloat() - baseSinGasTanque
                val gasFugaTanque = if (diferenciaTanque > 0) (diferenciaTanque / (maxValorTanque - baseSinGasTanque)) * 100f else 0f
                val gasPrincipalTanque = (100f - gasFugaTanque).coerceAtLeast(0f)

                // Actualizar histórico TANQUE
                if (historialGasTanque.size >= 10) historialGasTanque.removeAt(0)
                historialGasTanque.add(gasPrincipalTanque)
                if (historialFugaTanque.size >= 10) historialFugaTanque.removeAt(0)
                historialFugaTanque.add(gasFugaTanque)

                // Calcular para COCINA
                val baseSinGasCocina = 3200f
                val maxValorCocina = 4500f
                val diferenciaCocina = lecturaGasCocina.toFloat() - baseSinGasCocina
                val gasFugaCocina = if (diferenciaCocina > 0) (diferenciaCocina / (maxValorCocina - baseSinGasCocina)) * 100f else 0f
                val gasPrincipalCocina = (100f - gasFugaCocina).coerceAtLeast(0f)

                // Actualizar histórico COCINA
                if (historialGasCocina.size >= 10) historialGasCocina.removeAt(0)
                historialGasCocina.add(gasPrincipalCocina)
                if (historialFugaCocina.size >= 10) historialFugaCocina.removeAt(0)
                historialFugaCocina.add(gasFugaCocina)

            } catch (e: Exception) {
                e.printStackTrace()
                println("❌ Error al obtener datos: ${e.message}")
            }

            delay(5000)
        }
    }

    // Calcular valores actuales para mostrar
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

    val gasEntries by remember(sensorActual, historialGasTanque, historialFugaTanque, historialGasCocina, historialFugaCocina, lecturaActual) {
        derivedStateOf {
            val historialPrincipal: List<Float>
            val historialFuga: List<Float>
            val precargadoPrincipal: List<Float>
            val precargadoFuga: List<Float>

            if (sensorActual == "Tanque") {
                historialPrincipal = historialGasTanque
                historialFuga = historialFugaTanque
                precargadoPrincipal = listOf(70f, 68f, 65f)
                precargadoFuga = listOf(0f, 2f, 5f)
            } else {
                historialPrincipal = historialGasCocina
                historialFuga = historialFugaCocina
                precargadoPrincipal = listOf(55f, 52f, 50f)
                precargadoFuga = listOf(3f, 4f, 6f)
            }

            val diferencia = lecturaActual - baseSinGas
            val gasFugaActual = if (diferencia > 0) (diferencia / (maxValor - baseSinGas)) * 100f else 0f
            val gasPrincipalActual = (100f - gasFugaActual).coerceAtLeast(0f)

            val principalEntries = precargadoPrincipal.mapIndexed { index, valor ->
                FloatEntry(index.toFloat(), valor)
            }.toMutableList().apply {
                add(FloatEntry(size.toFloat(), gasPrincipalActual))
            }

            val fugaEntries = precargadoFuga.mapIndexed { index, valor ->
                FloatEntry(index.toFloat(), valor)
            }.toMutableList().apply {
                add(FloatEntry(size.toFloat(), gasFugaActual))
            }

            entryModelOf(principalEntries, fugaEntries)
        }
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
