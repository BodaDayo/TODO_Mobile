package com.rgbstudios.todomobile.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.database.FirebaseDatabase
import com.rgbstudios.todomobile.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

    }
}
