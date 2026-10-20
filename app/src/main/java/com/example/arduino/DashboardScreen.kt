package com.example.arduino

import android.widget.Toast
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToBluetooth: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var humedad by remember { mutableIntStateOf(20) }
    var temperatura by remember { mutableIntStateOf(24) }
    var nivelAgua by remember { mutableIntStateOf(80) }
    var automaticRiego by remember { mutableStateOf(false) }

    // Referencia al nodo de tu Arduino en la base de datos
    val database = remember { FirebaseDatabase.getInstance().reference.child("arduino_test") }

    // Lanzamos la conexión a Firebase al abrir la pantalla
    LaunchedEffect(Unit) {
        database.child("sensores").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hum = snapshot.child("humedad").getValue(Int::class.java)
                if (hum != null) humedad = hum.coerceIn(0, 100)
                
                val temp = snapshot.child("temperatura").getValue(Int::class.java)
                if (temp != null) temperatura = temp

                val agua = snapshot.child("nivel_agua").getValue(Int::class.java)
                if (agua != null) nivelAgua = agua
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
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión")
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SuggestionChip(onClick = {}, label = { Text("Conectado (Wi-Fi)") })
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
                        onCheckedChange = { newValue ->
                            database.child("controles").child("riego_automatico").setValue(newValue)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = onNavigateToBluetooth,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text("CONEXIÓN LOCAL (BLUETOOTH)")
            }

            Button(
                onClick = {
                    val timestamp = System.currentTimeMillis()
                    val fechaFormateada = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

                    // Incrementar el % de humedad simulada en +15% (hasta un máximo de 100%)
                    val nuevaHumedad = (humedad + 15).coerceAtMost(100)

                    // 1. Actualizar el % de humedad en la base de datos en tiempo real
                    database.child("sensores").child("humedad").setValue(nuevaHumedad)
                    database.child("sensores").child("temperatura").setValue(temperatura)
                    database.child("sensores").child("nivel_agua").setValue(nivelAgua)

                    // 2. Activar la orden de riego e indicar la última fecha
                    database.child("controles").child("regar_ahora").setValue(true)
                    database.child("controles").child("ultimo_riego").setValue(fechaFormateada)

                    // 3. Guardar el registro en el historial de riego en Firebase
                    val nuevoRegistro = mapOf(
                        "fecha" to fechaFormateada,
                        "timestamp" to timestamp,
                        "humedad" to nuevaHumedad,
                        "temperatura" to temperatura,
                        "nivel_agua" to nivelAgua,
                        "tipo" to "Manual (App)"
                    )
                    database.child("historial_riego").push().setValue(nuevoRegistro)

                    Toast.makeText(context, "¡Riego activado! Humedad subió a $nuevaHumedad%", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("REGAR AHORA")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    database.child("sensores").child("humedad").setValue(20)
                    database.child("sensores").child("temperatura").setValue(24)
                    database.child("sensores").child("nivel_agua").setValue(80)
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
