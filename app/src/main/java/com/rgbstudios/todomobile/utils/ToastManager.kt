package com.rgbstudios.todomobile.utils

import android.content.Context
import android.widget.Toast

class ToastManager {

    fun showShortToast(context: Context, text: String?) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT)
            .show()
    }

    fun showLongToast(context: Context, text: String?) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT)
            .show()
    }
}