package com.example.arduino

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScreen(
    onNavigateBack: () -> Unit,
    onBluetoothConnected: () -> Unit
) {
    val context = LocalContext.current
    var isScanning by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val canEnableBle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions[Manifest.permission.BLUETOOTH_SCAN] == true && permissions[Manifest.permission.BLUETOOTH_CONNECT] == true
        } else {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        }

        if (canEnableBle) {
            isScanning = true
            Toast.makeText(context, "Escaneando dispositivos BLE...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permisos denegados.", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conexión Local (BLE)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues).padding(16.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Modo Offline (Directo a la placa)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
                    } else {
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    permissionLauncher.launch(permissionsToRequest)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isScanning) "BUSCANDO ARDUINO..." else "ESCANEAR DISPOSITIVOS BLE")
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isScanning) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Arduino Riego (HW-038)", fontWeight = FontWeight.Bold)
                            Text("00:1A:7D:DA:71:13", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = {
                            isConnected = true
                            isScanning = false
                            onBluetoothConnected()
                            Toast.makeText(context, "Conectado vía GATT por Bluetooth", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Conectar")
                        }
                    }
                }
            }

            if (isConnected) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { Toast.makeText(context, "Comando '1' (Regar) enviado por BLE", Toast.LENGTH_SHORT).show() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) {
                    Text("FORZAR RIEGO (OFFLINE)")
                }
            }
        }
    }
}
