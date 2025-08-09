package com.angel.sentryhouse.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.angel.sentryhouse.RetrofitClient.getApi
import com.angel.sentryhouse.utils.SensorImageStore
import com.angel.sentryhouse.utils.createImageUri
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.angel.sentryhouse.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlin.math.max
import kotlin.math.min
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.angel.sentryhouse.R // Asume que R.drawable.ic_launcher_foreground existe

// Esta función creará y mostrará una notificación
fun showGasLeakNotification(context: Context, sensorName: String, percentage: Float) {
    val channelId = "gas_leak_channel"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // 1. Crear el canal de notificación (solo se necesita una vez)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Alerta de Fuga de Gas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones para alertar sobre fugas de gas."
        }
        notificationManager.createNotificationChannel(channel)
    }

    // 2. Construir la notificación
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_launcher_foreground) // Usa el ícono de tu app
        .setContentTitle("🚨 ¡ALERTA DE FUGA DE GAS! 🚨")
        .setContentText("Detectada una posible fuga del $sensorName. Nivel: ${"%.2f".format(percentage)}%")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true) // La notificación se cierra al hacer clic
        .build()

    // 3. Mostrar la notificación
    val notificationId = sensorName.hashCode() // ID único para cada sensor
    notificationManager.notify(notificationId, notification)
}

