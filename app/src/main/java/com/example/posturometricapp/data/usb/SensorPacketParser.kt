package com.example.posturometricapp.data.usb

import com.example.posturometricapp.data.usb.SensorDataUsb

/**
 * Парсер пакетов данных: извлечение полного сообщения и преобразование в модель.
 */
class SensorPacketParser {
    private val buffer = StringBuilder()

    /**
     * Добавляет новый фрагмент в буфер и возвращает полный пакет, если найден.
     */
    fun feed(data: String): String? {
        buffer.append(data)
        val end = buffer.indexOf("#")
        return if (end >= 0) {
            val packet = buffer.substring(0, end + 1)
            buffer.delete(0, end + 1)
            packet
        } else null
    }

    /**
     * Парсит одно полное сообщение, возвращая SensorDataUsb или null при ошибке.
     */
    fun parse(packet: String): SensorDataUsb? {
        val trimmed = packet.trim()
        if (!trimmed.startsWith("S") || !trimmed.endsWith("#")) return null
        val content = trimmed.substring(1, trimmed.length - 1)
        val parts = content.split("$")
        if (parts.size != 33) return null
        return try {
            val values = parts.subList(0, 32).map { it.toLong() }
            val temp = parts[32].toDouble()
            SensorDataUsb(values, temp)
        } catch (e: Exception) {
            null
        }
    }
}
