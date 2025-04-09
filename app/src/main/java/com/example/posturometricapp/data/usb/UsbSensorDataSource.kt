package com.example.posturometricapp.data.usb


import android.content.Context
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException



/**
 * Класс для работы с USB-датчиками.
 * Использует библиотеку usb-serial-for-android для подключения и чтения данных.
 *Реализована возможность отправки команд Arduino.
 */
class UsbSensorDataSource(private val context: Context) {

    private var serialPort: UsbSerialPort? = null
    private var ioManager: SerialInputOutputManager? = null

    /**
     * Запускает прослушивание USB-порта и возвращает поток данных типа SensorDataUsb.
     * Ожидается, что данные имеют следующий формат:
     * S<значение1>$<значение2>$...$<значение32>$<температура>#
     */
    fun startListening(): Flow<SensorDataUsb> = callbackFlow {
        // Получаем UsbManager из системного сервиса
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)

        if (availableDrivers.isEmpty()) {
            close(IOException("No USB devices found"))
            return@callbackFlow
        }

        // Для демонстрации используем первый найденный драйвер
        val driver: UsbSerialDriver = availableDrivers[0]
        val connection = usbManager.openDevice(driver.device)
        if (connection == null) {
            close(IOException("Could not open USB device"))
            return@callbackFlow
        }

        serialPort = driver.ports[0] // Предполагаем, что используем первый порт
        try {
            serialPort?.open(connection)
            serialPort?.setParameters(
                9600, // baud rate
                8,    // data bits
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE
            )
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }
//        try {
//            // Проверяем, что порт открыт
//            if (serialPort == null) {
//                throw IOException("Serial port is not opened")
//            }
//            // Отправляем команду с таймаутом 1000 мс
//            serialPort?.write("C#".toByteArray(), 1000)
//            Log.d("Command", "C#")
//            true
//        } catch (e: Exception) {
//            e.printStackTrace()
//            return@callbackFlow
//        }
        Log.d("SerialInputOutputManager", "ioManager")
        // Организуем IO Manager для асинхронного чтения данных
        ioManager = SerialInputOutputManager(serialPort, object : SerialInputOutputManager.Listener {

            override fun onNewData(data: ByteArray?) {

                data?.let {
                    val dataStr = String(it)
                    Log.d("SEND", dataStr)
                    // Ожидаемый формат: S<датчик1>$...$<датчик32>$<температура>#
                    if (dataStr.startsWith("S") && dataStr.endsWith("#")) {
                        val content = dataStr.substring(1, dataStr.length - 1)
                        val parts = content.split("$")
                        if (parts.size == 33) {
                            try {
                                val sensorValues = parts.subList(0, 32).map { it.toLong() }
                                val temperature = parts[32].toDouble()
                                val sensorDataUsb = SensorDataUsb(sensorValues, temperature)
                                trySend(sensorDataUsb).isSuccess
                                Log.d("SEND", sensorDataUsb.toString())
                            } catch (e: Exception) {
                                Log.d("SEND", "Notttt")
                            }
                        }
                    }
                }
            }

            override fun onRunError(e: Exception?) {
                close(e ?: IOException("Unknown IO error"))
            }
        })

        ioManager?.let { manager ->
            val job = launch(Dispatchers.IO) {
                Log.d("ioManager", "run")
                manager.run() // Блокирующий вызов, работающий до остановки
                Log.d("ioManager", "run2")
            }
            awaitClose {
                manager.stop()
                try {
                    serialPort?.close()
                } catch (e: Exception) {
                    // Игнорируем ошибки закрытия
                }
                job.cancel()
            }
        } ?: run {
            close(IOException("IO Manager initialization failed"))
        }
    }

    /**
     * Отправляет команду на Arduino.
     *
     * @param command Строка команды, например "CR#" для включения считывания.
     * @return true, если команда отправлена успешно, false в случае ошибки.
     */
    suspend fun sendCommand(command: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Проверяем, что порт открыт
            if (serialPort == null) {
                throw IOException("Serial port is not opened")
            }
            // Отправляем команду с таймаутом 1000 мс
            serialPort?.write(command.toByteArray(), 1000)
            Log.d("Command", command)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

}
