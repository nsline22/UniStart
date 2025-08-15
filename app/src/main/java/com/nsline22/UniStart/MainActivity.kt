package com.nsline22.UniStart

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.animation.AnimationUtils
import com.nsline22.UniStart.R

class MainActivity : AppCompatActivity() {

    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var pairedDevices: Set<BluetoothDevice>
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    private lateinit var deviceSpinner: Spinner
    private lateinit var outputTextView: TextView
    private lateinit var commandEditText: EditText
    private lateinit var connectButton: Button
    private lateinit var button1: Button
    private lateinit var button2: Button
    private lateinit var buttonF: Button
    private lateinit var buttonA: Button
    private lateinit var buttonSend: Button
    private lateinit var buttonL: Button
    private lateinit var buttonU: Button

    private val handler = Handler(Looper.getMainLooper())
    private val statusHandler = Handler(Looper.getMainLooper())
    private var isConnected = false
    private val statusUpdateInterval = 30000L
    private val REQUEST_PERMISSION_CODE = 1001

    // Для хранения последнего выбранного устройства
    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "UniStartPrefs"
    private val LAST_DEVICE_KEY = "lastDeviceAddress"

    private fun requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permission = Manifest.permission.BLUETOOTH_CONNECT
            val isGranted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

            if (!isGranted) {
                val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permission)

