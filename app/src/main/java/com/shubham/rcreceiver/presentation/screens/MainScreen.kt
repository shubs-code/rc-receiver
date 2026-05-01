package com.shubham.rcreceiver.presentation.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.shubham.rcreceiver.presentation.viewmodels.TelemetryViewModel
import com.shubham.rcreceiver.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MainScreen(
    navController: NavHostController,
    viewModel: TelemetryViewModel
) {
    val isConnected = viewModel.isConnected.collectAsState().value
    val errorMessage = viewModel.errorMessage.collectAsState().value
    Log.d("Mainscreen", "In main Mainscreen")
    val ipv6Addresses = remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        ipv6Addresses.value = withContext(Dispatchers.Default) {
            NetworkUtils.getPublicIPv6Addresses()
        }
        viewModel.initializeConnections()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "RC Receiver",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        StatusCard(
            title = "System Status",
            isConnected = isConnected,
            errorMessage = errorMessage,
            ipv6Addresses = ipv6Addresses.value
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        NavigationButton(
            title = "Live Telemetry",
            description = "View real-time telemetry data",
            icon = Icons.Default.SignalCellularAlt,
            onClick = { navController.navigate("telemetry") }
        )
        
        NavigationButton(
            title = "Sensor Data",
            description = "GPS, Compass, Altitude readings",
            icon = Icons.Default.LocationOn,
            onClick = { navController.navigate("sensors") }
        )
        
        NavigationButton(
            title = "Serial Debug",
            description = "Serial communication logs",
            icon = Icons.Default.BugReport,
            onClick = { navController.navigate("serial_debug") }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { viewModel.sendTelemetry() }
        ) {
            Icon(Icons.Default.Send, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send Telemetry")
        }
    }
}

@Composable
fun StatusCard(
    title: String,
    isConnected: Boolean,
    errorMessage: String?,
    ipv6Addresses: List<String> = emptyList()
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (isConnected)
                        Icons.Default.CheckCircle
                    else
                        Icons.Default.Error,
                    contentDescription = null,
                    tint = if (isConnected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isConnected)
                            "All systems operational"
                        else
                            errorMessage ?: "Initializing...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (ipv6Addresses.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "IPv6 Addresses:",
                        style = MaterialTheme.typography.labelSmall
                    )

                    ipv6Addresses.forEach { address ->
                        Text(
                            text = address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationButton(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    }
}
