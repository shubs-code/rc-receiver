package com.shubham.rcreceiver.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shubham.rcreceiver.data.repositories.SensorRepositoryImpl
import com.shubham.rcreceiver.data.repositories.SerialRepositoryImpl
import com.shubham.rcreceiver.data.repositories.TelemetryRepositoryImpl
import com.shubham.rcreceiver.domain.models.TelemetryData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TelemetryViewModel @Inject constructor(
    private val telemetryRepository: TelemetryRepositoryImpl,
    private val sensorRepository: SensorRepositoryImpl,
    private val serialRepository: SerialRepositoryImpl
) : ViewModel() {
    
    private val _telemetryData = MutableStateFlow<TelemetryData?>(null)
    val telemetryData: StateFlow<TelemetryData?> = _telemetryData.asStateFlow()
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    fun startTelemetryCollection() {
        viewModelScope.launch {
            try {
                telemetryRepository.getTelemetryStream().collect { data ->
                    _telemetryData.value = data
                    _isConnected.value = true
                    _errorMessage.value = null
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isConnected.value = false
                Log.e("TelemetryViewModel", "Error collecting telemetry", e)
            }
        }
    }
    
    fun sendTelemetry(host: String = "192.168.1.100", port: Int = 5001) {
        viewModelScope.launch {
            _telemetryData.value?.let { data ->
                try {
                    telemetryRepository.sendTelemetry(data, host, port)
                    Log.d("TelemetryViewModel", "Telemetry sent to $host:$port")
                } catch (e: Exception) {
                    _errorMessage.value = "Failed to send telemetry: ${e.message}"
                    Log.e("TelemetryViewModel", "Error sending telemetry", e)
                }
            }
        }
    }
    
    fun initializeConnections() {
        viewModelScope.launch {
            try {
                _isConnected.value = false
                sensorRepository.startSensorCollection()
                serialRepository.connectToESP32()
                startTelemetryCollection()
                _isInitialized.value = true
                Log.d("TelemetryViewModel", "Connections initialized")
            } catch (e: Exception) {
                _errorMessage.value = "Failed to initialize: ${e.message}"
                _isInitialized.value = false
                Log.e("TelemetryViewModel", "Error initializing connections", e)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                sensorRepository.stopSensorCollection()
                serialRepository.disconnect()
            } catch (e: Exception) {
                Log.e("TelemetryViewModel", "Error during cleanup", e)
            }
        }
    }
}
