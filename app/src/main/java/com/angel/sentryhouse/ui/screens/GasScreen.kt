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

data class SensorItem(
    val id: Int,
    val name: String,
    val imageUri: Uri? = null,
    val isClosed: Boolean = false,
    val hasLeak: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GasScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var lecturaGas by remember { mutableStateOf(0) }
    var lecturaGasCocina by remember { mutableStateOf(0) }

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

    // Lectura periódica de ambos sensores
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val api = getApi(context)
                val responseTanque = api.obtenerGas()
                lecturaGas = responseTanque.valor

                val responseCocina = api.obtenerGasCocina()
                lecturaGasCocina = responseCocina.valor
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
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()), // ✅ Scroll vertical
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    sensorTanque to lecturaGas,
                    sensorCocina to lecturaGasCocina
                ).forEach { (sensor, lectura) ->
                    Card(
                        modifier = Modifier
                            .width(160.dp)
                            .height(250.dp)
                            .clickable {
                                tempCameraUri = createImageUri(context)
                                editingSensor = sensor
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
                                sensor.imageUri?.let {
                                    Image(
                                        painter = rememberAsyncImagePainter(it),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } ?: Text(sensor.name, color = Color.White)
                            }

                            Text(
                                "Lectura: $lectura",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            Button(
                                onClick = { /* Acción de ventilación */ },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Ventilar", style = MaterialTheme.typography.labelLarge)
                            }

                            if (sensor.hasLeak) {
                                Text(
                                    "⚠️ Posible fuga",
                                    color = Color.Red,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
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
