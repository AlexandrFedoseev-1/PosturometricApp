package com.example.posturometricapp.di

import androidx.room.Room
import com.example.posturometricapp.data.dp.AppDatabase
import com.example.posturometricapp.data.dp.MIGRATION_1_2
import com.example.posturometricapp.data.usb.UsbSensorDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "database.db")
            .addMigrations(MIGRATION_1_2).build()
    }
    single {
        UsbSensorDataSource(androidContext())
    }
}