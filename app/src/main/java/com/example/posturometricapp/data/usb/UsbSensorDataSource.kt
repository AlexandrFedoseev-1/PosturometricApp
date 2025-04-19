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

    // Флаг, указывающий на включённый режим калибровки
    @Volatile
    private var isCalibrating: Boolean = false

    // Накопитель калибровочных данных
    private val calibrationAccumulator = mutableListOf<List<Long>>()

    // Количество сообщений для калибровки
    private val calibrationMessageCount = 10

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
            postToast("No USB devices found")
            close()
            return@callbackFlow
        }

        // Для демонстрации используем первый найденный драйвер
        val driver: UsbSerialDriver = availableDrivers[0]
        val connection = usbManager.openDevice(driver.device)
        if (connection == null) {
            postToast("Could not open USB device")
            close()
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
            postToast("Error initializing port: ${e.localizedMessage}")
            close()
            return@callbackFlow
        }

        // Буфер для накопления данных
        val readBuffer = StringBuilder()

        // Реализация ioManager
        ioManager =
            SerialInputOutputManager(serialPort, object : SerialInputOutputManager.Listener {
                override fun onNewData(data: ByteArray?) {
                    try {
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
                                Log.d("fullMessage", "fullMessage: '$fullMessage'")
                                if (fullMessage.startsWith("S") && fullMessage.endsWith("#")) {
                                    val content = fullMessage.substring(1, fullMessage.length - 1)
                                    val parts = content.split("$")
                                    if (parts.size == 33) {
                                        try {
                                            var sensorValues =
                                                parts.subList(0, 32).map { it.toLong() }
                                            val temperature = parts[32].toDouble()

                                            // Если режим калибровки включён, накапливаем данные, не отправляя их дальше
                                            if (isCalibrating) {
                                                calibrationAccumulator.add(sensorValues)
                                                if (calibrationAccumulator.size >= calibrationMessageCount) {
                                                    // Вычисляем offset'ы по накопленным данным
                                                    val numSensors =
                                                        calibrationAccumulator.first().size
                                                    val offsets = List(numSensors) { index ->
                                                        calibrationAccumulator.map { it[index] }
                                                            .average().toLong()
                                                    }
                                                    calibrationOffsets = offsets
                                                    Log.d(
                                                        "CALIBRATION",
                                                        "Calibration offsets computed: $offsets"
                                                    )
                                                    // Выключаем режим калибровки и очищаем накопитель
                                                    isCalibrating = false
                                                    calibrationAccumulator.clear()
                                                    // Можно уведомить UI, что калибровка завершена (через какой-либо callback или LiveData)
                                                }
                                            }
                                            // Применяем калибровку, если она проведена
                                            calibrationOffsets?.let { offsets ->
                                                sensorValues =
                                                    sensorValues.mapIndexed { index, value ->
                                                        value - offsets[index]
                                                    }
                                            }
                                            // Если не в режиме калибровки, сразу отправляем данные в поток
                                            val sensorDataUsb =
                                                SensorDataUsb(sensorValues, temperature)
                                            Log.d("sensorValues", "sensorValues: $sensorValues")
                                            trySend(sensorDataUsb).isSuccess


//                                        Log.d("SEND", "sensorDataUsb: $sensorDataUsb")
                                        } catch (e: Exception) {
                                            Log.d("SEND", "Error parsing message: $fullMessage", e)
                                            postToast("Parsing error: ${e.localizedMessage}")
                                        }
                                    } else {
                                        Log.d(
                                            "SEND",
                                            "Received incomplete or invalid message: $fullMessage"
                                        )
                                    }
                                } else {
                                    Log.d(
                                        "SEND",
                                        "Received incomplete or invalid message: $fullMessage"
                                    )
                                }
                                // Удаляем обработанный фрагмент из буфера
                                readBuffer.delete(0, endIndex + 1)
                                endIndex = readBuffer.indexOf("#")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("SEND", "Unexpected error while processing data", e)
                        postToast("Data processing error: ${e.localizedMessage}")
                    }
                }

                override fun onRunError(e: Exception?) {
                    // Вывод ошибки через Toast и корректное завершение потока
                    postToast("USB error: ${e?.localizedMessage ?: "Unknown error"}")
                    close()  // Закрываем поток без передачи ошибки, чтобы приложение не крашнулось
                }
            })

        ioManager?.let { manager ->
            val job = launch(Dispatchers.IO) {
                try {
                    Log.d("ioManager", "Starting IO Manager run")
                    manager.run()  // Блокирующий вызов
                    Log.d("ioManager", "IO Manager run finished")
                } catch (e: Exception) {
                    Log.e("ioManager", "Error during IO manager run", e)
                    postToast("IO manager error: ${e.localizedMessage}")
                }
            }
            awaitClose {
                try {
                    manager.stop()
                    serialPort?.close()
                } catch (e: Exception) {
                    Log.e("ioManager", "Error closing port", e)
                    postToast("Error closing USB port: ${e.localizedMessage}")
                }
                job.cancel()
            }
        } ?: run {
            postToast("IO Manager initialization failed")
            close()
        }
    }

    // Вспомогательная функция для показа Toast на главном потоке
    private fun postToast(message: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Запускает калибровку сенсоров.
     * Собирает calibrationMessageCount полных сообщений и вычисляет среднее значение для каждого сенсора.
     * Сохранённые offset'ы затем используются для коррекции данных.
     */
    suspend fun calibrateSensors(): Boolean = withContext(Dispatchers.IO) {
        // Включаем режим калибровки и очищаем накопитель
        isCalibrating = true
        calibrationAccumulator.clear()
        // Если поток уже запущен, он начнет накапливать данные.
        // Если нет – можно запустить startListening() для сбора данных.
        postToast("Calibration started")
        true

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
