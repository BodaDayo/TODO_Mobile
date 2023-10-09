package com.rgbstudios.todomobile.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.TodoMobileApplication
import com.rgbstudios.todomobile.data.remote.FirebaseAccess
import com.rgbstudios.todomobile.databinding.FragmentSignUpBinding
import com.rgbstudios.todomobile.utils.ToastManager
import com.rgbstudios.todomobile.viewmodel.TodoViewModel
import com.rgbstudios.todomobile.viewmodel.TodoViewModelFactory


class SignUpFragment : Fragment() {

    private val sharedViewModel: TodoViewModel by activityViewModels {
        TodoViewModelFactory(activity?.application as TodoMobileApplication)
    }

    private val firebase = FirebaseAccess()
    private val auth = firebase.auth
    private lateinit var binding: FragmentSignUpBinding
    private val toastManager = ToastManager()
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

        binding.apply {

            signInTv.setOnClickListener {
                findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
            }

            signUpButton.text = getString(R.string.register)
            signUpButton.setOnClickListener {
                val email = emailEt.text.toString().trim()
                val pass = passEt.text.toString().trim()
                val confirmPass = confirmPassEt.text.toString().trim()

                if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
                    if (pass == confirmPass) {

                        // Define a regex pattern for a strong password
                        val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$"

                        if (pass.matches(Regex(passwordPattern))) {

                            progressBar.visibility = View.VISIBLE
                            signUpButton.text = ""
                            firebase.signUp(email, pass) { signUpSuccessful, errorMessage ->
                                if (signUpSuccessful) {
                                    // Get the user ID from Firebase Auth
                                    val userId = auth.currentUser?.uid ?: ""

                                    // Send new user to repository
                                    sharedViewModel.setUpNewUser(
                                        userId,
                                        email,
                                        pass,
                                        requireContext(),
                                        resources,
                                        TAG
                                    ) { isSuccessful ->
                                        if (isSuccessful) {
                                            findNavController().navigate(R.id.action_signUpFragment_to_homeFragment)
                                            toastManager.showShortToast(
                                                requireContext(),
                                                "Set up complete!, Let's get things done! \uD83D\uDE80",
                                            )
                                        } else {
                                            Log.d("SignUpFragment", "UserEntity initiation failed")
                                        }
                                    }
                                } else {
                                    toastManager.showShortToast(
                                        requireContext(),
                                        errorMessage
                                    )
                                }
                                progressBar.visibility = View.GONE
                                signUpButton.text = getString(R.string.register)
                            }
                        } else {
                            toastManager.showLongToast(
                                requireContext(),
                                "Password must be at least 8 characters long, containing uppercase, lowercase, and numbers.",
                            )
                        }
                    } else {
                        toastManager.showShortToast(
                            requireContext(),
                            "Passwords do not match. Please try again.",
                        )
                    }
                } else {
                    toastManager.showShortToast(
                        requireContext(),
                        "Empty fields are not allowed !!"
                    )
                }
            }

            googleLoginButton.setOnClickListener {
                toastManager.showShortToast(
                    requireContext(),
                    "Coming soon!"
                )
            }
        }
    }

    companion object {
        private const val TAG = "SignUpFragment"
    }

}