package com.shubham.rcreceiver.domain.models

import kotlinx.serialization.Serializable
import java.util.UUID

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class TelemetryData(
    val timestamp: Long = System.currentTimeMillis(),
    val sensorData: SensorData,
    val udpSignal: String? = null,
    val esp32Data: SerialData? = null,
    val id: String = UUID.randomUUID().toString()
)

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class SensorData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val bearing: Float,
    val accuracy: Float,
    val speed: Float = 0f
)

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class SerialData(
    val payload: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SerialData

        if (!payload.contentEquals(other.payload)) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = payload.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
