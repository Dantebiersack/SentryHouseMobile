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

    var sensor by remember {
        mutableStateOf(SensorItem(1, "Sensor Principal"))
    }

    LaunchedEffect(Unit) {
        val uri = SensorImageStore.getImageUri(context, "gas", sensor.id).firstOrNull()
        if (uri != null) sensor = sensor.copy(imageUri = uri)
    }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                val api = getApi(context)
                val response = api.obtenerGas()
                lecturaGas = response.valor
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(5000) // Actualizar cada 5 segundos
        }
    }

    var showDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            sensor = sensor.copy(imageUri = uri)
            scope.launch {
                SensorImageStore.saveImageUri(context, "gas", sensor.id, uri)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            sensor = sensor.copy(imageUri = tempCameraUri)
            scope.launch {
                SensorImageStore.saveImageUri(context, "gas", sensor.id, tempCameraUri!!)
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
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .clickable {
                        tempCameraUri = createImageUri(context)
                        showDialog = true
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Lectura de Gas: $lecturaGas")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Estado: ${if (sensor.isClosed) "Cerrado" else "Abierto"}")

                    Switch(
                        checked = sensor.isClosed,
                        onCheckedChange = { isChecked ->
                            sensor = sensor.copy(isClosed = isChecked)
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    if (sensor.hasLeak) {
                        Text("⚠️ Posible fuga", color = Color.Red, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }

    if (showDialog) {
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
                    // boton para quitar la foto
                    if (sensor.imageUri != null) { // Solo mostrar si hay una imagen
                        TextButton(onClick = {
                            showDialog = false
                            sensor = sensor.copy(imageUri = null) //Limpia la uri
                            scope.launch {
                                SensorImageStore.saveImageUri(context, "gas", sensor.id, null) // Guardar null
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