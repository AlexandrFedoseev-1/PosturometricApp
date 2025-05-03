package com.example.posturometricapp.data.usb

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Менеджер подключения к USB-устройствам: поиск, подключение и закрытие порта.
 */
class UsbConnectionManager(private val context: Context) {
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val _permissionEvents = MutableSharedFlow<PermissionEvent>()
    val permissionEvents: Flow<PermissionEvent> = _permissionEvents.asSharedFlow()

    sealed class PermissionEvent {
        object NoDevice : PermissionEvent()
        data class Granted(val driver: UsbSerialDriver, val port: UsbSerialPort) : PermissionEvent()
        data class Denied(val device: UsbDevice?) : PermissionEvent()
    }

    /**
     * Ищет первый доступный драйвер и пытается открыть порт.
     * Для упрощения подразумевается, что разрешение на доступ уже получено.
     */
    fun openFirstPort(): UsbSerialPort? {
        val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (drivers.isEmpty()) {
            _permissionEvents.tryEmit(PermissionEvent.NoDevice)
            return null
        }
        val driver = drivers[0]
        val connection = usbManager.openDevice(driver.device) ?: run {
            _permissionEvents.tryEmit(PermissionEvent.Denied(driver.device))
            return null
        }
        val port = driver.ports[0]
        port.open(connection)
        port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
        _permissionEvents.tryEmit(PermissionEvent.Granted(driver, port))
        return port
    }

    /**
     * Закрывает порт и сбрасывает состояние.
     */
    fun closePort(port: UsbSerialPort) {
        try {
            port.close()
        } catch (_: Exception) { /* ignore */ }
    }
}
