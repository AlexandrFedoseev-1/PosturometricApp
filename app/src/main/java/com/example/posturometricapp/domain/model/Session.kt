package com.example.posturometricapp.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
@Parcelize
data class Session(
    val id: Long,
    val startTime: Long,
    val endTime: Long?
): Parcelable