package com.shubham.rcreceiver.data.remote

import android.content.Context
import android.util.Log
import com.shubham.rcreceiver.data.local.SerialPortDataSource
import com.shubham.rcreceiver.domain.models.TelemetryData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketException
import javax.inject.Inject
import java.net.NetworkInterface
import javax.inject.Singleton

/**
 * UDP Data Source
 * - Listens on all IPv4 and IPv6 addresses
 * - Processes commands from UDP packets
 * - Routes commands to serial port
 */
@Singleton
class UDPDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serialPortDataSource: SerialPortDataSource
) {
    companion object {
        private const val TAG = "UDP"
        private const val DEFAULT_PORT_IPV4 = 5000
        private const val DEFAULT_PORT_IPV6 = 5000
        private const val BUFFER_SIZE = 4096
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // UDP Sockets - IPv4 and IPv6
    private var ipv4Socket: DatagramSocket? = null
    private var ipv6Socket: DatagramSocket? = null
    private var ipv4ReceiverJob: Job? = null
    private var ipv6ReceiverJob: Job? = null

    // Flow for receiving UDP messages
    private val _signalFlow = MutableStateFlow("")
    val signalFlow: StateFlow<String> = _signalFlow.asStateFlow()

    // Flow for processed commands to send to serial

    // Connection status
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    // Debug messages
    private val _debugMessage = MutableStateFlow("UDP: Not listening")
    val debugMessage: StateFlow<String> = _debugMessage.asStateFlow()

    /**
     * Start listening on both IPv4 and IPv6
     */
    fun startListening(portIPv4: Int = DEFAULT_PORT_IPV4, portIPv6: Int = DEFAULT_PORT_IPV6) {
        Log.d(TAG, "🔊 Starting UDP listener on ports $portIPv4 (IPv4) and $portIPv6 (IPv6)")

        // Start IPv6 listener
        startIPv6Listener(portIPv6)

        _isListening.value = true
    }

    /**
     * Start IPv6 listener
     * Listens on [::]:port (all IPv6 addresses)
     */
    private fun startIPv6Listener(port: Int) {
        ipv6ReceiverJob?.cancel()

        ipv6ReceiverJob = scope.launch {
            try {
                // Create socket with IPv6 support
                val socket = DatagramSocket(null) as DatagramSocket
                socket.reuseAddress = true

                // Bind to all IPv6 addresses
                socket.bind(InetSocketAddress("::", port))
                ipv6Socket = socket

                Log.d(TAG, "✅ IPv6 listener started on [::]:$port")
                _debugMessage.value = "✅ IPv6 listening on [::]:$port"

                val buffer = ByteArray(BUFFER_SIZE)

                while (isActive && ipv6Socket != null) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        ipv6Socket?.receive(packet)

                        val message = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        val senderIP = packet.address.hostAddress
                        val senderPort = packet.port

                        Log.d(TAG, "IPv6 Message from [$senderIP]:$senderPort")
                        Log.d(TAG, "   Data: $message")

                        // Emit raw signal
                        if(isValidJson(message)){
                            _signalFlow.emit(message)
                            // Process command
                            processCommand(message, senderIP, senderPort)   // do something later on
                        }

                    } catch (e: SocketException) {
                        if (isActive) {
                            Log.e(TAG, "❌ IPv6 Socket error: ${e.message}")
                        }
                        break
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ IPv6 Error: ${e.message}")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to start IPv6 listener: ${e.message}")
                _debugMessage.value = "❌ IPv6 Error: ${e.message}"
            }
        }
    }
    private fun isValidJson(text: String): Boolean {
        return try {
            org.json.JSONObject(text)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Process incoming command
     * Parses message and extracts command data
     */
//    {
//        "type":"ctrl/cmd",
//        "value":[] if ctrl or "cmd type"
//    }
    private suspend fun processCommand(message: String, senderIP: String, senderPort: Int) {
        try {
            Log.d(TAG, "🔍 Processing command from $senderIP:$senderPort")

            // Try to parse as JSON command format
            if (message.startsWith("{") && message.endsWith("}")) {
                val jsonData = Json.parseToJsonElement(message)
                val obj = jsonData.jsonObject

                val cmdType = obj["type"]?.jsonPrimitive?.content ?: "UNKNOWN"
                if(cmdType.equals("ctrl")){
                    val data = obj["value"]?.jsonArray
                        ?.joinToString(",") { it.jsonPrimitive.int.toString() }
                        ?: message

                    serialPortDataSource.writeData((data + "\n").toByteArray())
                }
                Log.d(TAG, "command: ${cmdType}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing command: ${e.message}")
        }
    }


    /**
     * Stop listening
     */
    fun stopListening() {
        Log.d(TAG, "🛑 Stopping UDP listener...")

        ipv4ReceiverJob?.cancel()
        ipv6ReceiverJob?.cancel()

        try {
            ipv4Socket?.close()
            ipv6Socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing sockets: ${e.message}")
        }

        ipv4Socket = null
        ipv6Socket = null

        _isListening.value = false
        _debugMessage.value = "⏸️ UDP listener stopped"
        Log.d(TAG, "✅ UDP listener stopped")
    }

    /**
     * Send telemetry data via UDP
     */
    suspend fun sendTelemetry(data: TelemetryData, host: String, port: Int = 5001) {
        scope.launch {
            try {
                val json = Json.encodeToString(TelemetryData.serializer(), data)
                val socket = DatagramSocket()
                val address = InetAddress.getByName(host)
                val packet = DatagramPacket(
                    json.toByteArray(Charsets.UTF_8),
                    json.length,
                    address,
                    port
                )
                socket.send(packet)
                socket.close()
                Log.d(TAG, "📤 Telemetry sent to $host:$port")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending telemetry: ${e.message}")
            }
        }
    }

    /**
     * Release resources
     */
    fun release() {
        stopListening()
        scope.cancel()
    }
}
