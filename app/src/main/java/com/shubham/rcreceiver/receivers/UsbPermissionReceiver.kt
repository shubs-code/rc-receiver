package com.shubham.rcreceiver.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint

/**
 * Handles USB permission requests
 * Called when user approves/denies USB device access
 */
@AndroidEntryPoint
class UsbPermissionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "UsbPermission"
        const val ACTION_USB_PERMISSION = "com.shubham.rcreceiver.USB_PERMISSION"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
        val permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)

        if (device != null) {
            if (permissionGranted) {
                Log.d(TAG, "✅ Permission GRANTED for device: ${device.deviceName}")
                Log.d(TAG, "Device Details:")
                Log.d(TAG, "  - VendorID: ${String.format("0x%04x", device.vendorId)}")
                Log.d(TAG, "  - ProductID: ${String.format("0x%04x", device.productId)}")
                Log.d(TAG, "  - ManufacturerName: ${device.manufacturerName}")
                Log.d(TAG, "  - ProductName: ${device.productName}")
                Log.d(TAG, "  - SerialNumber: ${device.serialNumber}")
                Log.d(TAG, "Ready to connect!")
            } else {
                Log.e(TAG, "❌ Permission DENIED for device: ${device.deviceName}")
                Log.e(TAG, "User must grant USB permission to use this device")
            }
        } else {
            Log.e(TAG, "❌ Device is null in permission broadcast")
        }
    }
}
