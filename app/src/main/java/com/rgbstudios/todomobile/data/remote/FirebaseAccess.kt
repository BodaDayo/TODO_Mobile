package com.rgbstudios.todomobile.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class FirebaseAccess {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val crashlytics = Firebase.crashlytics


    fun signIn(email: String, pass: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {
            if (it.isSuccessful) {
                callback(true, null)
            } else {
                val errorMessage =
                    it.exception?.message?.substringAfter(": ")
                        ?: "Unknown error occurred!\nTry Again"
                callback(false, errorMessage)
            }
        }
    }

    fun signUp(email: String, pass: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener {
            if (it.isSuccessful) {
                callback(true, null)
            } else {
                val errorMessage =
                    it.exception?.message?.substringAfter(": ")
                        ?: "Unknown error occurred!\nTry Again"
                callback(false, errorMessage)
            }
        }
    }

    fun logOut(callback: (Boolean, String?) -> Unit) {
        Log.d("aaaaLog", "attempt entering firebaseAccess")
        try {
            auth.signOut()
            callback(true, null)
        } catch (e: Exception) {
            Log.e("aaaaLog", "Error logging out: ${e.message}", e)
            callback(false, e.message)
        }
    }

    fun getTasksListRef(userId: String): DatabaseReference {

        return database.reference
            .child("users")
            .child(userId)
            .child("tasks")
    }

    fun getUserDetailsRef(userId: String): DatabaseReference {

        return database.reference
            .child("users")
            .child(userId)
            .child("userDetails")
    }

    fun getAvatarStorageRef(userId: String): StorageReference {

        return storage.reference
            .child("avatars")
            .child(userId)
    }

    fun addLog(message: String) {
        crashlytics.log(message)
    }

    fun setUserId(userId: String) {
        crashlytics.setUserId(userId)
    }

    fun recordCaughtException(e: Exception) {
        crashlytics.recordException(e)
    }

}