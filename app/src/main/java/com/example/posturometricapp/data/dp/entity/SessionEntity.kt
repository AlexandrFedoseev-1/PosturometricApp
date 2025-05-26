package com.example.posturometricapp.data.dp.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сущность, описывающая сеанс записи данных.
 * При старте сеанса записывается startTime, endTime обновляется при завершении.
 */

@Entity(tableName = "session")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val startTime: Long,
    val endTime: Long = 0L
)