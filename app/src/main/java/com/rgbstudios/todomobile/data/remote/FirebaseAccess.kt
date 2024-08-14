package com.rgbstudios.todomobile.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.SignInMethodQueryResult
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

    fun fetchSignInMethodsForUser(callback: (List<String>?) -> Unit) {
        val user = auth.currentUser
        val email = user?.email

        if (email == null) {
            callback(null)
            return
        }

        auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    handleSuccessfulTask(task.result, callback)
                } else {
                    handleFailedTask(task.exception, callback)
                }
            }
    }

    private fun handleSuccessfulTask(
        result: SignInMethodQueryResult?,
        callback: (List<String>?) -> Unit
    ) {
        val providersList = mutableListOf<String>()
        val linkedProviders = result?.signInMethods

        if (linkedProviders.isNullOrEmpty()) {
            callback(emptyList())
            return
        }

        if (linkedProviders.contains("google.com")) {
            providersList.add("google")
        }

        if (linkedProviders.contains("facebook.com")) {
            providersList.add("facebook")
        }

        callback(providersList)
    }

    private fun handleFailedTask(
        exception: Exception?,
        callback: (List<String>?) -> Unit
    ) {
        val errorMessage = when (exception) {
            is FirebaseAuthUserCollisionException -> "Email is associated with multiple accounts."
            else -> "Failed to fetch sign-in methods."
        }

        addLog(errorMessage)
        callback(null)
    }

    fun changeEmail(newEmail: String, callback: (Boolean, String?) -> Unit) {
        val user = auth.currentUser

        user?.updateEmail(newEmail)
            ?.addOnCompleteListener {
                if (it.isSuccessful) {
                    callback(true, null)
                } else {
                    val errorMessage = it.exception?.message?.substringAfter(": ")
                    callback(false, errorMessage ?: "Failed to update email.")
                }
            }
    }


    fun changePassword(newPassword: String, callback: (Boolean, String?) -> Unit) {
        val user = auth.currentUser

        user?.updatePassword(newPassword)
            ?.addOnCompleteListener {
                if (it.isSuccessful) {
                    callback(true, null)
                } else {
                    val errorMessage = it.exception?.message?.substringAfter(": ")
                    callback(false, errorMessage ?: "Failed to update password.")
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