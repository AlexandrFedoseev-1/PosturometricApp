package com.example.posturometricapp.data.dp

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.posturometricapp.data.dp.converter.ListLongConverter
import com.example.posturometricapp.data.dp.dao.SensorDataDao
import com.example.posturometricapp.data.dp.dao.SessionDao
import com.example.posturometricapp.data.dp.entity.SensorDataEntity
import com.example.posturometricapp.data.dp.entity.SessionEntity

@Database(entities = [SessionEntity::class, SensorDataEntity::class], version = 1 )
@TypeConverters(ListLongConverter::class)
abstract class AppDatabase: RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun sensorDao(): SensorDataDao
}