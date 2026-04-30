package com.shubham.rcreceiver.data.repositories

import android.util.Log
import com.shubham.rcreceiver.data.local.SerialPortDataSource
import com.shubham.rcreceiver.data.remote.UDPDataSource
import com.shubham.rcreceiver.data.sensor.SensorDataCollector
import com.shubham.rcreceiver.domain.models.SerialData
import com.shubham.rcreceiver.domain.models.TelemetryData
import com.shubham.rcreceiver.domain.repositories.ITelemetryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelemetryRepositoryImpl @Inject constructor(
    private val udpDataSource: UDPDataSource,
    private val sensorCollector: SensorDataCollector,
    private val serialDataSource: SerialPortDataSource
) : ITelemetryRepository {
    
    override suspend fun getTelemetryStream(): Flow<TelemetryData> = flow {
        combine(
            sensorCollector.sensorDataFlow,
            udpDataSource.signalFlow.stateIn(CoroutineScope(Dispatchers.Default)),
            serialDataSource.serialDataFlow.stateIn(CoroutineScope(Dispatchers.Default))
        ) { sensor, signal, serial ->
            if (sensor != null) {
                TelemetryData(
                    sensorData = sensor,
                    udpSignal = signal,
                    esp32Data = SerialData(serial)
                )
            } else {
                null
            }
        }.collect { data ->
            if (data != null) {
                emit(data)
            }
        }
    }
    
    override suspend fun sendTelemetry(data: TelemetryData, host: String, port: Int) {
        udpDataSource.sendTelemetry(data, host, port)
    }
}

class SensorRepositoryImpl @Inject constructor(
    private val sensorCollector: SensorDataCollector
) : com.shubham.rcreceiver.domain.repositories.ISensorRepository {
    
    override suspend fun startSensorCollection() {
        sensorCollector.startCollecting()
    }
    
    override suspend fun stopSensorCollection() {
        sensorCollector.stopCollecting()
    }
}

class SerialRepositoryImpl @Inject constructor(
    private val serialDataSource: SerialPortDataSource
) : com.shubham.rcreceiver.domain.repositories.ISerialRepository {
    
    override suspend fun connectToESP32(baudRate: Int) {
        serialDataSource.connectToESP32(baudRate)
    }
    
    override suspend fun disconnect() {
        serialDataSource.disconnect()
    }
    
    override suspend fun writeData(data: ByteArray) {
        serialDataSource.writeData(data)
    }
}
