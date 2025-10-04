package com.nsline22.UniStart

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.nsline22.UniStart.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var isPinVisible = false
    private var isPinClickEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("UniStartPrefs", MODE_PRIVATE)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        setupCurrentSettings()
        setupListeners()
    }

    private fun setupCurrentSettings() {
        // Получаем сохраненные настройки
        val deviceAddress = sharedPreferences.getString("selected_device", null)
        val pinCode = sharedPreferences.getString("device_pin", "0000") ?: "0000"

        // Отображаем информацию об устройстве
        val deviceInfo = if (deviceAddress != null) {
            getDeviceInfo(deviceAddress)
        } else {
            "No device selected"
        }
        binding.deviceInfoText.text = deviceInfo

        // Отображаем PIN-код (маскируем для безопасности)
        val maskedPin = "••••" // Полностью маскируем
        binding.pinInfoText.text = maskedPin

        // Сохраняем полный PIN для отображения при нажатии
        binding.pinInfoText.tag = pinCode
    }

    private fun getDeviceInfo(address: String): String {
        return try {
            val device = getBluetoothDevice(address)
            val deviceName = device?.name ?: "Unknown Device"
            val formattedAddress = formatDeviceAddress(address)

            "$deviceName\n$formattedAddress"
        } catch (e: Exception) {
            formatDeviceAddress(address)
        }
    }

    private fun getBluetoothDevice(address: String): BluetoothDevice? {
        return try {
            bluetoothAdapter.getRemoteDevice(address)
        } catch (e: Exception) {
            null
        }
    }

    private fun formatDeviceAddress(address: String): String {
        return try {
            // Форматируем MAC-адрес в читаемый вид: XX:XX:XX:XX:XX:XX
            address.replace(":", "").chunked(2).joinToString(":")
        } catch (e: Exception) {
            address // Возвращаем как есть в случае ошибки
        }
    }

    private fun setupListeners() {

        binding.factoryResetButton.setOnClickListener {
            showFactoryResetConfirmation()
        }

        binding.aboutBackButton.setOnClickListener {
            finish()
        }

        // Клик по контейнеру устройства - показываем детали
        binding.deviceInfoContainer.setOnClickListener {
            showDeviceDetails()
        }

        // Клик по контейнеру PIN - показываем полный PIN
        binding.pinInfoContainer.setOnClickListener {
            togglePinVisibility()
        }
    }

    private fun showFactoryResetConfirmation() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Factory Reset")
            .setMessage("This will reset all settings and return to device setup. Your current device and PIN will be cleared. Continue?")
            .setPositiveButton("Reset") { dialog, _ ->
                performFactoryReset()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performFactoryReset() {
        // Очищаем только настройки устройства и PIN, оставляя другие настройки
        sharedPreferences.edit().apply {
            remove("selected_device")
            remove("device_pin")
            remove("onboarding_complete")
            apply()
        }

        // Переходим на онбординг
        val intent = Intent(this, OnboardingActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun showDeviceDetails() {
        val deviceAddress = sharedPreferences.getString("selected_device", null)
        if (deviceAddress != null) {
            val device = getBluetoothDevice(deviceAddress)
            val deviceName = device?.name ?: "Unknown Device"
            val deviceType = getDeviceType(device)

            val message = """
                Device: $deviceName
                Address: ${formatDeviceAddress(deviceAddress)}
                Type: $deviceType
            """.trimIndent()

            showInfoDialog("Device Information", message)
        } else {
            android.widget.Toast.makeText(this, "No device selected", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDeviceType(device: BluetoothDevice?): String {
        return when (device?.type) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
            BluetoothDevice.DEVICE_TYPE_LE -> "Low Energy"
            BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual"
            else -> "Unknown"
        }
    }

    private fun togglePinVisibility() {
        if (!isPinClickEnabled) return

        isPinClickEnabled = false
        val currentText = binding.pinInfoText.text.toString()
        val fullPin = binding.pinInfoText.tag as? String ?: "0000"

        if (currentText == "••••") {
            // Показываем полный PIN
            binding.pinInfoText.text = fullPin
            isPinVisible = true

            // Через 3 секунды скрываем обратно
            handler.postDelayed({
                binding.pinInfoText.text = "••••"
                isPinVisible = false
                isPinClickEnabled = true
            }, 3000)
        } else {
            // Скрываем PIN
            binding.pinInfoText.text = "••••"
            isPinVisible = false

            // Включаем клик через 500мс
            handler.postDelayed({
                isPinClickEnabled = true
            }, 500)
        }
    }

    private fun showInfoDialog(title: String, message: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun resetApp() {
        sharedPreferences.edit().clear().apply()
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Обновляем настройки при возвращении на экран
        setupCurrentSettings()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Очищаем handler чтобы избежать утечек памяти
        handler.removeCallbacksAndMessages(null)
    }
}