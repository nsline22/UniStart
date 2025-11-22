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
    private var isFragmentVisible = false

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
    }

    private fun setupPinInput() {
        binding.pinInput.hint = "PIN"
        binding.pinInput.inputType = InputType.TYPE_CLASS_NUMBER
        binding.pinInput.isFocusable = true
        binding.pinInput.isFocusableInTouchMode = true

        binding.pinInput.setOnClickListener {
            if (isFragmentVisible) {
                forceShowKeyboard()
            }
        }

        binding.root.setOnClickListener {
            if (isFragmentVisible) {
                forceShowKeyboard()
            }
        }
    }

    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)
        isFragmentVisible = menuVisible

        if (menuVisible && _binding != null) {
            binding.root.postDelayed({
                if (isFragmentVisible && _binding != null) {
                    binding.pinInput.requestFocus()
                    forceShowKeyboard()
                }
            }, 300)
        } else if (_binding != null) {
            hideKeyboard()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isFragmentVisible && _binding != null) {
            binding.root.postDelayed({
                if (isFragmentVisible && _binding != null) {
                    binding.pinInput.requestFocus()
                    forceShowKeyboard()
                }
            }, 300)
        }
    }

    private fun forceShowKeyboard() {
        if (_binding == null || !isFragmentVisible) return

        try {
            binding.pinInput.requestFocus()

            val imm = ContextCompat.getSystemService(
                requireContext(),
                InputMethodManager::class.java
            )
            imm?.showSoftInput(binding.pinInput, InputMethodManager.SHOW_IMPLICIT)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard()
    }

    private fun hideKeyboard() {
        if (_binding == null) return

        try {
            val imm = ContextCompat.getSystemService(
                requireContext(),
                InputMethodManager::class.java
            )
            imm?.hideSoftInputFromWindow(binding.pinInput.windowToken, 0)
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
            val pin = binding.pinInput.text?.toString()?.trim() ?: ""

            when {
                pin.isEmpty() -> {
                    ""
                }
                pin.length < 4 -> {
                    pin
                }
                !pin.all { it.isDigit() } -> {
                    showMessage("PIN must contain only digits")
                    ""
                }
                else -> pin
            }
        } catch (e: Exception) {
            ""
        }
    }
}