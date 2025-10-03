package com.nsline22.UniStart

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nsline22.UniStart.databinding.ActivityLogBinding

class LogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogBinding
    private var isConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupBackButton()
    }

    private fun setupViews() {
        binding.outputTextView.movementMethod = ScrollingMovementMethod()

        // Настройка поля ввода команд
        binding.commandEditText.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                val command = binding.commandEditText.text.toString().trim()
                if (command.isNotEmpty()) {
                    sendCommand(command)
                    binding.commandEditText.text.clear()
                }
                hideKeyboard()
                true
            } else {
                false
            }
        }

        binding.buttonSend.setOnClickListener {
            val command = binding.commandEditText.text.toString().trim()
            if (command.isNotEmpty()) {
                sendCommand(command)
                binding.commandEditText.text.clear()
            }
            hideKeyboard()
        }
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun sendCommand(command: String) {
        // Здесь будет логика отправки команды через Bluetooth
        // Пока просто добавляем в лог
        binding.outputTextView.append("> $command\n")

        // Автопрокрутка вниз
        val scrollAmount = binding.outputTextView.layout?.getLineTop(binding.outputTextView.lineCount) ?: 0 - binding.outputTextView.height
        if (scrollAmount > 0) {
            binding.outputTextView.scrollTo(0, scrollAmount)
        } else {
            binding.outputTextView.scrollTo(0, 0)
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.commandEditText.windowToken, 0)
    }

    // Метод для обновления логов из MainActivity
    fun updateLogs(logText: String) {
        runOnUiThread {
            binding.outputTextView.append(logText)
            val scrollAmount = binding.outputTextView.layout?.getLineTop(binding.outputTextView.lineCount) ?: 0 - binding.outputTextView.height
            if (scrollAmount > 0) {
                binding.outputTextView.scrollTo(0, scrollAmount)
            }
        }
    }
}