package com.nsline22.UniStart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        binding.pinInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.pinInput.text?.clear()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun getPinCode(): String {
        return try {
            binding.pinInput.text?.toString()?.trim() ?: "9374"
        } catch (e: Exception) {
            "9374"
        }
    }
}