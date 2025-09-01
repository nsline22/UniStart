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
import android.text.method.ScrollingMovementMethod
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
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

    private val handler = Handler(Looper.getMainLooper())
    private var isConnected = false
    private val statusUpdateInterval = 30000L
    private val REQUEST_PERMISSION_CODE = 1001

    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "UniStartPrefs"
    private var devicePinCode = "9374"

    private val receiveRunnable = Runnable {
        val buffer = ByteArray(1024)
        var bytes: Int

        while (true) {
            try {
                bytes = inputStream?.read(buffer) ?: -1
                if (bytes > 0) {
                    val receivedData = String(buffer, 0, bytes)
                    runOnUiThread {
                        binding.outputTextView.append(receivedData)
                        val scrollAmount = binding.outputTextView.layout.getLineTop(binding.outputTextView.lineCount) - binding.outputTextView.height
                        if (scrollAmount > 0) {
                            binding.outputTextView.scrollTo(0, scrollAmount)
                        }
                    }
                }
            } catch (e: IOException) {
                runOnUiThread {
                    showMessage("Data receive error: ${e.message}")
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

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        applySavedTheme()
        devicePinCode = sharedPreferences.getString("device_pin", "9374") ?: "9374"

        initViews()
        setupButtons()
        setupEditText()
        updateUIForConnectedState(false)
        requestBluetoothPermission()

        val deviceAddress = sharedPreferences.getString("selected_device", null)
        if (deviceAddress != null && !isConnected) {
            connectToDevice(deviceAddress)
        }

        binding.appIcon.setOnLongClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            true
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
        binding.outputTextView.movementMethod = ScrollingMovementMethod()
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

        binding.button1.setOnClickListener { sendCommand("1", clearOutput = true) }
        binding.button2.setOnClickListener { sendCommand("2", clearOutput = true) }
        binding.buttonF.setOnClickListener { sendCommand("F", clearOutput = true) }
        binding.buttonA.setOnClickListener { sendCommand("A", clearOutput = true) }
        binding.buttonL.setOnClickListener { sendCommand("L", clearOutput = true) }
        binding.buttonU.setOnClickListener { sendCommand(devicePinCode, clearOutput = true) }
        binding.buttonSend.setOnClickListener {
            val command = binding.commandEditText.text.toString().trim()
            if (command.isNotEmpty()) {
                sendCommand(command, clearOutput = true)
                binding.commandEditText.text.clear()
            }
        }
    }

    private fun setupEditText() {
        binding.commandEditText.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.commandEditText, InputMethodManager.SHOW_IMPLICIT)
        }

        binding.commandEditText.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                val command = binding.commandEditText.text.toString().trim()
                if (command.isNotEmpty()) {
                    sendCommand(command, clearOutput = true)
                    binding.commandEditText.text.clear()
                }
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.commandEditText.windowToken, 0)
                true
            } else {
                false
            }
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
                binding.connectButton.text = getString(R.string.connect)
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
                binding.outputTextView.text = ""
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