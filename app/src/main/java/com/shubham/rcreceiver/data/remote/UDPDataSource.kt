package com.shubham.rcreceiver.data.remote

import android.content.Context
import android.util.Log
import com.shubham.rcreceiver.domain.models.TelemetryData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject

class UDPDataSource @Inject constructor(
    private val context: Context
) {
    private var udpSocket: DatagramSocket? = null
    private var receiverJob: Job? = null

    private val _signalFlow = MutableStateFlow("")
    val signalFlow: StateFlow<String> = _signalFlow

    fun startListening(port: Int = 5000) {
        receiverJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                udpSocket = DatagramSocket(port)
                val buffer = ByteArray(1024)
                
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    udpSocket?.receive(packet)
                    val message = String(packet.data, 0, packet.length)
                    _signalFlow.emit(message)
                }
            } catch (e: Exception) {
                Log.e("UDP", "Error listening: ${e.message}")
            }
        }
    }
    
    fun stopListening() {
        receiverJob?.cancel()
        udpSocket?.close()
        udpSocket = null
    }
    
    suspend fun sendTelemetry(data: TelemetryData, host: String, port: Int = 5001) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = Json.encodeToString(TelemetryData.serializer(), data)
                val socket = DatagramSocket()
                val address = InetAddress.getByName(host)
                val packet = DatagramPacket(
                    json.toByteArray(),
                    json.length,
                    address,
                    port
                )
                socket.send(packet)
                socket.close()
                Log.d("UDP", "Telemetry sent to $host:$port")
            } catch (e: Exception) {
                Log.e("UDP", "Error sending telemetry: ${e.message}")
            }
        }
    }
}
