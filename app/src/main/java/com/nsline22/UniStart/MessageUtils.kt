package com.nsline22.UniStart

import android.app.Activity
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

object MessageUtils {

    fun showMessage(view: View, message: String) {
        val context = view.context
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
        snackbar.view.setBackgroundColor(ContextCompat.getColor(context, R.color.snackbar_bg))
        snackbar.setTextColor(ContextCompat.getColor(context, R.color.snackbar_text))
        snackbar.show()
    }

    fun showMessageLong(view: View, message: String) {
        val context = view.context
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        snackbar.view.setBackgroundColor(ContextCompat.getColor(context, R.color.snackbar_bg))
        snackbar.setTextColor(ContextCompat.getColor(context, R.color.snackbar_text))
        snackbar.show()
    }
}

// Debounce click listener - предотвращает множественные нажатия
fun View.setDebouncedClickListener(debounceTime: Long = 1000L, action: (View) -> Unit) {
    var lastClickTime = 0L
    setOnClickListener { view ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= debounceTime) {
            lastClickTime = currentTime
            action(view)
        }
    }
}

// Extension functions для удобства
fun Activity.showMessage(message: String) {
    val rootView = findViewById<View>(android.R.id.content)
    MessageUtils.showMessage(rootView, message)
}

fun Activity.showMessageLong(message: String) {
    val rootView = findViewById<View>(android.R.id.content)
    MessageUtils.showMessageLong(rootView, message)
}

fun Fragment.showMessage(message: String) {
    view?.let { MessageUtils.showMessage(it, message) }
}

fun Fragment.showMessageLong(message: String) {
    view?.let { MessageUtils.showMessageLong(it, message) }
}