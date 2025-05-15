package com.example.posturometricapp.di

import com.example.posturometricapp.SessionPlaybackSensorDataSource
import com.example.posturometricapp.domain.api.PsychStateInteractor

import com.example.posturometricapp.domain.api.SensorDataInteractor
import com.example.posturometricapp.domain.api.SessionInteractor
import com.example.posturometricapp.domain.impl.PsychStateInteractorImpl
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

    factory<PsychStateInteractor> {
        PsychStateInteractorImpl(repository = get())
    }

    //test things

    single {
        SessionPlaybackSensorDataSource(sessionInteractor = get())
    }
}