app/src/main/
├── java/com/yourcompany/telemetry/
│   ├── di/                          # Dependency Injection (Hilt)
│   │   ├── NetworkModule.kt
│   │   ├── SensorModule.kt
│   │   └── RepositoryModule.kt
│   │
│   ├── domain/
│   │   ├── models/
│   │   │   ├── TelemetryData.kt
│   │   │   ├── SensorData.kt
│   │   │   └── SerialData.kt
│   │   └── repositories/
│   │       ├── ITelemetryRepository.kt
│   │       ├── ISensorRepository.kt
│   │       └── ISerialRepository.kt
│   │
│   ├── data/
│   │   ├── local/
│   │   │   └── SerialPortDataSource.kt
│   │   ├── remote/
│   │   │   ├── UDPDataSource.kt
│   │   │   └── TelemetrySender.kt
│   │   ├── repositories/
│   │   │   ├── TelemetryRepositoryImpl.kt
│   │   │   ├── SensorRepositoryImpl.kt
│   │   │   └── SerialRepositoryImpl.kt
│   │   └── sensor/
│   │       ├── SensorDataCollector.kt
│   │       ├── GPSProvider.kt
│   │       ├── CompassProvider.kt
│   │       └── AltitudeProvider.kt
│   │
│   ├── presentation/
│   │   ├── screens/
│   │   │   ├── MainScreen.kt
│   │   │   ├── TelemetryScreen.kt
│   │   │   ├── SensorScreen.kt
│   │   │   └── SerialDebugScreen.kt
│   │   ├── viewmodels/
│   │   │   ├── TelemetryViewModel.kt
│   │   │   ├── SensorViewModel.kt
│   │   │   └── SerialViewModel.kt
│   │   ├── components/
│   │   │   ├── TelemetryCard.kt
│   │   │   ├── SensorReadings.kt
│   │   │   └── StatusIndicator.kt
│   │   └── MainActivity.kt
│   │
│   ├── utils/
│   │   ├── Constants.kt
│   │   └── Extensions.kt
│   │
│   └── TelemetryApp.kt              # Hilt Application

└── AndroidManifest.xml
