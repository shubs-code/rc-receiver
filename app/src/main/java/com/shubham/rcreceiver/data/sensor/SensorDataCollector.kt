package com.shubham.rcreceiver.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.shubham.rcreceiver.domain.models.SensorData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class SensorDataCollector @Inject constructor(
    private val context: Context,
    private val fusedLocationProvider: FusedLocationProviderClient,
    private val sensorManager: SensorManager
) {
    private var fusedLocationListener: LocationCallback? = null
    private var compassListener: SensorEventListener? = null
    
    private val _sensorDataFlow = MutableStateFlow<SensorData?>(null)
    val sensorDataFlow: StateFlow<SensorData?> = _sensorDataFlow.asStateFlow()
    
    fun startCollecting() {
        try {
            // GPS/Location
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                1000
            ).setMinUpdateIntervalMillis(500).build()
            
            fusedLocationListener = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation
                    if (location != null) {
                        updateSensorData(location)
                    }
                }
            }
            
            try {
                fusedLocationProvider.requestLocationUpdates(
                    locationRequest,
                    fusedLocationListener!!,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                Log.e("Sensor", "Location permission denied: ${e.message}")
            }
            
            setupCompass()
        } catch (e: Exception) {
            Log.e("Sensor", "Error starting collection: ${e.message}")
        }
    }
    
    private fun setupCompass() {
        try {
            val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            
            compassListener = object : SensorEventListener {
                private var magneticValues = FloatArray(3)
                private var accelerometerValues = FloatArray(3)
                
                override fun onSensorChanged(event: SensorEvent) {
                    when (event.sensor.type) {
                        Sensor.TYPE_MAGNETIC_FIELD -> magneticValues = event.values.copyOf()
                        Sensor.TYPE_ACCELEROMETER -> accelerometerValues = event.values.copyOf()
                    }
                    calculateBearing()
                }
                
                private fun calculateBearing() {
                    val rotationMatrix = FloatArray(9)
                    val inclinationMatrix = FloatArray(9)
                    val orientations = FloatArray(3)
                    
                    SensorManager.getRotationMatrix(
                        rotationMatrix, inclinationMatrix,
                        accelerometerValues, magneticValues
                    )
                    SensorManager.getOrientation(rotationMatrix, orientations)
                    
                    val bearing = Math.toDegrees(orientations[0].toDouble()).toFloat()
                    _sensorDataFlow.value = _sensorDataFlow.value?.copy(
                        bearing = if (bearing < 0) bearing + 360 else bearing
                    )
                }
                
                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
            }
            
            if (magnetometer != null && accelerometer != null) {
                sensorManager.registerListener(
                    compassListener,
                    magnetometer,
                    SensorManager.SENSOR_DELAY_UI
                )
                sensorManager.registerListener(
                    compassListener,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_UI
                )
            }
        } catch (e: Exception) {
            Log.e("Sensor", "Error setting up compass: ${e.message}")
        }
    }
    
    private fun updateSensorData(location: Location) {
        _sensorDataFlow.value = _sensorDataFlow.value?.copy(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            accuracy = location.accuracy,
            speed = location.speed
        ) ?: SensorData(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            bearing = 0f,
            accuracy = location.accuracy,
            speed = location.speed
        )
    }
    
    fun stopCollecting() {
        try {
            fusedLocationListener?.let {
                fusedLocationProvider.removeLocationUpdates(it)
            }
            compassListener?.let {
                sensorManager.unregisterListener(it)
            }
        } catch (e: Exception) {
            Log.e("Sensor", "Error stopping collection: ${e.message}")
        }
    }
}