                if (shouldShowRationale) {
                    ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_PERMISSION_CODE)
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_PERMISSION_CODE)
                }
            } else {
                setupBluetooth()
            }
        } else {
            setupBluetooth()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
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
            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = android.net.Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }
        builder.setNegativeButton("Exit") { _, _ ->
            finish()
        }
        builder.setCancelable(false)
        builder.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        initViews()
        requestBluetoothPermission()
        setupButtons()
        setupEditText()
        updateUIForConnectedState(false)

        commandEditText.postDelayed({
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(commandEditText, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }

    private fun initViews() {
        deviceSpinner = findViewById(R.id.deviceSpinner)
        outputTextView = findViewById(R.id.outputTextView)
        commandEditText = findViewById(R.id.commandEditText)
        connectButton = findViewById(R.id.connectButton)
        button1 = findViewById(R.id.button1)
        button2 = findViewById(R.id.button2)
        buttonF = findViewById(R.id.buttonF)
        buttonA = findViewById(R.id.buttonA)
        buttonL = findViewById(R.id.buttonL)
        buttonU = findViewById(R.id.buttonU)
        buttonSend = findViewById(R.id.buttonSend)

        outputTextView.movementMethod = ScrollingMovementMethod()

        val indicator = findViewById<View>(R.id.connectionIndicator)
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
        refreshPairedDevices()
    }

    @SuppressLint("MissingPermission")
    private fun refreshPairedDevices() {
        pairedDevices = bluetoothAdapter.bondedDevices
        val deviceList = mutableListOf<String>()
        val deviceAddresses = mutableListOf<String>()

        pairedDevices.forEach { device ->
            deviceList.add("${device.name} (${device.address})")
            deviceAddresses.add(device.address)
        }

        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            deviceList
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        deviceSpinner.adapter = adapter

        // Восстановление последнего выбранного устройства
        val lastDeviceAddress = sharedPreferences.getString(LAST_DEVICE_KEY, null)
        if (lastDeviceAddress != null && deviceAddresses.contains(lastDeviceAddress)) {
            val position = deviceAddresses.indexOf(lastDeviceAddress)
            deviceSpinner.setSelection(position)
        }

        if (deviceList.isEmpty()) {
            showMessage("No paired devices or bluetooth turned off")
        }
    }

    private val receiveRunnable = Runnable {
        val buffer = ByteArray(1024)
        var bytes: Int

        while (true) {
            try {
                bytes = inputStream?.read(buffer) ?: -1
                if (bytes > 0) {
                    val receivedData = String(buffer, 0, bytes)
                    runOnUiThread {
                        outputTextView.append(receivedData)

                        val scrollAmount = outputTextView.layout.getLineTop(outputTextView.lineCount) - outputTextView.height
                        if (scrollAmount > 0) {
                            outputTextView.scrollTo(0, scrollAmount)
                        }
                    }
                }
            } catch (e: IOException) {
                runOnUiThread {
                    showMessage("Data receive error : ${e.message}")
                    disconnectBluetooth()
                }
                break
            }
        }
    }

    private val statusUpdateRunnable = object : Runnable {
        override fun run() {
            if (isConnected) {
                sendCommand("S", clearOutput = true)
            }
            handler.postDelayed(this, statusUpdateInterval)
        }
    }

    private fun setupButtons() {
        connectButton.setOnClickListener {
            if (isConnected) disconnectBluetooth() else connectToSelectedDevice()
        }

        button1.setOnClickListener { sendCommand("1", clearOutput = true) }
        button2.setOnClickListener { sendCommand("2", clearOutput = true) }
        buttonF.setOnClickListener { sendCommand("F", clearOutput = true) }
        buttonA.setOnClickListener { sendCommand("A", clearOutput = true) }
        buttonL.setOnClickListener { sendCommand("L", clearOutput = true) }
        buttonU.setOnClickListener { sendCommand("9374", clearOutput = true) }
        buttonSend.setOnClickListener {
            val command = commandEditText.text.toString().trim()
            if (command.isNotEmpty()) {
                sendCommand(command, clearOutput = true)
                commandEditText.text.clear()
            }
        }
    }

    private fun setupEditText() {
        commandEditText.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(commandEditText, InputMethodManager.SHOW_IMPLICIT)
        }

        commandEditText.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN &&
                keyCode == KeyEvent.KEYCODE_ENTER) {
                val command = commandEditText.text.toString().trim()
                if (command.isNotEmpty()) {
                    sendCommand(command, clearOutput = true)
                    commandEditText.text.clear()
                }
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(commandEditText.windowToken, 0)
                true
            } else {
                false
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToSelectedDevice() {
        showMessage("Connecting...")
        startBlinkingIndicator()

        if (pairedDevices.isEmpty()) {
            showMessage("No available devices or bluetooth turned off")
            stopBlinkingIndicator()
            return
        }

        val selectedPosition = deviceSpinner.selectedItemPosition
        if (selectedPosition == -1) {
            showMessage("Choose device")
            stopBlinkingIndicator()
            return
        }

        val selectedDevice = pairedDevices.elementAt(selectedPosition)

        // Сохраняем выбранное устройство
        sharedPreferences.edit().putString(LAST_DEVICE_KEY, selectedDevice.address).apply()

        Thread {
            try {
                bluetoothSocket = selectedDevice.createRfcommSocketToServiceRecord(uuid)
                bluetoothSocket?.connect()

                outputStream = bluetoothSocket?.outputStream
                inputStream = bluetoothSocket?.inputStream

                runOnUiThread {
                    isConnected = true
                    connectButton.text = getString(R.string.disconnect)
                    showMessage("Connected to ${selectedDevice.name}")
                    updateUIForConnectedState(true)
                    stopBlinkingIndicator()

                    Thread(receiveRunnable).start()
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

    private fun startBlinkingIndicator() {
        runOnUiThread {
            val indicator = findViewById<View>(R.id.connectionIndicator)
            val animation = AnimationUtils.loadAnimation(this, R.anim.blink_animation)
            indicator.startAnimation(animation)

            // Устанавливаем желтый цвет для мигания
            val drawable = ContextCompat.getDrawable(this, R.drawable.connection_indicator)?.mutate()
            drawable?.setTint(ContextCompat.getColor(this, android.R.color.holo_orange_light))
            indicator.background = drawable
        }
    }

    private fun stopBlinkingIndicator() {
        runOnUiThread {
            val indicator = findViewById<View>(R.id.connectionIndicator)
            indicator.clearAnimation()
            // Вернем обычный цвет (серый) после остановки
            val drawable = ContextCompat.getDrawable(this, R.drawable.connection_indicator)?.mutate()
            drawable?.setTint(ContextCompat.getColor(this, android.R.color.darker_gray))
            indicator.background = drawable
        }
    }

    private fun disconnectBluetooth() {
        try {
            handler.removeCallbacks(statusUpdateRunnable)
            bluetoothSocket?.close()
            outputStream?.close()
            inputStream?.close()
        } catch (e: IOException) {
            showMessage("Disconnect error: ${e.message}")
        } finally {
            isConnected = false
            bluetoothSocket = null
            outputStream = null
            inputStream = null

            runOnUiThread {
                connectButton.text = getString(R.string.connect)
                updateUIForConnectedState(false)
                showMessage("Disconnected or unable to connect")
                stopBlinkingIndicator()
            }
        }
    }

    private fun sendCommand(command: String, clearOutput: Boolean = true) {
        if (!isConnected) {
            showMessage("Not connected to device")
            return
        }

        if (clearOutput) {
            runOnUiThread {
                outputTextView.text = ""
            }
        }

        Thread {
            try {
                outputStream?.write("$command\n".toByteArray())
                outputStream?.flush()
            } catch (e: IOException) {
                runOnUiThread {
                    showMessage("Send error: ${e.message}")
                    disconnectBluetooth()
                }
            }
        }.start()
    }

    private fun appendToOutput(text: String) {
        outputTextView.append(text)

        val scrollAmount = outputTextView.layout.getLineTop(outputTextView.lineCount) - outputTextView.height
        if (scrollAmount > 0) {
            outputTextView.scrollTo(0, scrollAmount)
        } else {
            outputTextView.scrollTo(0, 0)
        }
    }

    private fun updateUIForConnectedState(connected: Boolean) {
        connectButton.text = if (connected) getString(R.string.disconnect) else getString(R.string.connect)

        val indicator = findViewById<View>(R.id.connectionIndicator)
        handler.postDelayed({
            val color = if (connected) {
                ContextCompat.getColor(this, R.color.icolor) // Красный для подключенного
            } else {
                ContextCompat.getColor(this, android.R.color.darker_gray) // Серый для отключенного
            }

            val drawable = ContextCompat.getDrawable(this, R.drawable.connection_indicator)?.mutate()
            drawable?.setTint(color)
            indicator.background = drawable
        }, 500) // Задержка 500 мс
    }

    private fun showMessage(message: String) {
        val snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT)
        snackbar.view.setBackgroundColor(ContextCompat.getColor(this, R.color.snackbar_bg))
        val textView = snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(ContextCompat.getColor(this, R.color.snackbar_text))
        snackbar.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectBluetooth()
        handler.removeCallbacksAndMessages(null)
    }
}