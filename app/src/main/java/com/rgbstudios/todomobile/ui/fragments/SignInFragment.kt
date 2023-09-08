package com.rgbstudios.todomobile.ui.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.TodoMobileApplication
import com.rgbstudios.todomobile.data.entity.UserEntity
import com.rgbstudios.todomobile.data.remote.FirebaseAccess
import com.rgbstudios.todomobile.databinding.DialogForgotPasswordBinding
import com.rgbstudios.todomobile.databinding.FragmentSignInBinding
import com.rgbstudios.todomobile.ui.AvatarManager
import com.rgbstudios.todomobile.viewmodel.TodoViewModel
import com.rgbstudios.todomobile.viewmodel.TodoViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SignInFragment : Fragment() {

    private val sharedViewModel: TodoViewModel by activityViewModels {
        TodoViewModelFactory(activity?.application as TodoMobileApplication)
    }

    private lateinit var auth: FirebaseAuth

    private lateinit var binding: FragmentSignInBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        registerEvents()
    }

    private fun registerEvents() {

        binding.signUpTv.setOnClickListener {
            findNavController().navigate(R.id.action_signInFragment_to_signUpFragment)
        }
        binding.loginButton.setOnClickListener {
            val email = binding.emailEt.text.toString().trim()
            val pass = binding.passEt.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty()) {

                binding.progressBar.visibility = View.VISIBLE
                FirebaseAccess().signIn(email, pass) { signInSuccessful, errorMessage ->
                    if (signInSuccessful) {
                        // Get the user ID from Firebase Auth
                        val userId = auth.currentUser?.uid ?: ""

                        // Send new user details to repository
                        sharedViewModel.setUpNewUser(userId, email, requireContext(), resources, TAG) { isSuccessful ->
                            if (isSuccessful) {
                                findNavController().navigate(R.id.action_signInFragment_to_homeFragment)
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
                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                    binding.progressBar.visibility = View.GONE
                }
            } else {
                Toast.makeText(context, "Empty fields are not allowed!", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding.forgotPasswordTV.setOnClickListener {
            showForgotPasswordDialog()
        }

        binding.googleLoginButton.setOnClickListener {
            Toast.makeText(requireContext(), "Coming soon!", Toast.LENGTH_SHORT).show()
        }

    }

    private fun showForgotPasswordDialog() {
        val dialogBinding = DialogForgotPasswordBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.resetPasswordButton.setOnClickListener {
            val email = dialogBinding.emailEditText.text.toString().trim()

            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(
                                requireContext(),
                                "Password reset link sent to your email",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Failed to send password reset email",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        dialog.dismiss()
                    }
            } else {
                Toast.makeText(requireContext(), "Please enter your email", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        dialog.show()
    }

    companion object {
        private const val TAG = "SignInFragment"
    }

}