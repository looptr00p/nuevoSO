package com.nuevoso.launcher.agent.executors

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.provider.Settings

class ToggleSettingExecutor(private val context: Context) {

    private var torchOn = false

    fun execute(setting: String, value: String?): String {
        return when (setting.lowercase()) {
            "flashlight" -> toggleFlashlight(value)
            "wifi" -> openPanel(Settings.ACTION_WIFI_SETTINGS, "Wi-Fi")
            "bluetooth" -> openPanel(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth")
            "data" -> openPanel(Settings.ACTION_DATA_ROAMING_SETTINGS, "Datos móviles")
            "brightness" -> openPanel(Settings.ACTION_DISPLAY_SETTINGS, "Brillo")
            "sound" -> openPanel(Settings.ACTION_SOUND_SETTINGS, "Sonido")
            "airplane" -> openPanel(Settings.ACTION_AIRPLANE_MODE_SETTINGS, "Modo avión")
            else -> "No sé cómo controlar \"$setting\"."
        }
    }

    private fun toggleFlashlight(value: String?): String {
        return try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return "Este teléfono no tiene linterna."
            val turnOn = when (value?.lowercase()) {
                "on" -> true
                "off" -> false
                else -> !torchOn
            }
            cm.setTorchMode(cameraId, turnOn)
            torchOn = turnOn
            if (turnOn) "Linterna encendida." else "Linterna apagada."
        } catch (e: Exception) {
            "No pude controlar la linterna: ${e.message}"
        }
    }

    private fun openPanel(action: String, label: String): String {
        val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "Abriendo ajustes de $label."
    }
}
