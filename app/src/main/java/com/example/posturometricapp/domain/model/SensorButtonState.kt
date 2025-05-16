package com.example.posturometricapp.domain.model

sealed interface SensorButtonState {
    data object Empty: SensorButtonState
    data object HasPermission: SensorButtonState
    data object StreamData: SensorButtonState
    data object StartSession: SensorButtonState
}
