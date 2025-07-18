package com.angel.sentryhouse.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlin.text.isNotBlank
import kotlin.text.trim

//Pantalla que permite al usuario configurar la URL del servidor.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
    var url by remember {
        mutableStateOf(TextFieldValue(prefs.getString("url", "") ?: ""))
    }
    var urlCocina by remember {
        mutableStateOf(TextFieldValue(prefs.getString("url_cocina", "") ?: ""))
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
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
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Campo para ingresar la URL del servidor
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL del sensor TANQUE") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = urlCocina,
                onValueChange = { urlCocina = it },
                label = { Text("URL del sensor COCINA") },
                modifier = Modifier.fillMaxWidth()
            )
            // Botón para guardar la URL
            Button(
                onClick = {
                    if (url.text.isNotBlank()) {
                        prefs.edit()
                            .putString("base_url", url.text.trim())
                            .putString("url_cocina", urlCocina.text.trim()) // puede estar vacía
                            .apply()
                        Toast.makeText(context, "Configuración guardada", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    } else {
                        Toast.makeText(context, "La URL del Tanqui no puede estar vacía", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar URLs")
            }


        }
    }
}
