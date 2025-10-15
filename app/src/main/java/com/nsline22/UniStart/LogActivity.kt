package com.nsline22.UniStart

import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.nsline22.UniStart.databinding.ActivityLogBinding

class LogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogBinding
    private var isFirstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Анимация входа слева
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)

        setupViews()
        setupBackButton()
        setupClearButton()
        loadCommandHistory()

        binding.commandEditText.requestFocus()

        startLogUpdates()
    }

    private fun setupViews() {
        setupCommandInput()
    }

    private fun setupCommandInput() {
        binding.commandEditText.setOnClickListener {
            showKeyboard()
        }

        binding.commandEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showKeyboard()
            }
        }

        binding.commandEditText.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                sendCommandFromInput()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }

        binding.buttonSend.setOnClickListener {
            sendCommandFromInput()
        }

        binding.commandEditText.postDelayed({
            binding.commandEditText.requestFocus()
            showKeyboard()
        }, 300)
    }

    private fun setupClearButton() {
        binding.clearLogsButton.setOnClickListener {
            clearLogs()
        }
    }

    private fun clearLogs() {
        binding.outputTextView.text = ""
        val prefs = getSharedPreferences("UniStartPrefs", MODE_PRIVATE)
        prefs.edit().putString("command_history", "").apply()
        addToLogs("Logs cleared")
    }

    private fun startLogUpdates() {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                updateLogsFromSharedPreferences()
                handler.postDelayed(this, 500)
            }
        }
        handler.post(runnable)
    }

    private fun updateLogsFromSharedPreferences() {
        val prefs = getSharedPreferences("UniStartPrefs", MODE_PRIVATE)
        val currentHistory = prefs.getString("command_history", "") ?: ""
        val currentText = binding.outputTextView.text.toString()

        if (currentHistory != currentText) {
            binding.outputTextView.text = currentHistory

            if (shouldScrollToBottom()) {
                scrollToBottom()
            }
        }

        if (isFirstLoad) {
            isFirstLoad = false
        }
    }

    private fun showKeyboard() {
        try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.commandEditText, InputMethodManager.SHOW_IMPLICIT)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideKeyboard() {
        try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.commandEditText.windowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendCommandFromInput() {
        val command = binding.commandEditText.text.toString().trim()
        if (command.isNotEmpty()) {
            // НЕ добавляем команду в логи здесь!
            // Только отправляем команду
            sendCommandToMainActivity(command)

            binding.commandEditText.text.clear()
            scrollToBottom()
        }
    }

    private fun sendCommandToMainActivity(command: String) {
        // Отправляем команду напрямую в MainActivity
        MainActivity.instance?.sendCommandDirectly(command)
    }

    private fun addToLogs(text: String) {
        val currentText = binding.outputTextView.text.toString()
        val newText = if (currentText.isEmpty()) {
            text
        } else {
            "$currentText\n$text"
        }
        binding.outputTextView.text = newText
        scrollToBottom()

        val prefs = getSharedPreferences("UniStartPrefs", MODE_PRIVATE)
        prefs.edit().putString("command_history", newText).apply()
    }

    private fun shouldScrollToBottom(): Boolean {
        return isFirstLoad || binding.commandEditText.text?.isNotEmpty() == true
    }

    private fun scrollToBottom() {
        binding.logScrollView.post {
            binding.logScrollView.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }

    private fun loadCommandHistory() {
        val prefs = getSharedPreferences("UniStartPrefs", MODE_PRIVATE)
        val history = prefs.getString("command_history", "")
        history?.let {
            if (it.isNotEmpty()) {
                binding.outputTextView.text = it
                scrollToBottom()
            }
        }
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            hideKeyboard()
            finish()
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun onBackPressed() {
        hideKeyboard()
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard()
    }
}