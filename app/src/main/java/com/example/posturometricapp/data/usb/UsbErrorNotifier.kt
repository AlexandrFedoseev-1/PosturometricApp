package com.example.posturometricapp.data.usb

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Нотификатор ошибок USB-слоя через Flow.
 */
object UsbErrorNotifier {
    private val _errors = MutableSharedFlow<UsbError>()
    val errors: SharedFlow<UsbError> = _errors

    suspend fun notify(error: UsbError) {
        _errors.emit(error)
    }
}

/**
 * Модель ошибки USB-операций.
 */
data class UsbError(val message: String, val throwable: Throwable? = null)
