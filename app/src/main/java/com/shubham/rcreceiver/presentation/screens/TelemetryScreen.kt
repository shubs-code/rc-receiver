package com.shubham.rcreceiver.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.shubham.rcreceiver.domain.models.TelemetryData
import com.shubham.rcreceiver.presentation.viewmodels.TelemetryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TelemetryScreen(
    navController: NavHostController,
    viewModel: TelemetryViewModel
) {
    val telemetryData = viewModel.telemetryData.collectAsState().value
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
            Text("Back")
        }
        
        Text(
            text = "Live Telemetry Data",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (telemetryData != null) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TelemetryCard("GPS Data", telemetryData)
                SensorCard("Sensor Data", telemetryData)
                SignalCard("UDP Signal", telemetryData)
                ESP32Card("ESP32 Data", telemetryData)
            }
        } else {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun TelemetryCard(title: String, data: TelemetryData) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Divider()
            
            DataRow("Latitude", "%.6f".format(data.sensorData.latitude))
            DataRow("Longitude", "%.6f".format(data.sensorData.longitude))
            DataRow("Altitude", "%.2f m".format(data.sensorData.altitude))
            DataRow("Accuracy", "%.2f m".format(data.sensorData.accuracy))
            DataRow("Speed", "%.2f m/s".format(data.sensorData.speed))
        }
    }
}

@Composable
fun SensorCard(title: String, data: TelemetryData) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Divider()
            
            DataRow("Bearing", "%.1f°".format(data.sensorData.bearing))
            DataRow("Timestamp", formatTimestamp(data.timestamp))
            DataRow("ID", data.id.take(8) + "...")
        }
    }
}

@Composable
fun SignalCard(title: String, data: TelemetryData) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Divider()
            
            if (data.udpSignal != null) {
                Text(
                    data.udpSignal,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                Text(
                    "No UDP signal received",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ESP32Card(title: String, data: TelemetryData) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Divider()
            
            if (data.esp32Data != null) {
                DataRow(
                    "Payload Size",
                    "${data.esp32Data.payload.size} bytes"
                )
                DataRow(
                    "Payload (Hex)",
                    data.esp32Data.payload.joinToString("") {
                        "%02x".format(it)
                    }.take(32) + "..."
                )
            } else {
                Text(
                    "No ESP32 data received",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return SimpleDateFormat(
        "HH:mm:ss",
        Locale.getDefault()
    ).format(Date(timestamp))
}
