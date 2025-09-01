package com.nsline22.UniStart

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.nsline22.UniStart.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var viewPagerAdapter: OnboardingViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("UniStartPrefs", MODE_PRIVATE)

        if (sharedPreferences.getBoolean("onboarding_complete", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setupViewPager()
        setupListeners()
    }

    private fun setupViewPager() {
        viewPagerAdapter = OnboardingViewPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter
        binding.viewPager.isUserInputEnabled = false

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            // Можно установить иконки или текст для индикаторов
        }.attach()
    }

    private fun setupListeners() {
        binding.nextButton.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem < viewPagerAdapter.itemCount - 1) {
                binding.viewPager.currentItem = currentItem + 1
            } else {
                completeOnboarding() // ДОБАВЬ ЭТУ СТРОКУ
            }
        }
    }

    private fun completeOnboarding() {
        try {
            // Простая проверка перед сохранением
            val deviceFragment = supportFragmentManager.findFragmentByTag("f1") as? DeviceSelectionFragment
            val pinFragment = supportFragmentManager.findFragmentByTag("f3") as? PinSetupFragment

            deviceFragment?.getSelectedDevice()?.let {
                sharedPreferences.edit().putString("selected_device", it).apply()
            }

            pinFragment?.getPinCode()?.let {
                if (it.isNotEmpty()) {
                    sharedPreferences.edit().putString("device_pin", it).apply()
                }
            }

            sharedPreferences.edit().putBoolean("onboarding_complete", true).apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } catch (e: Exception) {
            // Если что-то пошло не так, все равно переходим
            sharedPreferences.edit().putBoolean("onboarding_complete", true).apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    fun getSharedPrefs(): SharedPreferences {
        return sharedPreferences
    }
}