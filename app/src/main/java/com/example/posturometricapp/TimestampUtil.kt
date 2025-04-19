package com.example.posturometricapp

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatTimestampDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}
fun formatTimestampTime(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(millis))
}
