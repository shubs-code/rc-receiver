package com.shubham.rcreceiver.utils

object Constants {
    // UDP Configuration
    const val UDP_SERVER_PORT = 5000
    const val UDP_RECEIVE_PORT = 5001
    const val UDP_RECEIVE_TIMEOUT = 1000
    
    // Serial Communication
    const val DEFAULT_BAUD_RATE = 115200
    const val SERIAL_READ_TIMEOUT = 1000
    
    // Sensor Configuration
    const val LOCATION_UPDATE_INTERVAL = 1000L
    const val LOCATION_FASTEST_INTERVAL = 500L
    const val SENSOR_DELAY_MS = 100L
    
    // Telemetry
    const val MAX_BUFFER_SIZE = 4096
    const val DEFAULT_TELEMETRY_HOST = "192.168.1.100"
    const val DEFAULT_TELEMETRY_PORT = 5001
}
