package com.example.arduino

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.database.FirebaseDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    var humedadMin by remember { mutableFloatStateOf(30f) }
    var humedadMax by remember { mutableFloatStateOf(70f) }
    val database = remember { FirebaseDatabase.getInstance().reference.child("arduino_test").child("configuracion") }

    LaunchedEffect(Unit) {
        database.child("humedad_min").get().addOnSuccessListener { if (it.exists()) (it.value as? Number)?.toFloat()?.let { v -> humedadMin = v } }
        database.child("humedad_max").get().addOnSuccessListener { if (it.exists()) (it.value as? Number)?.toFloat()?.let { v -> humedadMax = v } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes de Riego") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("LÍMITES DE HUMEDAD", color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Humedad Mínima para encender bomba: ${humedadMin.toInt()}%")
            Slider(
                value = humedadMin,
                onValueChange = { humedadMin = it },
                valueRange = 0f..50f,
                onValueChangeFinished = { database.child("humedad_min").setValue(humedadMin.toInt()) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Humedad Máxima para apagar bomba: ${humedadMax.toInt()}%")
            Slider(
                value = humedadMax,
                onValueChange = { humedadMax = it },
                valueRange = 51f..100f,
                onValueChangeFinished = { database.child("humedad_max").setValue(humedadMax.toInt()) }
            )
        }
    }
}
