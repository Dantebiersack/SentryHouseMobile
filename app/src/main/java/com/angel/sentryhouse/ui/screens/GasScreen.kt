package com.angel.sentryhouse.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

    var sensorList by remember {
        mutableStateOf(
            listOf(
                SensorItem(1, "Sensor 1"),
                SensorItem(2, "Sensor 2"),
                SensorItem(3, "Sensor 3"),
                SensorItem(4, "Sensor 4")
            )
        )
    }

    LaunchedEffect(Unit) {
        sensorList = sensorList.map { sensor ->
            val uri = SensorImageStore.getImageUri(context, "gas", sensor.id).firstOrNull()
            if (uri != null) sensor.copy(imageUri = uri) else sensor
        }
    }

    // Cargar imágenes guardadas
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val api = getApi(context)
                val response = api.obtenerGas()
                lecturaGas = response.valor
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(5000)  // actualizar cada 5 segundos
        }
    }

    var selectedSensor by remember { mutableStateOf<SensorItem?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedSensor?.let { sensor ->
                sensorList = sensorList.map {
                    if (it.id == sensor.id) it.copy(imageUri = uri) else it
                }
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
            selectedSensor?.let { sensor ->
                sensorList = sensorList.map {
                    if (it.id == sensor.id) it.copy(imageUri = tempCameraUri) else it
                }
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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    sensorList = sensorList.map { it.copy(isClosed = true) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text("Cerrar todos")
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sensorList) { sensor ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedSensor = sensor
                                tempCameraUri = createImageUri(context)
                                showDialog = true
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .fillMaxWidth()
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

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Estado: ${if (sensor.isClosed) "Cerrado" else "Abierto"}")

                            Switch(
                                checked = sensor.isClosed,
                                onCheckedChange = { isChecked ->
                                    sensorList = sensorList.map {
                                        if (it.id == sensor.id) it.copy(isClosed = isChecked) else it
                                    }
                                }
                            )

                            if (sensor.hasLeak) {
                                Text("⚠️ Posible fuga", color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog && selectedSensor != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Seleccionar imagen") },
            text = { Text("¿Deseas tomar una foto o elegir una de la galería?") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    tempCameraUri?.let { cameraLauncher.launch(it) }
                }) {
                    Text("Tomar foto")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    galleryLauncher.launch("image/*")
                }) {
                    Text("Galería")
                }
            }
        )
    }
}
