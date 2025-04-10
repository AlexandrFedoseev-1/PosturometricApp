package com.example.posturometricapp.data.usb


import android.content.Context
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
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

    // Поле для хранения калибровочных значений (offset) для 32 сенсоров.
    // Если калибровка не проведена, оно равно null.
    private var calibrationOffsets: List<Long>? = null

    // Количество сообщений для калибровки
    private val calibrationMessageCount = 5
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

        // Внутри класса UsbSensorDataSource объявляем буфер для накопления данных.
        val readBuffer = StringBuilder()

        // Реализация ioManager:
        ioManager =
            SerialInputOutputManager(serialPort, object : SerialInputOutputManager.Listener {
                override fun onNewData(data: ByteArray?) {
                    data?.let {
                        // Преобразуем полученный массив байтов в строку
                        val dataStr = String(it)

                        // Добавляем полученные данные в накопительный буфер
                        readBuffer.append(dataStr)

                        // Проверяем, содержится ли в буфере символ завершения '#'
                        var endIndex = readBuffer.indexOf("#")
                        // Пока находим символ завершения пакета, обрабатываем полное сообщение
                        while (endIndex != -1) {

                            // Извлекаем пакет: с первой позиции до символа '#' включительно
                            val fullMessageRaw = readBuffer.substring(0, endIndex + 1)
                            val fullMessage = fullMessageRaw.trim()

                            Log.d("fullMessage", "Received chunk: $fullMessage")
                            // Обрабатываем сообщение, если оно соответствует ожидаемому формату
                            if (fullMessage.startsWith("S") && fullMessage.endsWith("#")) {
                                val content = fullMessage.substring(1, fullMessage.length - 1)
                                val parts = content.split("$")
//                                parts.forEachIndexed { index, token ->
//                                    Log.d("SEND", "Token $index: '$token' (length: ${token.length})")
//                                }
                                if (parts.size == 33) {
                                    try {
                                        var sensorValues = parts.subList(0, 32).map { it.toLong() }
                                        val temperature = parts[32].toDouble()
                                        // Если проведена калибровка, вычитаем offset для каждого сенсора
                                        calibrationOffsets?.let { offsets ->
                                            sensorValues = sensorValues.mapIndexed { index, value ->
                                                value - offsets[index]
                                            }
                                        }
                                        val sensorDataUsb = SensorDataUsb(sensorValues, temperature)
                                        Log.d("sensorValues", "Full message processed: $sensorValues")
                                        trySend(sensorDataUsb).isSuccess
                                        Log.d("SEND", "Full message processed: $sensorDataUsb")
                                    } catch (e: Exception) {
                                        Log.d("SEND", "Error parsing message: $fullMessage", e)
                                    }
                                }
                            } else {
                                Log.d(
                                    "SEND",
                                    "Received incomplete or invalid message: $fullMessage"
                                )
                            }
                            // Удаляем обработанный фрагмент из буфера
                            readBuffer.delete(0, endIndex + 1)
                            // Проверяем, есть ли еще полный пакет в буфере
                            endIndex = readBuffer.indexOf("#")
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
     * Запускает калибровку сенсоров.
     * Собирает calibrationMessageCount полных сообщений и вычисляет среднее значение для каждого сенсора.
     * Сохранённые offset'ы затем используются для коррекции данных.
     */
    suspend fun calibrateSensors(): Boolean = withContext(Dispatchers.IO) {
        val collectedValues = mutableListOf<List<Long>>()
        // Запускаем временный Flow для калибровки
        val calibrationFlow = startListening()
        val job = launch {
            calibrationFlow.collect { sensorDataUsb ->
                // Добавляем массив значений в список
                collectedValues.add(sensorDataUsb.sensorValues)
                if (collectedValues.size >= calibrationMessageCount) {
                    cancel()  // Останавливаем сбор, как только накопили нужное количество сообщений.
                }
            }
        }
        job.join() // Ждём завершения сбора
        return@withContext if (collectedValues.size == calibrationMessageCount) {
            // Для каждого сенсора вычисляем среднее по собранным значениям.
            val numSensors = collectedValues[0].size // должен быть 32
            val offsets = List(numSensors) { index ->
                collectedValues.map { it[index] }.average().toLong()
            }
            calibrationOffsets = offsets
            Log.d("CALIBRATION", "Calibration offsets computed: $offsets")
            true
        } else {
            Log.e("CALIBRATION", "Failed to collect calibration data")
            false
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
