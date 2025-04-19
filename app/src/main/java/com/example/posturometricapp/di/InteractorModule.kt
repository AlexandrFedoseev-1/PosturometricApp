package com.example.posturometricapp.di

import com.example.posturometricapp.FakeUsbSensorDataSource

import com.example.posturometricapp.domain.api.SensorDataInteractor
import com.example.posturometricapp.domain.api.SessionInteractor
import com.example.posturometricapp.domain.impl.SensorDataInteractorImpl
import com.example.posturometricapp.domain.impl.SessionInteractorImpl
import org.koin.dsl.module

val interactorModule = module {
    factory<SensorDataInteractor> {
        SensorDataInteractorImpl(repository = get())
    }
    factory<SessionInteractor> {
        SessionInteractorImpl(repository = get())
    }

    //test things

    factory {
        FakeUsbSensorDataSource()
    }
}