package com.shubham.rcreceiver.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shubham.rcreceiver.presentation.screens.MainScreen
import com.shubham.rcreceiver.presentation.screens.SensorScreen
import com.shubham.rcreceiver.presentation.screens.SerialDebugScreen
import com.shubham.rcreceiver.presentation.screens.TelemetryScreen
import com.shubham.rcreceiver.presentation.viewmodels.TelemetryViewModel
import com.shubham.rcreceiver.utils.PermissionManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private var permissionsGranted = mutableStateOf(false)
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        permissionsGranted.value = allGranted
        
        if (!allGranted) {
            val deniedPermissions = permissions.filter { !it.value }.keys
            android.util.Log.w("Permissions", "Denied: $deniedPermissions")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestRequiredPermissions()
        
        setContent {
            MaterialTheme {
                if (permissionsGranted.value) {
                    AppContent()
                } else {
                    PermissionScreen(onRetry = { requestRequiredPermissions() })
                }
            }
        }
    }
    
    private fun requestRequiredPermissions() {
        val permissionsToRequest = PermissionManager.REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest)
        } else {
            permissionsGranted.value = true
        }
    }
}

@Composable
fun AppContent() {
    val navController = rememberNavController()
    val viewModel: TelemetryViewModel = hiltViewModel()
    
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(navController = navController, viewModel = viewModel)
        }
        composable("telemetry") {
            TelemetryScreen(navController = navController, viewModel = viewModel)
        }
        composable("sensors") {
            SensorScreen(navController = navController, viewModel = viewModel)
        }
        composable("serial_debug") {
            SerialDebugScreen(navController = navController, viewModel = viewModel)
        }
    }
}

@Composable
fun PermissionScreen(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(16.dp))
        
        Text(
            "Permissions Required",
            style = MaterialTheme.typography.headlineSmall
        )
        
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
        
        Text(
            "This app requires location, internet, and USB permissions to function properly.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
        
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(16.dp))
        
        Button(onClick = onRetry) {
            Text("Grant Permissions")
        }
    }
}
