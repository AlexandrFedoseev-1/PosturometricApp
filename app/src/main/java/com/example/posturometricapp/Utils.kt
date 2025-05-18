package com.example.posturometricapp

import kotlin.math.roundToInt

fun convertToGrams(printed: Float): Float {
    // 1) Восстанавливаем «сырые» коды АЦП:
    val rawCounts = printed * 0.035274f * 3.0f

    // 2) Параметры датчика:
    val capacityGrams = 10_000.0f          // 10 kg
    val codesPerFullScale = (1.0f * 5.0f / 20.0f) * 8_388_608.0f
    //                         └─ rated mV/V × Vexc/V ─┘ └─ full-scale код range ─┘

    // 3) Граммов на один код:
    val gramsPerCount = capacityGrams / codesPerFullScale

    // 4) Итог:
    return (rawCounts * gramsPerCount* 100).roundToInt() / 100f
//    return printed
}
fun timeToString(time:Long): String{

    val seconds = time / 1000
    val ms = (time % 1000) / 100
    // Например: "12.3s"
    return "${seconds}.${ms}s"
}