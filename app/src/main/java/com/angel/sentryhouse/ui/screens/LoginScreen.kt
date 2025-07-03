package com.angel.sentryhouse.ui.screens

import com.angel.sentryhouse.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun LoginScreen(navController: NavController) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logosh),
                contentDescription = "SentryHouse Logo",
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(24.dp))
            )

            Text("SentryHouse", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = user,
                onValueChange = { user = it },
                label = { Text("Usuario") },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            var pass by remember { mutableStateOf("") }
            var passwordVisible by remember { mutableStateOf(false) }

            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Default.CheckCircle else Icons.Default.CheckCircle
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(icon, contentDescription = "Mostrar/Ocultar contraseña")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (user == "root" && pass == "root") {
                        navController.navigate("home")
                    }
                }
            ) {
                Text("Iniciar")
            }
        }
    }
}
