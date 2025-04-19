package com.example.posturometricapp.domain.model

data class Session(
    val id: Long,
    val startTime: Long,
    val endTime: Long?
)