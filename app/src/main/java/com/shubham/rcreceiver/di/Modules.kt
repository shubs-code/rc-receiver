package com.shubham.rcreceiver.di

import android.content.Context
import android.hardware.SensorManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.shubham.rcreceiver.data.local.SerialPortDataSource
import com.shubham.rcreceiver.data.remote.UDPDataSource
import com.shubham.rcreceiver.data.repositories.SensorRepositoryImpl
import com.shubham.rcreceiver.data.repositories.SerialRepositoryImpl
import com.shubham.rcreceiver.data.repositories.TelemetryRepositoryImpl
import com.shubham.rcreceiver.data.sensor.SensorDataCollector
import com.shubham.rcreceiver.domain.repositories.ISerialRepository
import com.shubham.rcreceiver.domain.repositories.ISensorRepository
import com.shubham.rcreceiver.domain.repositories.ITelemetryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
//    @Singleton
//    @Provides
//    fun provideUDPDataSource(
//        @ApplicationContext context: Context
//    ): UDPDataSource = UDPDataSource(context)
    
    @Singleton
    @Provides
    fun provideTelemetryRepository(
        udpDataSource: UDPDataSource,
        sensorCollector: SensorDataCollector,
        serialDataSource: SerialPortDataSource
    ): ITelemetryRepository = TelemetryRepositoryImpl(
        udpDataSource, sensorCollector, serialDataSource
    )
}

@Module
@InstallIn(SingletonComponent::class)
object SensorModule {
    
    @Singleton
    @Provides
    fun provideSensorManager(
        @ApplicationContext context: Context
    ): SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    @Singleton
    @Provides
    fun provideFusedLocationClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    
    @Singleton
    @Provides
    fun provideSensorDataCollector(
        @ApplicationContext context: Context,
        fusedLocationProvider: FusedLocationProviderClient,
        sensorManager: SensorManager
    ): SensorDataCollector = SensorDataCollector(context, fusedLocationProvider, sensorManager)
    
    @Singleton
    @Provides
    fun provideSensorRepository(
        sensorCollector: SensorDataCollector
    ): ISensorRepository = SensorRepositoryImpl(sensorCollector)
}

@Module
@InstallIn(SingletonComponent::class)
object SerialModule {
    
    @Singleton
    @Provides
    fun provideSerialPortDataSource(
        @ApplicationContext context: Context
    ): SerialPortDataSource = SerialPortDataSource(context)
    
    @Singleton
    @Provides
    fun provideSerialRepository(
        serialDataSource: SerialPortDataSource
    ): ISerialRepository = SerialRepositoryImpl(serialDataSource)
}