data class SensorItem(
    val id: Int,
    val name: String,
    val imageUri: Uri? = null,
    val isClosed: Boolean = false,
    val hasLeak: Boolean = false
)
val VerdeEcologico = Color(0xFF4CAF50)
val VerdeOscuroEcologico = Color(0xFF2E7D32)
val AzulEcologico = Color(0xFF2196F3)
val AmarilloEcologico = Color(0xFFFFC107)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GasScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var lecturaGas by remember { mutableStateOf(0) }
    var lecturaGasCocina by remember { mutableStateOf(0) }
    var ventiladorEncendido by remember { mutableStateOf(false) }

    // Variables de estado para el porcentaje y notificaciones
    var fugaTanquePorcentaje by remember { mutableStateOf(0f) }
    var fugaCocinaPorcentaje by remember { mutableStateOf(0f) }
    var notificacionTanqueEnviada by remember { mutableStateOf(false) }
    var notificacionCocinaEnviada by remember { mutableStateOf(false) }

    // Constantes para el cálculo de porcentaje (ajustar según tus sensores)
    val umbralFuga = 300 // Valor mínimo para considerar fuga
    val valorMaximo = 4095 // Valor máximo de la lectura del sensor
    val umbralNotificacion = 30f // Porcentaje para enviar la notificación

    var sensorTanque by remember { mutableStateOf(SensorItem(1, "Sensor Tanque")) }
    var sensorCocina by remember { mutableStateOf(SensorItem(2, "Sensor Cocina")) }

    var activeSensorId by remember { mutableStateOf(1) }

    // Cargar imagen guardada si existe
    LaunchedEffect(Unit) {
        val uriTanque = SensorImageStore.getImageUri(context, "gas", 1).firstOrNull()
        if (uriTanque != null) sensorTanque = sensorTanque.copy(imageUri = uriTanque)

        val uriCocina = SensorImageStore.getImageUri(context, "gas", 2).firstOrNull()
        if (uriCocina != null) sensorCocina = sensorCocina.copy(imageUri = uriCocina)
    }

    // Lectura periódica de ambos sensores y cálculo de porcentaje
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val api = getApi(context)
                val responseTanque = api.obtenerGas()
                lecturaGas = responseTanque.valor

                val responseCocina = api.obtenerGasCocina()
                lecturaGasCocina = responseCocina.valor

                // Calcula el porcentaje de fuga para el tanque
                val diferenciaTanque = lecturaGas - umbralFuga
                fugaTanquePorcentaje = if (diferenciaTanque > 0) {
                    min(100f, max(0f, (diferenciaTanque.toFloat() / (valorMaximo - umbralFuga)) * 100f))
                } else {
                    0f
                }
                sensorTanque = sensorTanque.copy(hasLeak = fugaTanquePorcentaje > 20)

                // Lógica de notificación para el sensor del tanque
                if (fugaTanquePorcentaje > umbralNotificacion && !notificacionTanqueEnviada) {
                    showGasLeakNotification(context, "Sensor Tanque", fugaTanquePorcentaje)
                    notificacionTanqueEnviada = true
                } else if (fugaTanquePorcentaje <= umbralNotificacion) {
                    notificacionTanqueEnviada = false
                }

                // Calcula el porcentaje de fuga para la cocina
                val diferenciaCocina = lecturaGasCocina - umbralFuga
                fugaCocinaPorcentaje = if (diferenciaCocina > 0) {
                    min(100f, max(0f, (diferenciaCocina.toFloat() / (valorMaximo - umbralFuga)) * 100f))
                } else {
                    0f
                }
                sensorCocina = sensorCocina.copy(hasLeak = fugaCocinaPorcentaje > 20)

                // Lógica de notificación para el sensor de la cocina
                if (fugaCocinaPorcentaje > umbralNotificacion && !notificacionCocinaEnviada) {
                    showGasLeakNotification(context, "Sensor Cocina", fugaCocinaPorcentaje)
                    notificacionCocinaEnviada = true
                } else if (fugaCocinaPorcentaje <= umbralNotificacion) {
                    notificacionCocinaEnviada = false
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(5000)
        }
    }

    var showDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var editingSensor by remember { mutableStateOf<SensorItem?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            editingSensor?.let { sensor ->
                val updated = sensor.copy(imageUri = uri)
                if (sensor.id == 1) sensorTanque = updated else sensorCocina = updated
                scope.launch {
                    SensorImageStore.saveImageUri(context, "gas", sensor.id, uri)
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            editingSensor?.let { sensor ->
                val updated = sensor.copy(imageUri = tempCameraUri)
                if (sensor.id == 1) sensorTanque = updated else sensorCocina = updated
                scope.launch {
                    SensorImageStore.saveImageUri(context, "gas", sensor.id, tempCameraUri!!)
                }
            }
        }
    }
    fun toggleVentilador(api: ApiService, encender: Boolean, scope: CoroutineScope) {
        scope.launch {
            try {
                val response = api.cambiarEstadoVentilador(ApiService.VentiladorRequest(encender))
                if (response.isSuccessful) {
                    ventiladorEncendido = encender
                } else {
                    // Manejo de error (opcional)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detector de Fuga de Gas") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // Primer Card - Sensor Tanque
                Card(
                    modifier = Modifier
                        .width(160.dp)
                        .height(250.dp)
                        .clickable {
                            tempCameraUri = createImageUri(context)
                            editingSensor = sensorTanque
                            showDialog = true
                        },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            sensorTanque.imageUri?.let {
                                Image(
                                    painter = rememberAsyncImagePainter(it),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } ?: Text(sensorTanque.name, color = Color.White)
                        }

                        // Muestra la lectura en porcentaje
                        Text(
                            "Fuga: %.2f%%".format(fugaTanquePorcentaje),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Button(
                            onClick = {
                                val api = getApi(context)
                                toggleVentilador(api, !ventiladorEncendido, scope)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (ventiladorEncendido)
                                ButtonDefaults.buttonColors(containerColor = Color.Red)
                            else
                                ButtonDefaults.buttonColors(containerColor = VerdeEcologico)
                        ) {
                            Text(if (ventiladorEncendido) "Apagar Ventilador" else "Encender Ventilador", style = MaterialTheme.typography.labelLarge)
                        }


                        if (sensorTanque.hasLeak) {
                            Text(
                                "⚠️ Posible fuga",
                                color = Color.Red,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Segundo Card - Sensor Cocina
                Card(
                    modifier = Modifier
                        .width(160.dp)
                        .height(250.dp)
                        .clickable {
                            tempCameraUri = createImageUri(context)
                            editingSensor = sensorCocina
                            showDialog = true
                        },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            sensorCocina.imageUri?.let {
                                Image(
                                    painter = rememberAsyncImagePainter(it),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } ?: Text(sensorCocina.name, color = Color.White)
                        }

                        // Muestra la lectura en porcentaje
                        Text(
                            "Fuga: %.2f%%".format(fugaCocinaPorcentaje),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Button(
                            onClick = {
                                val api = getApi(context)
                                toggleVentilador(api, !ventiladorEncendido, scope)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (ventiladorEncendido)
                                ButtonDefaults.buttonColors(containerColor = Color.Red)
                            else
                                ButtonDefaults.buttonColors(containerColor = VerdeEcologico)
                        ) {
                            Text(if (ventiladorEncendido) "Apagar Ventilador" else "Encender Ventilador", style = MaterialTheme.typography.labelLarge)
                        }


                        if (sensorCocina.hasLeak) {
                            Text(
                                "⚠️ Posible fuga",
                                color = Color.Red,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }

    if (showDialog && editingSensor != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Seleccionar imagen") },
            text = { Text("¿Deseas tomar una foto o elegir una de la galería?") },
            confirmButton = {
                Column {
                    TextButton(onClick = {
                        showDialog = false
                        tempCameraUri?.let { cameraLauncher.launch(it) }
                    }) {
                        Text("Tomar foto")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = {
                        showDialog = false
                        galleryLauncher.launch("image/*")
                    }) {
                        Text("Galería")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (editingSensor!!.imageUri != null) {
                        TextButton(onClick = {
                            showDialog = false
                            val clearedSensor = editingSensor!!.copy(imageUri = null)
                            if (editingSensor!!.id == 1) sensorTanque = clearedSensor else sensorCocina = clearedSensor
                            scope.launch {
                                SensorImageStore.saveImageUri(context, "gas", editingSensor!!.id, null)
                            }
                        }) {
                            Text("Quitar foto")
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}