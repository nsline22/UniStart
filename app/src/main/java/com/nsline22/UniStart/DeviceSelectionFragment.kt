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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.nsline22.UniStart.databinding.FragmentDeviceSelectionBinding
import com.google.android.material.snackbar.Snackbar

class DeviceSelectionFragment : Fragment() {

    private var _binding: FragmentDeviceSelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var pairedDevices: Set<BluetoothDevice>
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
        checkBluetoothPermissions()

        // Кнопка обновления списка устройств
        binding.refreshButton.setOnClickListener {
            refreshPairedDevices()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    REQUEST_BLUETOOTH_PERMISSION
                )
            } else {
                checkBluetoothEnabled()
            }
        } else {
            checkBluetoothEnabled()
        }
    }

    private fun checkBluetoothEnabled() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(requireContext(), "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            // Запрашиваем включение Bluetooth
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
                }
            } else {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
            }
        } else {
            setupBluetooth()
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupBluetooth() {
        if (bluetoothAdapter.isEnabled) {
            refreshPairedDevices()
            Toast.makeText(requireContext(), "Bluetooth is ready", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun refreshPairedDevices() {
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(requireContext(), "Bluetooth is disabled", Toast.LENGTH_SHORT).show()
            return
        }

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(requireContext(), "Bluetooth permission required", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            pairedDevices = bluetoothAdapter.bondedDevices
            val deviceList = mutableListOf<String>()

            pairedDevices.forEach { device ->
                deviceList.add("${device.name} (${device.address})")
            }

            val adapter = ArrayAdapter(
                requireContext(),
                R.layout.spinner_item,
                deviceList
            )
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            binding.deviceSpinner.adapter = adapter

            if (deviceList.isEmpty()) {
                Toast.makeText(requireContext(), "No paired devices found", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Found ${deviceList.size} paired devices", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(requireContext(), "Bluetooth permission denied", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error refreshing devices", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(
                    requireContext(),
                    "Bluetooth permission required to discover devices",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_ENABLE_BLUETOOTH -> {
                if (resultCode == android.app.Activity.RESULT_OK) {
                    // Bluetooth включен пользователем
                    setupBluetooth()
                    Toast.makeText(requireContext(), "Bluetooth enabled", Toast.LENGTH_SHORT).show()
                } else {
                    // Пользователь отказался включать Bluetooth
                    Toast.makeText(requireContext(), "Bluetooth is required for device connection", Toast.LENGTH_LONG).show()
                }
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