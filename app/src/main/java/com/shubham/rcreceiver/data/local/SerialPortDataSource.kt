package com.shubham.rcreceiver.data.local

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class SerialPortDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "SerialPort"
        private const val ACTION_USB_PERMISSION = "com.shubham.rcreceiver.USB_PERMISSION"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serialPort: UsbSerialPort? = null
    private var readJob: kotlinx.coroutines.Job? = null
    private var usbPermissionReceiver: BroadcastReceiver? = null

    // For permission waiting
    private var pendingPermissionDevice: UsbDevice? = null
    private var isWaitingForPermission = false

    private val _serialDataFlow = MutableStateFlow(ByteArray(0))
    val serialDataFlow: StateFlow<ByteArray> = _serialDataFlow.asStateFlow()

    private val _connectionStatusFlow = MutableStateFlow(false)
    val connectionStatusFlow: StateFlow<Boolean> = _connectionStatusFlow.asStateFlow()

    private val _debugMessage = MutableStateFlow("Ready")
    val debugMessage: StateFlow<String> = _debugMessage.asStateFlow()

    init {
        registerUsbPermissionReceiver()
    }

    /**
     * Register broadcast receiver for USB permission requests
     */
    private fun registerUsbPermissionReceiver() {
        val intentFilter = IntentFilter(ACTION_USB_PERMISSION)

        usbPermissionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (context == null || intent == null) return

                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                val permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)

                Log.d(TAG, "Permission broadcast received for device: ${device?.deviceName}")
                Log.d(TAG, "Permission granted: $permissionGranted")

                if (device != null && permissionGranted) {
                    Log.d(TAG, "✅ USB PERMISSION GRANTED - Proceeding with connection")
                    _debugMessage.value = "✅ Permission granted, connecting..."
                    scope.launch {
                        connectToDevice(device)
                    }
                } else if (device != null) {
                    Log.e(TAG, "❌ USB PERMISSION DENIED")
                    _debugMessage.value = "❌ USB permission denied by user"
                    _connectionStatusFlow.value = false
                } else {
                    Log.e(TAG, "Device is null in permission broadcast")
                    _debugMessage.value = "❌ Device is null"
                }

                isWaitingForPermission = false
            }
        }

        try {
            context.registerReceiver(usbPermissionReceiver, intentFilter, Context.RECEIVER_EXPORTED)
            Log.d(TAG, "✅ USB permission receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register USB receiver", e)
            _debugMessage.value = "❌ Failed to register receiver"
        }
    }

    /**
     * Main connection method
     */
    fun connectToESP32(baudRate: Int = 115200) {
        scope.launch {
            try {
                _debugMessage.value = "⏳ Starting connection..."
                Log.d(TAG, "=== Starting ESP32 Connection ===")

                disconnect()

                val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                Log.d(TAG, "UsbManager acquired")

                // Step 1: List all USB devices
                val allDevices = usbManager.deviceList
                Log.d(TAG, "📱 Total USB devices connected: ${allDevices.size}")

                if (allDevices.isEmpty()) {
                    _debugMessage.value = "❌ No USB devices found\nPlug in your USB device"
                    Log.e(TAG, "No USB devices connected")
                    _connectionStatusFlow.value = false
                    return@launch
                }

                // List all devices for debugging
                allDevices.forEach { (name, device) ->
                    Log.d(TAG, "Device: $name")
                    Log.d(TAG, "  - DeviceName: ${device.deviceName}")
                    Log.d(TAG, "  - VendorID: ${String.format("0x%04x", device.vendorId)}")
                    Log.d(TAG, "  - ProductID: ${String.format("0x%04x", device.productId)}")
                }

                // Step 2: Find USB serial drivers
                val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
                Log.d(TAG, "🔌 Found ${drivers.size} compatible USB serial drivers")

                if (drivers.isEmpty()) {
                    _debugMessage.value = "❌ No compatible USB drivers found\nDevice may need CH340/CP2102 drivers"
                    Log.e(TAG, "No compatible USB serial drivers found")
                    _connectionStatusFlow.value = false
                    return@launch
                }

                // Step 3: Get first driver and device
                val driver = drivers.first()
                val device = driver.device

                Log.d(TAG, "✓ Selected device: ${device.deviceName}")
                Log.d(TAG, "  - VendorID: ${String.format("0x%04x", device.vendorId)}")
                Log.d(TAG, "  - ProductID: ${String.format("0x%04x", device.productId)}")
                _debugMessage.value = "ℹ️ Found device: ${device.deviceName}"

                // Step 4: Check if we already have permission
                val hasPermission = usbManager.hasPermission(device)
                Log.d(TAG, "Permission check: hasPermission = $hasPermission")

                if (!hasPermission) {
                    Log.d(TAG, "⏳ Permission required - requesting...")
                    _debugMessage.value = "⏳ Requesting USB permission...\n(Check phone for dialog)"

                    // Request permission
                    requestUsbPermission(device)

                    // The receiver will handle the rest when user responds
                    Log.d(TAG, "Permission request sent to Android")
                    return@launch
                }

                // We already have permission, proceed directly
                Log.d(TAG, "✓ Permission already granted, proceeding...")
                connectToDevice(device)

            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException: ${e.message}", e)
                _debugMessage.value = "❌ Security error: ${e.message}"
                _connectionStatusFlow.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Connection error: ${e.message}", e)
                _debugMessage.value = "❌ Error: ${e.message}"
                _connectionStatusFlow.value = false
            }
        }
    }

    /**
     * Request USB permission from user
     */
    private fun requestUsbPermission(device: UsbDevice) {
        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

            val intent = Intent(ACTION_USB_PERMISSION)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            pendingPermissionDevice = device
            isWaitingForPermission = true

            Log.d(TAG, "Requesting USB permission for: ${device.deviceName}")
            usbManager.requestPermission(device, pendingIntent)
            Log.d(TAG, "Permission request sent")

        } catch (e: Exception) {
            Log.e(TAG, "Error requesting permission: ${e.message}", e)
            _debugMessage.value = "❌ Failed to request permission"
        }
    }

    /**
     * Actual connection logic after permission is granted
     */
    private suspend fun connectToDevice(device: UsbDevice) {
        try {
            Log.d(TAG, "🔗 Connecting to device: ${device.deviceName}")
            _debugMessage.value = "🔗 Opening USB connection..."

            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

            // Open connection
            val connection = usbManager.openDevice(device)
            if (connection == null) {
                Log.e(TAG, "❌ Failed to open USB connection")
                _debugMessage.value = "❌ Failed to open USB connection"
                _connectionStatusFlow.value = false
                return
            }

            Log.d(TAG, "✓ USB connection opened")
            _debugMessage.value = "✓ USB connection opened"

            // Get serial port
            val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
            if (drivers.isEmpty()) {
                Log.e(TAG, "No drivers found after connection")
                connection.close()
                _debugMessage.value = "❌ No drivers found"
                return
            }

            val driver = drivers.first()
            if (driver.ports.isEmpty()) {
                Log.e(TAG, "No ports available on driver")
                connection.close()
                _debugMessage.value = "❌ No serial ports available"
                return
            }

            val port = driver.ports.first()

            // Configure serial port
            Log.d(TAG, "⚙️ Configuring serial port...")
            port.open(connection)
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

            serialPort = port
            _connectionStatusFlow.value = true
            _debugMessage.value = "✅ Connected at 115200 baud"

            Log.d(TAG, "✅✅✅ SUCCESSFULLY CONNECTED TO ESP32 ✅✅✅")
            startReading()

        } catch (e: SecurityException) {
            Log.e(TAG, "❌ SecurityException during connection: ${e.message}", e)
            _debugMessage.value = "❌ Permission denied: ${e.message}"
            _connectionStatusFlow.value = false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Connection error: ${e.message}", e)
            _debugMessage.value = "❌ Connection error: ${e.message}"
            _connectionStatusFlow.value = false
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
                        Log.d(TAG, "📥 Received ${data.size} bytes")
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Read error: ${e.message}", e)
                    _connectionStatusFlow.value = false
                    _debugMessage.value = "❌ Read error: ${e.message}"
                    break
                }
            }
        }
    }

    fun writeData(data: ByteArray) {
        scope.launch {
            try {
                serialPort?.write(data, 1000)
                Log.d(TAG, "📤 Sent ${data.size} bytes")
            } catch (e: Exception) {
                Log.e(TAG, "Write error: ${e.message}", e)
                _debugMessage.value = "❌ Write error: ${e.message}"
            }
        }
    }

    fun disconnect() {
        try {
            readJob?.cancel()
            readJob = null

            serialPort?.close()
            serialPort = null

            Log.d(TAG, "Disconnected")
            _debugMessage.value = "⏸️ Disconnected"

        } catch (e: Exception) {
            Log.e(TAG, "Disconnect error: ${e.message}", e)
        }

        _connectionStatusFlow.value = false
    }

    fun release() {
        disconnect()

        try {
            if (usbPermissionReceiver != null) {
                context.unregisterReceiver(usbPermissionReceiver)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}", e)
        }

        scope.cancel()
    }
}