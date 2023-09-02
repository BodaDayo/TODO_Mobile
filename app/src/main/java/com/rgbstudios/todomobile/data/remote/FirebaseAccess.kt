package com.rgbstudios.todomobile.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class FirebaseAccess {

    val auth = FirebaseAuth.getInstance()

    fun getTasksListRef(userId: String): DatabaseReference {

        return FirebaseDatabase.getInstance().reference
            .child("users")
            .child(userId)
            .child("tasks")
    }

    fun getUserDetailsRef(userId: String): DatabaseReference {

        return FirebaseDatabase.getInstance().reference
            .child("users")
            .child(userId)
            .child("userDetails")
    }

    fun getAvatarStorageRef(userId: String): StorageReference {

        return FirebaseStorage.getInstance().reference
            .child("avatars")
            .child(userId)
    }
}