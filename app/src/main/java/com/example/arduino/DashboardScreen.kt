package com.example.arduino

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Funciones auxiliares para física simulada de sensores y formato de fecha
private fun calcTemp(hum: Int) = (35 - (hum * 0.15f)).toInt().coerceIn(18, 38)
private fun calcAgua(hum: Int) = (100 - (hum * 0.5f)).toInt().coerceIn(15, 100)
private fun formatFecha(ts: Long) = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(ts))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToBluetooth: () -> Unit,
    onSignOut: () -> Unit,
    isBluetoothMode: Boolean = false,
    onSwitchToWiFi: () -> Unit = {}
) {
    val context = LocalContext.current
    var humedad by remember { mutableIntStateOf(20) }
    var temperatura by remember { mutableIntStateOf(32) }
    var nivelAgua by remember { mutableIntStateOf(90) }
    var automaticRiego by remember { mutableStateOf(false) }

    var notified25 by remember { mutableStateOf(false) }
    var notified50 by remember { mutableStateOf(false) }
    var notified100 by remember { mutableStateOf(false) }

    val database = remember { FirebaseDatabase.getInstance().reference.child("arduino_test") }

    // Solicitar permiso de notificaciones en Android 13+ (API 33+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            when {
                humedad <= 25 -> {
                    NotificationHelper.showNotification(context, 2, "Alerta: Suelo Seco ($humedad%)", "Humedad baja ($humedad%). Requiere riego.")
                    notified25 = true
                }
                humedad in 50..99 -> {
                    NotificationHelper.showNotification(context, 1, "Humedad Óptima ($humedad%)", "Humedad del suelo óptima ($humedad%).")
                    notified50 = true
                }
                humedad >= 100 -> {
                    NotificationHelper.showNotification(context, 3, "Humedad Máxima - Suelo Saturado (100%)", "Humedad al 100%. Tierra regada.")
                    notified100 = true
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Suscripciones a Firebase
        database.child("sensores").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.child("humedad").getValue(Int::class.java)?.let { humedad = it.coerceIn(0, 100) }
                snapshot.child("temperatura").getValue(Int::class.java)?.let { temperatura = it }
                snapshot.child("nivel_agua").getValue(Int::class.java)?.let { nivelAgua = it }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        database.child("controles").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                automaticRiego = snapshot.child("riego_automatico").getValue(Boolean::class.java) ?: false
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Notificaciones en 2do plano al alcanzar umbrales
    LaunchedEffect(humedad) {
        when {
            humedad <= 25 && !notified25 -> {
                NotificationHelper.showNotification(context, 2, "Alerta: Suelo Muy Seco ($humedad%)", "Humedad bajó al $humedad%. Activar riego.")
                notified25 = true; notified50 = false; notified100 = false
            }
            humedad in 50..99 && !notified50 -> {
                NotificationHelper.showNotification(context, 1, "Humedad Óptima ($humedad%)", "Humedad del suelo alcanzó $humedad%.")
                notified50 = true; notified25 = false; notified100 = false
            }
            humedad >= 100 && !notified100 -> {
                NotificationHelper.showNotification(context, 3, "Humedad Máxima - Suelo Saturado (100%)", "Humedad alcanzó 100%. Tierra regada.")
                notified100 = true; notified50 = false; notified25 = false
            }
            humedad in 26..49 -> { notified25 = false; notified50 = false }
            humedad in 51..99 -> { notified50 = false; notified100 = false }
        }
    }

    val estadoHumedad = when {
        humedad < 30 -> "Seca"
        humedad in 30..70 -> "Óptima"
        else -> "Exceso"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel Central") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, contentDescription = "Ajustes") }
                    IconButton(onClick = onSignOut) { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SuggestionChip(
                onClick = {},
                label = { Text(if (isBluetoothMode) "Conectado (Bluetooth BLE)" else "Conectado (Wi-Fi)") },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = if (isBluetoothMode) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Humedad del suelo", style = MaterialTheme.typography.titleMedium)
                    Text("$humedad% ($estadoHumedad)", style = MaterialTheme.typography.headlineMedium)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Temperatura")
                        Text("$temperatura °C", style = MaterialTheme.typography.titleLarge)
                    }
                }
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Nivel de agua")
                        Text("$nivelAgua%", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Riego Automático", style = MaterialTheme.typography.titleMedium)
                        Text("Control por sensores", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = automaticRiego,
                        onCheckedChange = { newValue -> database.child("controles").child("riego_automatico").setValue(newValue) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isBluetoothMode) {
                Button(
                    onClick = {
                        onSwitchToWiFi()
                        Toast.makeText(context, "Modo Wi-Fi (Nube) reconectado", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("RECONECTAR A WI-FI (NUBE)")
                }
            } else {
                OutlinedButton(
                    onClick = onNavigateToBluetooth,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text("CONEXIÓN LOCAL (BLUETOOTH)")
                }
            }

            Button(
                onClick = {
                    val ts = System.currentTimeMillis()
                    val fechaStr = formatFecha(ts)
                    val nuevaHum = (humedad + 15).coerceAtMost(100)
                    val nuevaTemp = calcTemp(nuevaHum)
                    val nuevoAgua = calcAgua(nuevaHum)

                    database.child("sensores").child("humedad").setValue(nuevaHum)
                    database.child("sensores").child("temperatura").setValue(nuevaTemp)
                    database.child("sensores").child("nivel_agua").setValue(nuevoAgua)

                    database.child("controles").child("regar_ahora").setValue(true)
                    database.child("controles").child("ultimo_riego").setValue(fechaStr)

                    database.child("historial_riego").push().setValue(
                        mapOf(
                            "fecha" to fechaStr, "timestamp" to ts, "humedad" to nuevaHum,
                            "temperatura" to nuevaTemp, "nivel_agua" to nuevoAgua,
                            "tipo" to if (isBluetoothMode) "Manual (BLE)" else "Manual (App)"
                        )
                    )

                    Toast.makeText(context, "¡Riego activado! Humedad: $nuevaHum%, Temp: $nuevaTemp°C, Agua: $nuevoAgua%", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("REGAR AHORA")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    database.child("sensores").child("humedad").setValue(20)
                    database.child("sensores").child("temperatura").setValue(32)
                    database.child("sensores").child("nivel_agua").setValue(90)
                    database.child("controles").child("regar_ahora").setValue(false)
                    Toast.makeText(context, "Valores de sensores restablecidos", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text("RESTABLECER SENSORES")
            }

            FilledTonalButton(
                onClick = onSignOut,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CERRAR SESIÓN")
            }
        }
    }
}
