package com.shubham.rcreceiver.data.local

import android.content.Context
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SerialPortDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "Serial"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var serialPort: UsbSerialPort? = null

    private var readJob: kotlinx.coroutines.Job? = null

    private val _serialDataFlow =
        MutableStateFlow(ByteArray(0))

    val serialDataFlow: StateFlow<ByteArray> =
        _serialDataFlow.asStateFlow()

    private val _connectionStatusFlow = MutableStateFlow(false)
    val connectionStatusFlow: StateFlow<Boolean> = _connectionStatusFlow.asStateFlow()

    fun connectToESP32(baudRate: Int = 115200) {
        scope.launch {
            try {
                disconnect()

                val usbManager =
                    context.getSystemService(Context.USB_SERVICE) as UsbManager

                val availableDrivers =
                    UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)

                Log.d(TAG, "Found ${availableDrivers.size} USB devices")

                if (availableDrivers.isEmpty()) {
                    _connectionStatusFlow.value = false
                    Log.e(TAG, "No USB serial devices found")
                    return@launch
                }

                val driver = availableDrivers.first()

                val connection = usbManager.openDevice(driver.device)

                if (connection == null) {
                    _connectionStatusFlow.value = false
                    Log.e(TAG, "USB permission denied or open failed")
                    return@launch
                }

                val port = driver.ports.first()

                port.open(connection)
                port.setParameters(
                    baudRate,
                    8,
                    UsbSerialPort.STOPBITS_1,
                    UsbSerialPort.PARITY_NONE
                )

                serialPort = port
                _connectionStatusFlow.value = true

                Log.d(TAG, "Connected at $baudRate baud")

                startReading()

            } catch (e: Exception) {
                Log.e(TAG, "Connection error", e)
                _connectionStatusFlow.value = false
            }
        }
    }

    private fun startReading() {
        readJob?.cancel()

        readJob = scope.launch {
            val buffer = ByteArray(4096)

            while (isActive && serialPort != null) {
                try {
                    val count = serialPort?.read(buffer, 1000) ?: 0

                    if (count > 0) {
                        val data = buffer.copyOfRange(0, count)
                        _serialDataFlow.emit(data)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Read error", e)
                    _connectionStatusFlow.value = false
                    break
                }
            }
        }
    }

    fun writeData(data: ByteArray) {
        scope.launch {
            try {
                serialPort?.write(data, 1000)
                Log.d(TAG, "Sent ${data.size} bytes")
            } catch (e: Exception) {
                Log.e(TAG, "Write error", e)
            }
        }
    }

    fun disconnect() {
        try {
            readJob?.cancel()
            readJob = null

            serialPort?.close()
            serialPort = null

        } catch (e: Exception) {
            Log.e(TAG, "Disconnect error", e)
        }

        _connectionStatusFlow.value = false
    }

    fun release() {
        disconnect()
        scope.cancel()
    }
}