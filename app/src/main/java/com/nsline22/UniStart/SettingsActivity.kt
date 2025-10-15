package com.nsline22.UniStart

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.widget.EditText
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
        val deviceAddress = sharedPreferences.getString("selected_device", null)
        val pinCode = sharedPreferences.getString("device_pin", "0000") ?: "0000"

        val deviceInfo = if (deviceAddress != null) {
            getDeviceInfo(deviceAddress)
        } else {
            "No device selected"
        }
        binding.deviceInfoText.text = deviceInfo

        val maskedPin = "••••"
        binding.pinInfoText.text = maskedPin
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
            address.replace(":", "").chunked(2).joinToString(":")
        } catch (e: Exception) {
            address
        }
    }

    private fun setupListeners() {
        binding.syncTimeButton.setOnClickListener {
            syncTime()
        }

        binding.changePinButton.setOnClickListener {
            showChangePinDialog()
        }

        binding.listCardsButton.setOnClickListener {
            MainActivity.instance?.openLogsWithClear()
            MainActivity.instance?.sendCommandDirectly("F")
        }

        binding.addCardButton.setOnClickListener {
            MainActivity.instance?.openLogsWithClear()
            MainActivity.instance?.sendCommandDirectly("A")
        }

        binding.factoryResetButton.setOnClickListener {
            showFactoryResetConfirmation()
        }

        binding.aboutBackButton.setOnClickListener {
            finish()
        }

        binding.deviceInfoContainer.setOnClickListener {
            showDeviceDetails()
        }

        binding.pinInfoContainer.setOnClickListener {
            togglePinVisibility()
        }
    }

    private fun showChangePinDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Change PIN Code")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.hint = "Enter new 4-digit PIN"
        input.maxLines = 1

        val container = android.widget.FrameLayout(this)
        val params = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = resources.getDimensionPixelSize(R.dimen.dialog_margin)
        params.rightMargin = resources.getDimensionPixelSize(R.dimen.dialog_margin)
        input.layoutParams = params
        container.addView(input)

        builder.setView(container)

        builder.setPositiveButton("Save") { dialog, _ ->
            val newPin = input.text.toString().trim()
            if (newPin.length == 4 && newPin.all { it.isDigit() }) {
                // Сохраняем пинкод в SharedPreferences
                sharedPreferences.edit().putString("device_pin", newPin).apply()
                binding.pinInfoText.tag = newPin
                binding.pinInfoText.text = "••••"

                // Отправляем команду на ESP32 для смены пинкода
                val changePinCommand = "P:$newPin"
                MainActivity.instance?.sendCommandSilent(changePinCommand)

                android.widget.Toast.makeText(
                    this,
                    "PIN changed and sent to device",
                    android.widget.Toast.LENGTH_SHORT
                ).show()

                // Обновляем PIN в MainActivity
                MainActivity.instance?.onResume()
            } else {
                android.widget.Toast.makeText(
                    this,
                    "PIN must be exactly 4 digits",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()

        // Показываем клавиатуру
        input.requestFocus()
        handler.postDelayed({
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }, 200)
    }

    private fun syncTime() {
        MainActivity.instance?.let { mainActivity ->
            mainActivity.syncTimeWithESP32()
            android.widget.Toast.makeText(
                this,
                "Synchronizing time with ESP32...",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } ?: run {
            android.widget.Toast.makeText(
                this,
                "MainActivity not available. Please return to main screen.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
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
        sharedPreferences.edit().apply {
            remove("selected_device")
            remove("device_pin")
            remove("onboarding_complete")
            apply()
        }

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
            binding.pinInfoText.text = fullPin
            isPinVisible = true

            handler.postDelayed({
                binding.pinInfoText.text = "••••"
                isPinVisible = false
                isPinClickEnabled = true
            }, 3000)
        } else {
            binding.pinInfoText.text = "••••"
            isPinVisible = false

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

    override fun onResume() {
        super.onResume()
        setupCurrentSettings()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}