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
import retrofit2.Response

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
    var isLoading by remember { mutableStateOf(false) }

    var sensorTanque by remember { mutableStateOf(SensorItem(1, "Sensor Tanque")) }
    var sensorCocina by remember { mutableStateOf(SensorItem(2, "Sensor Cocina")) }

    // Cargar imagen guardada si existe


    // Función para cargar el estado inicial del ventilador
    fun loadInitialVentiladorState() {
        scope.launch {
            try {
                val api = getApi(context)
                val response = api.obtenerEstadoVentilador()
                ventiladorEncendido = response.estado
                println("Estado inicial del ventilador: ${response.estado}")
            } catch (e: Exception) {
                println("Error al cargar estado inicial del ventilador: ${e.message}")
            }
        }
    }
    LaunchedEffect(Unit) {
        val uriTanque = SensorImageStore.getImageUri(context, "gas", 1).firstOrNull()
        if (uriTanque != null) sensorTanque = sensorTanque.copy(imageUri = uriTanque)

        val uriCocina = SensorImageStore.getImageUri(context, "gas", 2).firstOrNull()
        if (uriCocina != null) sensorCocina = sensorCocina.copy(imageUri = uriCocina)

        // Cargar estado inicial del ventilador
        loadInitialVentiladorState()
    }
    // Lectura periódica de ambos sensores
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val api = getApi(context)
                val responseTanque = api.obtenerGas()
                lecturaGas = responseTanque.valor

                val responseCocina = api.obtenerGasCocina()
                lecturaGasCocina = responseCocina.valor

                // Actualizar estado de fugas
                sensorTanque = sensorTanque.copy(hasLeak = lecturaGas > 500) // Ajusta el umbral según necesites
                sensorCocina = sensorCocina.copy(hasLeak = lecturaGasCocina > 500)
            } catch (e: Exception) {
                println("Error al leer sensores: ${e.message}")
            }
            delay(5000)
        }
    }

    fun toggleVentilador() {
        val nuevoEstado = !ventiladorEncendido
        scope.launch {
            isLoading = true
            try {
                val api = getApi(context)
                val response = api.cambiarEstadoVentilador(ApiService.VentiladorRequest(nuevoEstado))

                if (response.isSuccessful) {
                    response.body()?.let {
                        ventiladorEncendido = it.estado
                        println("Ventilador actualizado a: ${it.estado}")
                    } ?: run {
                        println("Respuesta vacía del servidor")
                        // Forzar actualización del estado
                        val estadoActual = api.obtenerEstadoVentilador()
                        ventiladorEncendido = estadoActual.estado
                    }
                } else {
                    println("Error en la respuesta: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                println("Error al cambiar estado del ventilador: ${e.message}")
                // Recuperar estado actual si hay error
                try {
                    val api = getApi(context)
                    val estadoActual = api.obtenerEstadoVentilador()
                    ventiladorEncendido = estadoActual.estado
                } catch (e: Exception) {
                    println("Error al recuperar estado: ${e.message}")
                }
            } finally {
                isLoading = false
            }
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

                        Text(
                            "Lectura: $lecturaGas",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Button(
                            onClick = { toggleVentilador() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                            colors = if (ventiladorEncendido)
                                ButtonDefaults.buttonColors(containerColor = Color.Red)
                            else
                                ButtonDefaults.buttonColors(containerColor = VerdeEcologico)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    if (ventiladorEncendido) "Apagar Ventilador" else "Encender Ventilador",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        if (sensorTanque.hasLeak) {
                            Text(
                                "⚠️ Posible fuga detectada!",
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

                        Text(
                            "Lectura: $lecturaGasCocina",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Button(
                            onClick = { toggleVentilador() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                            colors = if (ventiladorEncendido)
                                ButtonDefaults.buttonColors(containerColor = Color.Red)
                            else
                                ButtonDefaults.buttonColors(containerColor = VerdeEcologico)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    if (ventiladorEncendido) "Apagar Ventilador" else "Encender Ventilador",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        if (sensorCocina.hasLeak) {
                            Text(
                                "⚠️ Posible fuga detectada!",
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