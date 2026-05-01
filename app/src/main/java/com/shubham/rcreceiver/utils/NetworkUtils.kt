package com.shubham.rcreceiver.utils

import android.util.Log
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface

object NetworkUtils {

    fun getPublicIPv6Addresses(): List<String> {
        val publicIPv6Addresses = mutableListOf<String>()

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()

            for (networkInterface in interfaces) {

                // Skip interfaces that are down / loopback / virtual
                if (!networkInterface.isUp ||
                    networkInterface.isLoopback ||
                    networkInterface.isVirtual
                ) continue

                val addresses = networkInterface.inetAddresses

                for (address in addresses) {

                    if (address is Inet6Address && !address.isLoopbackAddress) {

                        val ipv6 = address.hostAddress?.split("%")?.get(0) ?: continue

                        // Keep only PUBLIC IPv6 addresses
                        if (isPublicIPv6(address)) {
                            publicIPv6Addresses.add(ipv6)
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("NetworkUtils", "Error getting IPv6 addresses: ${e.message}")
        }

        return publicIPv6Addresses.distinct()
    }

    fun isPublicIPv6(address: Inet6Address): Boolean {

        return !address.isAnyLocalAddress &&          // ::
                !address.isLoopbackAddress &&          // ::1
                !address.isLinkLocalAddress &&         // fe80::/10
                !address.isSiteLocalAddress &&         // fec0::/10 (deprecated)
                !address.isMulticastAddress &&         // ff00::/8
                !address.isUniqueLocalAddress()        // fc00::/7
    }
    fun Inet6Address.isUniqueLocalAddress(): Boolean {
        val firstByte = this.address[0].toInt() and 0xFF
        return (firstByte and 0xFE) == 0xFC
    }
    fun getAllIPv4Addresses(): List<String> {
        val ipv4Addresses = mutableListOf<String>()

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()

            for (networkInterface in interfaces) {
                if (!networkInterface.isUp) continue

                val addresses = networkInterface.inetAddresses

                for (address in addresses) {
                    if (address is java.net.Inet4Address && !address.isLoopbackAddress) {
                        ipv4Addresses.add(address.hostAddress ?: continue)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkUtils", "Error getting IPv4 addresses: ${e.message}")
        }

        return ipv4Addresses.distinct()
    }
}
