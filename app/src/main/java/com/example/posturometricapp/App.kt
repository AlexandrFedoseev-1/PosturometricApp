package com.example.posturometricapp

import android.app.Application
import com.example.posturometricapp.di.dataModule
import com.example.posturometricapp.di.interactorModule
import com.example.posturometricapp.di.repositoriesModule
import com.example.posturometricapp.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(dataModule, repositoriesModule, interactorModule, viewModelModule)
        }
    }
}