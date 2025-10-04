package com.nsline22.UniStart

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.nsline22.UniStart.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var viewPagerAdapter: OnboardingViewPagerAdapter
    private val dots = mutableListOf<View>()

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
        setupDots()
        setupListeners()
        updateButtonVisibility()
    }

    private fun setupViewPager() {
        viewPagerAdapter = OnboardingViewPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter
        binding.viewPager.isUserInputEnabled = false

        // Слушатель для обновления кружков при смене страницы
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateDots(position)
                updateButtonVisibility()
            }
        })
    }

    private fun setupDots() {
        dots.clear()
        binding.dotsContainer.removeAllViews()

        for (i in 0 until viewPagerAdapter.itemCount) {
            val dot = View(this)
            val size = resources.getDimensionPixelSize(R.dimen.dot_size)
            val margin = resources.getDimensionPixelSize(R.dimen.dot_margin)

            val layoutParams = LinearLayout.LayoutParams(size, size)
            layoutParams.setMargins(margin, 0, margin, 0)
            dot.layoutParams = layoutParams

            // Создаем круглый background
            val background = GradientDrawable()
            background.shape = GradientDrawable.OVAL
            background.setColor(getDotColor(i == 0)) // Первая точка активная

            dot.background = background
            binding.dotsContainer.addView(dot)
            dots.add(dot)
        }
    }

    private fun updateDots(currentPosition: Int) {
        dots.forEachIndexed { index, dot ->
            val isActive = index == currentPosition

            // Плавное изменение цвета
            val background = GradientDrawable()
            background.shape = GradientDrawable.OVAL
            background.setColor(getDotColor(isActive))
            dot.background = background
        }
    }

    private fun getDotColor(isActive: Boolean): Int {
        return if (isActive) {
            ContextCompat.getColor(this, R.color.accent_purple)
        } else {
            ContextCompat.getColor(this, R.color.dot_inactive)
        }
    }

    private fun setupListeners() {
        binding.nextButton.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem < viewPagerAdapter.itemCount - 1) {
                binding.viewPager.currentItem = currentItem + 1
            } else {
                completeOnboarding()
            }
        }

        binding.backButton.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem > 0) {
                binding.viewPager.currentItem = currentItem - 1
            }
        }
    }

    private fun updateButtonVisibility() {
        val currentItem = binding.viewPager.currentItem
        val totalItems = viewPagerAdapter.itemCount

        // Показываем кнопку "Назад" только если не на первой странице
        if (currentItem == 0) {
            binding.backButton.visibility = View.GONE
        } else {
            binding.backButton.visibility = View.VISIBLE
        }

        // Меняем текст кнопки "Далее" на "Завершить" на последней странице
        if (currentItem == totalItems - 1) {
            binding.nextButton.text = "COMPLETE"
        } else {
            binding.nextButton.text = "NEXT"
        }
    }

    private fun completeOnboarding() {
        try {
            // Получаем PIN код из текущего фрагмента
            val currentFragment = getCurrentFragment()
            val pinCode = when (currentFragment) {
                is PinSetupFragment -> currentFragment.getPinCode()
                else -> "0000"
            }

            // Получаем выбранное устройство
            val deviceFragment =
                supportFragmentManager.findFragmentByTag("f1") as? DeviceSelectionFragment
            val selectedDevice = deviceFragment?.getSelectedDevice()

            // Сохраняем настройки
            sharedPreferences.edit().apply {
                putBoolean("onboarding_complete", true)
                putString("device_pin", pinCode)
                selectedDevice?.let {
                    putString("selected_device", it)
                }
                apply() // Используем apply() для немедленного сохранения
            }

            // Логируем для отладки
            android.util.Log.d("Onboarding", "Saved PIN: $pinCode, Device: $selectedDevice")

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } catch (e: Exception) {
            android.util.Log.e("Onboarding", "Error completing onboarding", e)
            // Если что-то пошло не так, все равно переходим
            sharedPreferences.edit().putBoolean("onboarding_complete", true).apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    // Добавляем метод для получения текущего фрагмента
    private fun getCurrentFragment(): Fragment? {
        return try {
            val currentItem = binding.viewPager.currentItem
            val fragmentTag = "f$currentItem"
            supportFragmentManager.findFragmentByTag(fragmentTag)
        } catch (e: Exception) {
            null
        }
    }
}