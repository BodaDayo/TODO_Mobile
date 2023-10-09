package com.rgbstudios.todomobile.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.TodoMobileApplication
import com.rgbstudios.todomobile.data.remote.FirebaseAccess
import com.rgbstudios.todomobile.databinding.FragmentSignInBinding
import com.rgbstudios.todomobile.utils.DialogManager
import com.rgbstudios.todomobile.utils.ToastManager
import com.rgbstudios.todomobile.viewmodel.TodoViewModel
import com.rgbstudios.todomobile.viewmodel.TodoViewModelFactory
import java.util.concurrent.Executor

class SignInFragment : Fragment() {

    private val sharedViewModel: TodoViewModel by activityViewModels {
        TodoViewModelFactory(activity?.application as TodoMobileApplication)
    }

    private val firebase = FirebaseAccess()
    private val auth = firebase.auth
    private lateinit var binding: FragmentSignInBinding
    private val dialogManager = DialogManager()
    private val toastManager = ToastManager()
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

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

        binding.apply {
            signUpTv.setOnClickListener {
                findNavController().navigate(R.id.action_signInFragment_to_signUpFragment)
            }

            loginButton.text = getString(R.string.login)
            loginButton.setOnClickListener {
                val email = emailEt.text.toString().trim()
                val pass = passEt.text.toString().trim()

                if (email.isNotEmpty() && pass.isNotEmpty()) {

                    progressBar.visibility = View.VISIBLE
                    loginButton.text = ""
                    firebase.signIn(email, pass) { signInSuccessful, errorMessage ->
                        if (signInSuccessful) {
                            // Get the user ID from Firebase Auth
                            val userId = auth.currentUser?.uid ?: ""

                            // Send new user details to repository
                            sharedViewModel.setUpNewUser(
                                userId,
                                email,
                                pass,
                                requireContext(),
                                resources,
                                TAG
                            ) { isSuccessful ->
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
                        progressBar.visibility = View.GONE
                        loginButton.text = getString(R.string.login)
                    }
                } else {
                    Toast.makeText(context, "Empty fields are not allowed!", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            forgotPasswordTV.setOnClickListener {
                showForgotPasswordDialog()
            }

            googleLoginButton.setOnClickListener {
                toastManager.showShortToast(requireContext(), "Coming soon!")
            }

            fingerprintButton.setOnClickListener {
                showBiometricListener()
                createPromptInfo()
                biometricPrompt.authenticate(promptInfo)
            }

            // Check if biometric authentication is enabled
            val isBiometricEnabled = sharedViewModel.isBiometricEnabled.value ?: false

            if (isBiometricEnabled) {
                fingerprintLayout.visibility = View.VISIBLE
            } else {
                fingerprintLayout.visibility = View.GONE
            }
        }

    }

    private fun showBiometricListener() {
        executor = ContextCompat.getMainExecutor(requireContext())
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errString != "Cancel") {
                    toastManager.showShortToast(requireContext(), errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                toastManager.showShortToast(requireContext(), "Authentication failed. Please try again.")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)

                val sharedPreferences = requireActivity().getSharedPreferences("TODOMobilePrefs", Context.MODE_PRIVATE)

                val email = sharedPreferences.getString("email", "") ?: ""
                val pass = sharedPreferences.getString("pass", "") ?: ""

                if (email.isNotEmpty() && pass.isNotEmpty()) {

                    binding.progressBar.visibility = View.VISIBLE
                    binding.loginButton.text = ""
                    firebase.signIn(email, pass) { signInSuccessful, errorMessage ->
                        if (signInSuccessful) {
                            // Get the user ID from Firebase Auth
                            val userId = auth.currentUser?.uid ?: ""

                            // Send new user details to repository
                            sharedViewModel.setUpNewUser(
                                userId,
                                email,
                                pass,
                                requireContext(),
                                resources,
                                TAG
                            ) { isSuccessful ->
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
                        binding.loginButton.text = getString(R.string.login)
                    }
                } else {
                    toastManager.showShortToast(requireContext(), "Authentication failed. Please sign in using other methods.")
                }

            }
        })
    }

    private fun createPromptInfo() {
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verify Fingerprint")
            .setSubtitle("")
            .setNegativeButtonText("Cancel")
            .build()
    }

    private fun showForgotPasswordDialog() {
        dialogManager.showForgotPasswordDialog(this, auth)
    }

    companion object {
        private const val TAG = "SignInFragment"
    }

}