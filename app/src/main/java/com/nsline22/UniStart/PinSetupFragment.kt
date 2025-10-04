package com.nsline22.UniStart

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.nsline22.UniStart.databinding.FragmentPinSetupBinding

class PinSetupFragment : Fragment() {

    private var _binding: FragmentPinSetupBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPinSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPinInput()

        // Гарантированный показ клавиатуры
        binding.root.postDelayed({
            forceShowKeyboard()
        }, 500)
    }

    private fun setupPinInput() {
        // Устанавливаем hint
        binding.pinInput.hint = "PIN"

        // Настраиваем для числового ввода
        binding.pinInput.inputType = InputType.TYPE_CLASS_NUMBER
        binding.pinInput.isFocusable = true
        binding.pinInput.isFocusableInTouchMode = true
        binding.pinInput.requestFocus()

        // Очищаем при фокусе (только если пусто)
        binding.pinInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && binding.pinInput.text?.isEmpty() == true) {
                binding.pinInput.text?.clear()
            }
        }

        // Показываем клавиатуру при касании
        binding.pinInput.setOnClickListener {
            forceShowKeyboard()
        }

        // Также показываем клавиатуру при касании всей области
        binding.root.setOnClickListener {
            forceShowKeyboard()
        }
    }

    private fun forceShowKeyboard() {
        try {
            binding.pinInput.requestFocus()
            binding.pinInput.isFocusable = true
            binding.pinInput.isFocusableInTouchMode = true

            val inputMethodManager = ContextCompat.getSystemService(
                requireContext(),
                InputMethodManager::class.java
            )

            // Несколько попыток показать клавиатуру
            inputMethodManager?.showSoftInput(binding.pinInput, InputMethodManager.SHOW_IMPLICIT)

            // Дополнительная попытка через 100мс
            binding.pinInput.postDelayed({
                inputMethodManager?.showSoftInput(
                    binding.pinInput,
                    InputMethodManager.SHOW_IMPLICIT
                )
            }, 100)

            // Еще одна попытка через 300мс
            binding.pinInput.postDelayed({
                inputMethodManager?.showSoftInput(binding.pinInput, InputMethodManager.SHOW_FORCED)
            }, 300)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        // Показываем клавиатуру при возвращении на фрагмент
        binding.root.postDelayed({
            forceShowKeyboard()
        }, 200)
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard()
    }

    private fun hideKeyboard() {
        try {
            val inputMethodManager = ContextCompat.getSystemService(
                requireContext(),
                InputMethodManager::class.java
            )
            inputMethodManager?.hideSoftInputFromWindow(binding.pinInput.windowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideKeyboard()
        _binding = null
    }

    fun getPinCode(): String {
        return try {
            val pin = binding.pinInput.text?.toString()?.trim()
            android.util.Log.d("PinSetupFragment", "Current PIN input: '$pin'")

            when {
                pin.isNullOrEmpty() -> {
                    android.util.Log.d("PinSetupFragment", "PIN is empty, using default: 0000")
                    "0000"
                }

                pin.length != 4 -> {
                    android.util.Log.d(
                        "PinSetupFragment",
                        "PIN length is ${pin.length}, using: $pin"
                    )
                    pin // Используем то что введено, даже если не 4 цифры
                }

                else -> {
                    android.util.Log.d("PinSetupFragment", "Valid PIN: $pin")
                    pin
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PinSetupFragment", "Error getting PIN", e)
            "0000"
        }
    }
}