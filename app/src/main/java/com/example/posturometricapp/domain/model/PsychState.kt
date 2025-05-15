package com.example.posturometricapp.domain.model


import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PsychState(
    val id: Long,
    val sessionId: Long,
    val stateName: String,
    val startTime: Long,
    val endTime: Long?
) : Parcelable
