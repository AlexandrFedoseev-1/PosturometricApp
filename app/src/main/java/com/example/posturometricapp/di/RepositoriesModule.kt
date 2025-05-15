package com.example.posturometricapp.di

import com.example.posturometricapp.data.repository.PsychStateRepositoryImpl
import com.example.posturometricapp.data.repository.SensorDataRepositoryImpl
import com.example.posturometricapp.data.repository.SessionRepositoryImpl
import com.example.posturometricapp.domain.api.PsychStateRepository
import com.example.posturometricapp.domain.api.SensorDataRepository
import com.example.posturometricapp.domain.api.SessionRepository
import org.koin.dsl.module

val repositoriesModule = module {
    single<SensorDataRepository>{
        SensorDataRepositoryImpl(usbSensorDataSource = get(), appDatabase = get())
    }
    single<SessionRepository> {
        SessionRepositoryImpl(appDatabase = get())
    }
    single<PsychStateRepository> {
        PsychStateRepositoryImpl(appDatabase = get())
    }
}