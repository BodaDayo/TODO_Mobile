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

    val auth = FirebaseAuth.getInstance()
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
        try {
            auth.signOut()
            callback(true, null)
        } catch (e: Exception) {
            callback(false, e.message)
        }
    }

    fun changePasswordAndEmail(newEmail: String, newPassword: String, callback: (Boolean, String?) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser

        // Step 1: Update Email
        user?.updateEmail(newEmail)
            ?.addOnCompleteListener { emailUpdateTask ->
                if (emailUpdateTask.isSuccessful) {
                    // Step 2: Change Password
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { passwordUpdateTask ->
                            if (passwordUpdateTask.isSuccessful) {
                                callback(true, null)
                            } else {
                                callback(false, "Failed to update password.")
                            }
                        }
                } else {
                    callback(false, "Failed to update email.")
                }
            }
    }

    fun deleteAccountAndData(callback: (Boolean, String?) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid

        // Delete Firebase Authentication account
        user?.delete()
            ?.addOnCompleteListener { accountDeleteTask ->
                if (accountDeleteTask.isSuccessful) {
                    // Delete data from Realtime Database
                    if (userId != null) {
                        val userRef = database.reference.child("users").child(userId)
                        userRef.removeValue()
                    }

                    // Delete avatar from Firebase Storage
                    if (userId != null) {
                        val avatarRef = storage.reference.child("avatars").child(userId)
                        avatarRef.delete()
                    }

                    callback(true, null)
                } else {
                    callback(false, "Failed to delete account.")
                }
            }
    }

    fun getTasksListRef(userId: String): DatabaseReference {

        return database.reference
            .child("users")
            .child(userId)
            .child("tasks")
    }

    fun getCategoriesListRef(userId: String): DatabaseReference {

        return database.reference
            .child("users")
            .child(userId)
            .child("categories")
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