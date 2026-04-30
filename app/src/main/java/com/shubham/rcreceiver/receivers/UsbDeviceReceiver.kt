package com.shubham.rcreceiver.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Handles USB device attach/detach events
 * Called when user plugs in or unplugs a USB device
 */
@AndroidEntryPoint
class UsbDeviceReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "UsbDevice"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
        val action = intent.action

        when (action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                if (device != null) {
                    Log.d(TAG, "🔌 USB Device ATTACHED")
                    Toast.makeText(context, "USB Device Attached: ${device.productName}", Toast.LENGTH_SHORT).show()

                    logDeviceInfo(device)

                    // Notify observers that a device was attached
                    // You can broadcast to your app or trigger connection
                    notifyDeviceAttached(context, device)
                } else {
                    Log.e(TAG, "❌ Device is null in attach broadcast")
                }
            }

            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                if (device != null) {
                    Log.d(TAG, "🔌 USB Device DETACHED")
                    Toast.makeText(context, "USB Device Detached: ${device.productName}", Toast.LENGTH_SHORT).show()

                    // Notify observers that a device was detached
                    // Trigger disconnection cleanup
                    notifyDeviceDetached(context, device)
                } else {
                    Log.e(TAG, "❌ Device is null in detach broadcast")
                }
            }

            else -> {
                Log.w(TAG, "⚠️ Unknown USB action: $action")
            }
        }
    }

    /**
     * Log detailed device information
     */
    private fun logDeviceInfo(device: UsbDevice) {
        Log.d(TAG, "Device Name: ${device.deviceName}")
        Log.d(TAG, "Device ID: ${device.deviceId}")
        Log.d(TAG, "VendorID: ${String.format("0x%04x", device.vendorId)}")
        Log.d(TAG, "ProductID: ${String.format("0x%04x", device.productId)}")
        Log.d(TAG, "Device Class: ${device.deviceClass}")
        Log.d(TAG, "Device Subclass: ${device.deviceSubclass}")
        Log.d(TAG, "Device Protocol: ${device.deviceProtocol}")
        Log.d(TAG, "Manufacturer: ${device.manufacturerName}")
        Log.d(TAG, "Product: ${device.productName}")
        Log.d(TAG, "Serial: ${device.serialNumber}")
        Log.d(TAG, "Interface Count: ${device.interfaceCount}")
    }

    /**
     * Notify app that device was attached
     * You can broadcast this or use local listeners
     */
    private fun notifyDeviceAttached(context: Context, device: UsbDevice) {
        // Option 1: Broadcast to your app
        val broadcastIntent = Intent("com.shubham.rcreceiver.USB_DEVICE_ATTACHED").apply {
            putExtra(UsbManager.EXTRA_DEVICE, device)
        }
        context.sendBroadcast(broadcastIntent)

        // Option 2: If you have a Service running, start it
        // val serviceIntent = Intent(context, SerialService::class.java)
        // context.startService(serviceIntent)

        Log.d(TAG, "✅ Device attached notification sent")
    }

    /**
     * Notify app that device was detached
     */
    private fun notifyDeviceDetached(context: Context, device: UsbDevice) {
        // Option 1: Broadcast to your app
        val broadcastIntent = Intent("com.shubham.rcreceiver.USB_DEVICE_DETACHED").apply {
            putExtra(UsbManager.EXTRA_DEVICE, device)
        }
        context.sendBroadcast(broadcastIntent)

        // Option 2: If you have a Service running, tell it to disconnect
        // val serviceIntent = Intent(context, SerialService::class.java)
        // context.stopService(serviceIntent)

        Log.d(TAG, "✅ Device detached notification sent")
    }
}
