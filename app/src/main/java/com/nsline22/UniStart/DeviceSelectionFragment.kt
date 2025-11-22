package com.nsline22.UniStart

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.nsline22.UniStart.databinding.FragmentDeviceSelectionBinding

class DeviceSelectionFragment : Fragment() {

    private var _binding: FragmentDeviceSelectionBinding? = null
    private val binding get() = _binding!!

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var pairedDevices: Set<BluetoothDevice> = emptySet()
    private val REQUEST_BLUETOOTH_PERMISSION = 101
    private val REQUEST_ENABLE_BLUETOOTH = 102

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализируем адаптер сразу
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            showMessage("Bluetooth not supported")
            return
        }

        binding.refreshButton.setOnClickListener {
            checkBluetoothPermissions()
        }

        // Запрашиваем разрешения при показе фрагмента
        checkBluetoothPermissions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED -> {
                    checkBluetoothEnabled()
                }
                else -> {
                    requestPermissions(
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                        REQUEST_BLUETOOTH_PERMISSION
                    )
                }
            }
        } else {
            // Android 6-11
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED -> {
                    checkBluetoothEnabled()
                }
                else -> {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ),
                        REQUEST_BLUETOOTH_PERMISSION
                    )
                }
            }
        }
    }

    private fun checkBluetoothEnabled() {
        val adapter = bluetoothAdapter ?: return

        if (!adapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            try {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
            } catch (e: SecurityException) {
                showMessage("Cannot enable Bluetooth")
            }
        } else {
            refreshPairedDevices()
        }
    }

    @SuppressLint("MissingPermission")
    private fun refreshPairedDevices() {
        val adapter = bluetoothAdapter ?: return

        if (!adapter.isEnabled) {
            showMessage("Bluetooth is disabled")
            return
        }

        try {
            pairedDevices = adapter.bondedDevices
            val deviceList = mutableListOf<String>()

            pairedDevices.forEach { device ->
                deviceList.add("${device.name ?: "Unknown"} (${device.address})")
            }

            val arrayAdapter = ArrayAdapter(
                requireContext(),
                R.layout.spinner_item,
                deviceList
            )
            arrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            binding.deviceSpinner.adapter = arrayAdapter

            if (deviceList.isEmpty()) {
                showMessage("No paired devices found")
            }
        } catch (e: SecurityException) {
            showMessage("Permission denied")
        } catch (e: Exception) {
            showMessage("Error: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkBluetoothEnabled()
            } else {
                val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    "Bluetooth permission required"
                } else {
                    "Location permission required for Bluetooth"
                }
                showMessage(message)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == android.app.Activity.RESULT_OK) {
                refreshPairedDevices()
            } else {
                showMessage("Bluetooth is required")
            }
        }
    }

    fun getSelectedDevice(): String? {
        return try {
            val selectedPosition = binding.deviceSpinner.selectedItemPosition
            if (selectedPosition >= 0 && selectedPosition < pairedDevices.size) {
                pairedDevices.elementAt(selectedPosition).address
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}