package com.rgbstudios.todomobile.ui.fragments

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
import com.rgbstudios.todomobile.data.remote.FirebaseAccess
import com.rgbstudios.todomobile.databinding.FragmentSignUpBinding
import com.rgbstudios.todomobile.viewmodel.TodoViewModel
import com.rgbstudios.todomobile.viewmodel.TodoViewModelFactory


class SignUpFragment : Fragment() {

    private val sharedViewModel: TodoViewModel by activityViewModels {
        TodoViewModelFactory(activity?.application as TodoMobileApplication)
    }

    private val firebase = FirebaseAccess()
    private val auth = firebase.auth
    private lateinit var binding: FragmentSignUpBinding
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

                    // Define a regex pattern for a strong password
                    val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&+=])(?=\\S+\$).{8,}\$"


                    if (pass.matches(Regex(passwordPattern))) {
                        binding.progressBar.visibility = View.VISIBLE
                        firebase.signUp(email, pass) { signUpSuccessful, errorMessage ->
                            if (signUpSuccessful) {
                                // Get the user ID from Firebase Auth
                                val userId = auth.currentUser?.uid ?: ""

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
                                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                            }
                            binding.progressBar.visibility = View.GONE
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Password must be at least 8 characters long, containing uppercase, lowercase, numbers, and special characters.",
                            Toast.LENGTH_LONG
                        ).show()
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

        binding.googleLoginButton.setOnClickListener {
            Toast.makeText(requireContext(), "Coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
    companion object {
        private const val TAG = "SignUpFragment"
    }

}