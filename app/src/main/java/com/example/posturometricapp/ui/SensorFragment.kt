package com.example.posturometricapp.ui

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.registerReceiver
import androidx.core.view.isVisible

import com.example.posturometricapp.R
import com.example.posturometricapp.databinding.FragmentSensorBinding
import com.example.posturometricapp.domain.model.SensorButtonState
import com.google.android.material.button.MaterialButton
import com.hoho.android.usbserial.driver.UsbSerialProber
import org.koin.androidx.viewmodel.ext.android.viewModel


class SensorFragment : Fragment() {
    companion object {

        private const val ACTION_USB_PERMISSION = "com.example.posturometricapp.USB_PERMISSION"

    }

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_USB_PERMISSION -> {
                    synchronized(this) {
                        val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            device?.let {
                                postToast("Разрешение получено")
                                viewModel.setScreenState(SensorButtonState.HasPermission)
                                Log.d(
                                    "USB Connect",
                                    "Permission granted for device: ${it.deviceName}"
                                )
                            }
                        } else {
                            viewModel.setScreenState(SensorButtonState.Empty)
                            Log.d(
                                "USB Connect",
                                "Permission denied for device: ${device?.deviceName}"
                            )
                        }
                    }
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    // кабель выдернули — сразу стопаем сессию
                    postToast("USB отключен, сессия остановлена")
                    viewModel.stopSession()
                }
            }
        }
    }

    private val psychStates = listOf("Спокойный", "Напряженный", "Встревоженный", "Расслабленный")
    private val viewModel by viewModel<SensorViewModel>()
    private var _binding: FragmentSensorBinding? = null
    private val binding get() = _binding!!
    private val sensorIds = listOf(
        R.id.sensor1, R.id.sensor2, R.id.sensor3, R.id.sensor4,
        R.id.sensor5, R.id.sensor6, R.id.sensor7, R.id.sensor8,
        R.id.sensor9, R.id.sensor10, R.id.sensor11, R.id.sensor12,
        R.id.sensor13, R.id.sensor14, R.id.sensor15, R.id.sensor16,
        R.id.sensor17, R.id.sensor18, R.id.sensor19, R.id.sensor20,
        R.id.sensor21, R.id.sensor22, R.id.sensor23, R.id.sensor24,
        R.id.sensor25, R.id.sensor26, R.id.sensor27, R.id.sensor28,
        R.id.sensor29, R.id.sensor30, R.id.sensor31, R.id.sensor32
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSensorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(
                usbPermissionReceiver, filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else registerReceiver(
            requireContext(),
            usbPermissionReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        viewModel.message.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                postToast(message)
                viewModel.clearMessage()
            }
        }
        viewModel.screenState.observe(viewLifecycleOwner) { state ->
            binding.btnRequestUsbPermission.isVisible = state is SensorButtonState.Empty
            binding.btnMoreActions.isVisible =
                state is SensorButtonState.StreamData || state is SensorButtonState.StartSession
            binding.btnChangePsychState.isVisible = state is SensorButtonState.StartSession
            binding.btnStartLiveData.isVisible = state is SensorButtonState.HasPermission
            binding.btnStopLiveData.isVisible =
                state is SensorButtonState.StreamData || state is SensorButtonState.StartSession
            binding.btnEnableReading.isVisible = state is SensorButtonState.StreamData
            binding.btnStopReading.isVisible = state is SensorButtonState.StartSession
        }

        viewModel.psychState.observe(viewLifecycleOwner) { stateName ->
            binding.llPsychState.isVisible = stateName.isNotEmpty()
            binding.tvPsychState.text = stateName
        }

        viewModel.sensorData.observe(viewLifecycleOwner) { sensorData ->
            sensorData.sensorValues.forEachIndexed { index, sensorValue ->
                val color = viewModel.getColorForValue(sensorValue)
                val sensorButton = view.findViewById<MaterialButton>(sensorIds[index])
                sensorButton.backgroundTintList = ColorStateList.valueOf(color)

            }
        }

        binding.btnStopReading.setOnClickListener {
            viewModel.stopSession()
        }
        binding.btnRequestUsbPermission.setOnClickListener {
            requestUsbPermission()
//            viewModel.fakePermissions()
        }
        binding.btnMoreActions.setOnClickListener {
            showMoreActionsMenu(it)
        }
//        binding.btnCalibrateArduino.setOnClickListener {
//            viewModel.calibrate()
//        }
//        binding.btnCalibrateApp.setOnClickListener {
//            viewModel.calibrateSensors()
//        }
        binding.btnStartLiveData.setOnClickListener {
            requestUsbPermission()
//            viewModel.playSession(1)
        }

        binding.btnStopLiveData.setOnClickListener {
            viewModel.stopLiveData()
//            viewModel.stopPlayback()
        }
        binding.btnEnableReading.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Выберите ваше ментальное состояние")
                .setItems(psychStates.toTypedArray()) { _, which ->
                    val state = psychStates[which]
                    viewModel.startSessionWithPsychState(state)
                }
                .show()
        }

        binding.btnChangePsychState.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Изменить ваше ментальное состояние")
                .setItems(psychStates.toTypedArray()) { _, which ->
                    val state = psychStates[which]
                    viewModel.switchPsychState(state)
                }
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopSession()
    }
    override fun onDestroy() {
        super.onDestroy()

        requireActivity().unregisterReceiver(usbPermissionReceiver)
    }

    private fun requestUsbPermission() {
        val usbManager = requireContext().getSystemService(Context.USB_SERVICE) as UsbManager
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)

        if (availableDrivers.isEmpty()) {
            postToast("USB девайс не найден!")
            Log.e("requestUsbPermission", "No USB devices found")
            return
        }

        // Для примера используем первый найденный драйвер
        val driver = availableDrivers[0]
        // Проверяем, есть ли разрешение для данного устройства
        if (!usbManager.hasPermission(driver.device)) {
            // Создаем PendingIntent для запроса разрешения
            val permissionIntent = PendingIntent.getBroadcast(
                requireContext(),
                0,
                Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_UPDATE_CURRENT // или FLAG_UPDATE_CURRENT, в зависимости от версии SDK
            )
            postToast("Получение разрешения для подключения!")
            usbManager.requestPermission(driver.device, permissionIntent)
        } else {
            // Если разрешение уже есть, можно сразу запустить соединение

            Log.d(
                "requestUsbPermission",
                "Permission already granted for device: ${driver.device.deviceName}"
            )
            // Здесь можно вызывать метод для старта работы с устройством, например,
            // usbSensorDataSource.startListening() или аналогичный.
            viewModel.startLiveData()
        }
    }

    private fun showMoreActionsMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.buttons_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_calibrate_arduino -> {
                    viewModel.calibrate()
                    true
                }

                R.id.action_calibrate_app -> {
                    viewModel.calibrateSensors()
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    private fun postToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

}