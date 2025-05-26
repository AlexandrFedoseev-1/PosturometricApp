package com.example.posturometricapp.data.dp.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.posturometricapp.data.dp.converter.ListLongConverter


@Entity(
    tableName = "sensor_data",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class SensorDataEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sessionId: Long,
    val timestamp: Long,
    val sensorValues: List<Long>,
    val temperature: Double
)