package com.example.posturometricapp.di

import androidx.room.Room
import com.example.posturometricapp.data.dp.AppDatabase
import com.example.posturometricapp.data.usb.UsbSensorDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single {
        Room.databaseBuilder(androidContext(),AppDatabase::class.java,"database.db").fallbackToDestructiveMigration().build()
    }
    single {
        UsbSensorDataSource(androidContext())
    }
}