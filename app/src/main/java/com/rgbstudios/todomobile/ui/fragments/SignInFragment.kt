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
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.TodoMobileApplication
import com.rgbstudios.todomobile.data.remote.FirebaseAccess
import com.rgbstudios.todomobile.databinding.FragmentSignInBinding
import com.rgbstudios.todomobile.utils.DialogManager
import com.rgbstudios.todomobile.viewmodel.TodoViewModel
import com.rgbstudios.todomobile.viewmodel.TodoViewModelFactory

class SignInFragment : Fragment() {

    private val sharedViewModel: TodoViewModel by activityViewModels {
        TodoViewModelFactory(activity?.application as TodoMobileApplication)
    }

    private val firebase = FirebaseAccess()
    private val auth = firebase.auth
    private lateinit var binding: FragmentSignInBinding
    private val dialogManager = DialogManager()

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
                firebase.signIn(email, pass) { signInSuccessful, errorMessage ->
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
        dialogManager.showForgotPasswordDialog(this, auth)
    }

    companion object {
        private const val TAG = "SignInFragment"
    }

}