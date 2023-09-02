package com.rgbstudios.todomobile.ui.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.TodoMobileApplication
import com.rgbstudios.todomobile.data.entity.UserEntity
import com.rgbstudios.todomobile.data.repository.TodoRepository
import com.rgbstudios.todomobile.ui.AvatarManager
import com.rgbstudios.todomobile.databinding.FragmentSignUpBinding
import com.rgbstudios.todomobile.viewmodel.TodoViewModel
import com.rgbstudios.todomobile.viewmodel.TodoViewModelFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class SignUpFragment : Fragment() {

    private val sharedViewModel: TodoViewModel by activityViewModels {
        TodoViewModelFactory(activity?.application as TodoMobileApplication)
    }

    private lateinit var binding: FragmentSignUpBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        registerEvents()
    }

    private fun registerEvents() {

        binding.signInTv.setOnClickListener {
            findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
        }
        binding.SignUpButton.setOnClickListener {
            val email = binding.emailEt.text.toString().trim()
            val pass = binding.passEt.text.toString().trim()
            val confirmPass = binding.confirmPassEt.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
                if (pass == confirmPass) {

                    binding.progressBar.visibility = View.VISIBLE
                    auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener {

                        if (it.isSuccessful) {

                            // Notify user of successful account creation
                            Toast.makeText(
                                context,
                                "Registered successfully! Setting up your database...",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Get the user ID from Firebase Auth
                            val userId = auth.currentUser?.uid ?: ""

                            /*/ Get random Avatar
                            val defaultAvatarData = getAvatar(requireContext())

                            // Create userEntity with the user details available
                            val newUser = UserEntity(
                                userId = userId,
                                name = null,
                                email = email,
                                occupation = null,
                                avatarFilePath = defaultAvatarData
                            )

                             */

                            // Send new user to repository
                            sharedViewModel.setUpNewUser(userId, email, requireContext(), resources, TAG) { isSuccessful ->
                                if (isSuccessful) {
                                    findNavController().navigate(R.id.action_signUpFragment_to_homeFragment)
                                    Toast.makeText(
                                        context,
                                        "Set up complete!, Let's get things done! \uD83D\uDE80",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Log.d("SignUpFragment", "UserEntity initiation failed")
                                }
                            }
                        } else {
                            val errorMessage =
                                it.exception?.message?.substringAfter(": ")
                                    ?: "Unknown error occurred!\nTry Again"
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                        binding.progressBar.visibility = View.GONE
                    }

                } else {
                    Toast.makeText(
                        context,
                        "Passwords do not match. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(context, "Empty fields are not allowed !!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun getAvatar(context: Context): String? {
        // Get the drawable resource ID of a random avatar
        val avatarResource = AvatarManager().defaultAvatar

        // Convert the drawable resource to a bitmap
        val avatarBitmap = BitmapFactory.decodeResource(resources, avatarResource)

        // Create a directory to store avatars in the app's internal storage
        val avatarsDir = File(context.filesDir, "avatars")
        if (!avatarsDir.exists()) {
            avatarsDir.mkdirs()
        }

        val file = File(avatarsDir, "avatar.png")

        try {
            val stream = FileOutputStream(file)
            avatarBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()

            // Return the file path of the saved file
            return file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }

    companion object {
        private const val TAG = "SignUpFragment"
    }

}