package com.rgbstudios.todomobile.utils

import android.content.Context
import android.widget.Toast

class ToastManager {
    fun showToast(context: Context, text: String) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT)
            .show()
    }
}