package com.shubham.rcreceiver.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import kotlin.math.roundToInt

@Composable
fun SensorScreen(
    navController: NavHostController,
    viewModel: TelemetryViewModel
) {
    val telemetryData = viewModel.telemetryData.collectAsState().value
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues())
            .padding(16.dp)
    ) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            Text("Back")
        }
        
        Text(
            text = "Sensor Readings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (telemetryData != null) {
            val sensor = telemetryData.sensorData
            
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                CompassDisplay(bearing = sensor.bearing)
                MetricsGrid(telemetryData)
                AccuracyIndicator(accuracy = sensor.accuracy)
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun CompassDisplay(bearing: Float) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Compass Heading", style = MaterialTheme.typography.titleMedium)
            
            Text(
                text = "%.1f°".format(bearing),
                style = MaterialTheme.typography.displayMedium
            )
            
            Text(
                text = getCompassDirection(bearing),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MetricsGrid(data: TelemetryData) {
    val metrics = listOf(
        Triple("Altitude", "%.0f m".format(data.sensorData.altitude), "⬆"),
        Triple("Speed", "%.1f m/s".format(data.sensorData.speed), "→"),
        Triple("Accuracy", "%.1f m".format(data.sensorData.accuracy), "◯"),
        Triple("Latitude", "%.4f".format(data.sensorData.latitude), "N"),
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        metrics.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (label, value, icon) ->
                    MetricCard(label, value, icon, Modifier.weight(1f))
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, icon: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
fun AccuracyIndicator(accuracy: Float) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Signal Accuracy", style = MaterialTheme.typography.titleMedium)
            
            val accuracyPercent = ((100 - accuracy.coerceIn(0f, 100f)) / 100f * 100).roundToInt()
            
            LinearProgressIndicator(
                progress = { accuracyPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
            
            Text(
                text = "$accuracyPercent% (±${"%.1f".format(accuracy)}m)",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun getCompassDirection(bearing: Float): String {
    return when {
        bearing < 22.5 -> "N"
        bearing < 67.5 -> "NE"
        bearing < 112.5 -> "E"
        bearing < 157.5 -> "SE"
        bearing < 202.5 -> "S"
        bearing < 247.5 -> "SW"
        bearing < 292.5 -> "W"
        bearing < 337.5 -> "NW"
        else -> "N"
    }
}
