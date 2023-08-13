package com.rgbstudios.todomobile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rgbstudios.todomobile.databinding.DialogForgotPasswordBinding
import com.rgbstudios.todomobile.databinding.FragmentSignInBinding
import com.rgbstudios.todomobile.model.TaskViewModel

class SignInFragment : Fragment() {

    private val sharedViewModel: TaskViewModel by activityViewModels()

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

                sharedViewModel.emailPasswordSignIn(email, pass) { success, exception ->
                    if (success) {
                        sharedViewModel.setupAuthStateListener()
                        findNavController().navigate(R.id.action_signInFragment_to_homeFragment)
                        Toast.makeText(context, "Signed In Successfully!", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        val errorMessage =
                            exception?.message?.substringAfter(": ") ?: "Unknown error occurred!\nTry Again"
                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    }

                    binding.progressBar.visibility = View.GONE
                }
            } else {
                Toast.makeText(context, "Empty fields are not allowed !!", Toast.LENGTH_SHORT)
                    .show()
            }
        }


        binding.forgotPasswordTV.setOnClickListener {
            showForgotPasswordDialog()
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
                sharedViewModel.resetLoginPassword(email) { isSuccessful ->
                    if (isSuccessful) {
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

}