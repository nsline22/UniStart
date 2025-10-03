package com.nsline22.UniStart

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.*
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.nsline22.UniStart.databinding.ActivityMainBinding
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var isConnected = false

    private val handler = Handler(Looper.getMainLooper())
    private val statusUpdateInterval = 30000L
    private val REQUEST_PERMISSION_CODE = 1001

    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "UniStartPrefs"
    private var devicePinCode = "9374"

    companion object {
        var instance: MainActivity? = null
    }

    private val statusUpdateRunnable = object : Runnable {
        override fun run() {
            if (isConnected) {
                sendCommand("S")
            }
            handler.postDelayed(this, statusUpdateInterval)
        }
    }

    private fun requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permission = Manifest.permission.BLUETOOTH_CONNECT
            val isGranted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

            if (!isGranted) {
                val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
                ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_PERMISSION_CODE)
            } else {
                setupBluetooth()
            }
        } else {
            setupBluetooth()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            val permission = Manifest.permission.BLUETOOTH_CONNECT
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupBluetooth()
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    showSettingsDialog()
                } else {
                    showMessage("Permission needed for Bluetooth")
                    handler.postDelayed({ requestBluetoothPermission() }, 1500)
                }
            }
        }
    }

    private fun showSettingsDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Permission needed")
        builder.setMessage("For Bluetooth needed permission. Give the permission in the settings.")
        builder.setPositiveButton("Open app settings") { _, _ ->
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = android.net.Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }
        builder.setNegativeButton("Exit") { _, _ -> finish() }
        builder.setCancelable(false)
        builder.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        instance = this

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        applySavedTheme()
        devicePinCode = sharedPreferences.getString("device_pin", "9374") ?: "9374"

        initViews()
        setupButtons()
        updateUIForConnectedState(false)
        requestBluetoothPermission()
        setupSettingsButton()
        setupLogsButton()

        // Фоновая прогрузка LogActivity
        preloadLogActivityInBackground()

        val deviceAddress = sharedPreferences.getString("selected_device", null)
        if (deviceAddress != null && !isConnected) {
            connectToDevice(deviceAddress)
        }
    }

    private fun applySavedTheme() {
        if (!::sharedPreferences.isInitialized) return

        val theme = sharedPreferences.getInt("app_theme", 2)
        when (theme) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun initViews() {
        val indicator = binding.connectionIndicator
        val drawable = ContextCompat.getDrawable(this, R.drawable.connection_indicator)?.mutate()
        drawable?.setTint(ContextCompat.getColor(this, android.R.color.darker_gray))
        indicator.background = drawable
    }

    @SuppressLint("MissingPermission")
    private fun setupBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            showMessage("Bluetooth not supported")
            return
        }
    }

    private fun setupSettingsButton() {
        binding.settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupLogsButton() {
        binding.logsButton.setOnClickListener {
            val intent = Intent(this, LogActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    // Метод для очистки логов
    fun clearLogs() {
        val prefs = getSharedPreferences("UniStartPrefs", MODE_PRIVATE)
        prefs.edit().putString("command_history", "").apply()
    }

    // Метод для открытия LogActivity с очисткой логов
    fun openLogsWithClear() {
        clearLogs()
        val intent = Intent(this, LogActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun preloadLogActivityInBackground() {
        Handler(Looper.getMainLooper()).post {
            val intent = Intent(this, LogActivity::class.java)
        }
    }

    private fun setupButtons() {
        binding.connectButton.setOnClickListener {
            if (isConnected) disconnectBluetooth() else {
                val deviceAddress = sharedPreferences.getString("selected_device", null)
                if (deviceAddress != null) {
                    connectToDevice(deviceAddress)
                } else {
                    showMessage("No device selected. Please run onboarding again.")
                }
            }
        }

        binding.buttonF.setOnClickListener {
            // Очищаем логи и открываем страницу логов
            openLogsWithClear()
            // Также отправляем команду "F" на ESP32
            sendCommand("F")
        }

        binding.buttonA.setOnClickListener {
            // Очищаем логи и открываем страницу логов
            openLogsWithClear()
            // Также отправляем команду "A" на ESP32
            sendCommand("A")
        }

        binding.buttonU.setOnClickListener {
            sendCommand(devicePinCode)
        }

        binding.ignitionContainer.setOnClickListener {
            sendCommand("1")
            it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }.start()
        }

        binding.starterContainer.setOnClickListener {
            sendCommand("2")
            it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }.start()
        }

        binding.ledContainer.setOnClickListener {
            sendCommand("3")
            it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }.start()
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(deviceAddress: String) {
        showMessage("Connecting...")
        startBlinkingIndicator()

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            showMessage("Bluetooth not supported")
            stopBlinkingIndicator()
            return
        }

        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
        if (device == null) {
            showMessage("Device not found")
            stopBlinkingIndicator()
            return
        }

        Thread {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothSocket?.connect()

                outputStream = bluetoothSocket?.outputStream
                inputStream = bluetoothSocket?.inputStream

                runOnUiThread {
                    isConnected = true
                    binding.connectButton.text = getString(R.string.disconnect)
                    showMessage("Connected to ${device.name}")
                    updateUIForConnectedState(true)
                    stopBlinkingIndicator()

                    startDataReceiving()
                    handler.postDelayed(statusUpdateRunnable, statusUpdateInterval)
                }
            } catch (e: IOException) {
                runOnUiThread {
                    showMessage("Connection error: ${e.message}")
                    disconnectBluetooth()
                    stopBlinkingIndicator()
                }
            }
        }.start()
    }

    private fun startDataReceiving() {
        Thread {
            val buffer = ByteArray(1024)
            var bytes: Int

            while (isConnected) {
                try {
                    bytes = inputStream?.read(buffer) ?: -1
                    if (bytes > 0) {
                        val receivedData = String(buffer, 0, bytes).trim()
                        if (receivedData.isNotEmpty()) {
                            runOnUiThread {
                                addToCommandHistory(receivedData)
                            }
                        }
                    }
                } catch (e: IOException) {
                    if (isConnected) {
                        runOnUiThread {
                            showMessage("Data receive error: ${e.message}")
                            addToCommandHistory("Connection error: ${e.message}")
                            disconnectBluetooth()
                        }
                    }
                    break
                } catch (e: Exception) {
                    if (isConnected) {
                        runOnUiThread {
                            showMessage("Data receive error: ${e.message}")
                            disconnectBluetooth()
                        }
                    }
                    break
                }
            }
        }.start()
    }

    private fun startBlinkingIndicator() {
        runOnUiThread {
            val indicator = binding.connectionIndicator
            val animation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.blink_animation)
            indicator.startAnimation(animation)
            val drawable = ContextCompat.getDrawable(this, R.drawable.connection_indicator)?.mutate()
            drawable?.setTint(ContextCompat.getColor(this, android.R.color.holo_orange_light))
            indicator.background = drawable
        }
    }

    private fun stopBlinkingIndicator() {
        runOnUiThread {
            val indicator = binding.connectionIndicator
            indicator.clearAnimation()
            val drawable = ContextCompat.getDrawable(this, R.drawable.connection_indicator)?.mutate()
            drawable?.setTint(ContextCompat.getColor(this, android.R.color.darker_gray))
            indicator.background = drawable
        }
    }

    private fun disconnectBluetooth() {
        try {
            handler.removeCallbacks(statusUpdateRunnable)
            isConnected = false
            bluetoothSocket?.close()
            outputStream?.close()
            inputStream?.close()
        } catch (e: IOException) {
            showMessage("Disconnect error: ${e.message}")
        } finally {
            bluetoothSocket = null
            outputStream = null
            inputStream = null

            runOnUiThread {
                binding.connectButton.text = getString(R.string.connect)
                updateUIForConnectedState(false)
                showMessage("Disconnected or unable to connect")
                stopBlinkingIndicator()
            }
        }
    }

    private fun sendCommand(command: String) {
        sendCommandToESP32(command)
    }

    // Публичный метод для вызова из LogActivity
    fun sendCommandDirectly(command: String) {
        runOnUiThread {
            sendCommandToESP32(command)
        }
    }

    private fun sendCommandToESP32(command: String) {
        if (!isConnected) {
            showMessage("Not connected to ESP32")
            addToCommandHistory("> $command (not connected)")
            return
        }

        val cleanCommand = command.trim()

        // ДОБАВЛЯЕМ КОМАНДУ В ЛОГИ ТОЛЬКО ЗДЕСЬ!
        addToCommandHistory("> $cleanCommand")

        Thread {
            try {
                val commandToSend = if (cleanCommand.endsWith("\n")) cleanCommand else "$cleanCommand\n"
                outputStream?.write(commandToSend.toByteArray())
                outputStream?.flush()
            } catch (e: IOException) {
                runOnUiThread {
                    showMessage("Send error: ${e.message}")
                    addToCommandHistory("Error: ${e.message}")
                    disconnectBluetooth()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showMessage("Unexpected error: ${e.message}")
                }
            }
        }.start()
    }

    private fun addToCommandHistory(text: String) {
        val prefs = getSharedPreferences("UniStartPrefs", MODE_PRIVATE)
        val currentHistory = prefs.getString("command_history", "") ?: ""

        val newHistory = if (currentHistory.isEmpty()) {
            text
        } else {
            "$currentHistory\n$text"
        }

        val limitedHistory = if (newHistory.length > 5000) {
            newHistory.lines().takeLast(100).joinToString("\n")
        } else {
            newHistory
        }

        prefs.edit().putString("command_history", limitedHistory).apply()
    }

    private fun updateUIForConnectedState(connected: Boolean) {
        binding.connectButton.text = if (connected) getString(R.string.disconnect) else getString(R.string.connect)
        handler.postDelayed({
            val indicator = binding.connectionIndicator
            val color = if (connected) {
                ContextCompat.getColor(this, R.color.icolor)
            } else {
                ContextCompat.getColor(this, android.R.color.darker_gray)
            }
            val drawable = ContextCompat.getDrawable(this, R.drawable.connection_indicator)?.mutate()
            drawable?.setTint(color)
            indicator.background = drawable
        }, 500)
    }

    private fun showMessage(message: String) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
        snackbar.view.setBackgroundColor(ContextCompat.getColor(this, R.color.snackbar_bg))
        snackbar.setTextColor(ContextCompat.getColor(this, R.color.snackbar_text))
        snackbar.show()
    }

    override fun onResume() {
        super.onResume()
        applySavedTheme()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        disconnectBluetooth()
        handler.removeCallbacksAndMessages(null)
    }
}