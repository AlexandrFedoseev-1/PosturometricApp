package com.example.posturometricapp.di

import com.example.posturometricapp.ui.SensorViewModel
import com.example.posturometricapp.ui.session.SessionListViewModel
import com.example.posturometricapp.ui.sessionDetails.SessionDetailsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {

    viewModel {
        SessionListViewModel(interactor = get())
    }
    viewModel {
        SessionDetailsViewModel(interactor = get(), psychStateInteractor = get())
    }
    viewModel {
        SensorViewModel(
            sensorDataInteractor = get(),
            psychStateInteractor = get(),
            get()
        )
    }
}