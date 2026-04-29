package com.shubham.rcreceiver.domain.repositories

import com.shubham.rcreceiver.domain.models.TelemetryData
import kotlinx.coroutines.flow.Flow

interface ITelemetryRepository {
    suspend fun getTelemetryStream(): Flow<TelemetryData>
    suspend fun sendTelemetry(data: TelemetryData, host: String, port: Int)
}

interface ISensorRepository {
    suspend fun startSensorCollection()
    suspend fun stopSensorCollection()
}

interface ISerialRepository {
    suspend fun connectToESP32(baudRate: Int = 115200)
    suspend fun disconnect()
    suspend fun writeData(data: ByteArray)
}
