package com.nsline22.UniStart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nsline22.UniStart.databinding.FragmentThemeSelectionBinding

class ThemeSelectionFragment : Fragment() {

    private var _binding: FragmentThemeSelectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentThemeSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lightThemeButton.setOnClickListener {
            selectTheme(0)
        }

        binding.darkThemeButton.setOnClickListener {
            selectTheme(1)
        }

        binding.materialThemeButton.setOnClickListener {
            selectTheme(2)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun selectTheme(theme: Int) {
        binding.lightThemeButton.isSelected = false
        binding.darkThemeButton.isSelected = false
        binding.materialThemeButton.isSelected = false

        when (theme) {
            0 -> binding.lightThemeButton.isSelected = true
            1 -> binding.darkThemeButton.isSelected = true
            2 -> binding.materialThemeButton.isSelected = true
        }

        val onboardingActivity = activity as? OnboardingActivity
        onboardingActivity?.getSharedPrefs()?.edit()?.putInt("app_theme", theme)?.apply()
    }
}