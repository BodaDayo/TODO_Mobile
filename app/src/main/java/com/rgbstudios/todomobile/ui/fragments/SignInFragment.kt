package com.rgbstudios.todomobile.ui.fragments

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.TodoMobileApplication
import com.rgbstudios.todomobile.data.remote.FirebaseAccess
import com.rgbstudios.todomobile.databinding.FragmentSignInBinding
import com.rgbstudios.todomobile.utils.DialogManager
import com.rgbstudios.todomobile.utils.SharedPreferencesManager
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
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var webClientId: String

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
        webClientId = getString(R.string.default_web_client_id)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
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
                                    navigateToHomeFragment()
                                    toastManager.showShortToast(
                                        requireContext(),
                                        "Set up complete!, Let's get things done! \uD83D\uDE80"
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
                        loginButton.text = getString(R.string.login)
                    }
                } else {
                    toastManager.showShortToast(requireContext(), "Empty fields are not allowed!")
                }
            }

            forgotPasswordTV.setOnClickListener {
                showForgotPasswordDialog()
            }

            googleLoginButton.setOnClickListener {
                signInWithGoogle()
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
        biometricPrompt =
            BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errString != "Cancel") {
                        toastManager.showShortToast(requireContext(), errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    toastManager.showShortToast(
                        requireContext(),
                        "Authentication failed. Please try again."
                    )
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)

                    val sharedPreferences = SharedPreferencesManager(requireContext())

                    val email = sharedPreferences.getString("email", "")
                    val pass = sharedPreferences.getString("pass", "")

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
                        toastManager.showShortToast(
                            requireContext(),
                            "Authentication failed. Please sign in using other methods."
                        )
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

    private fun signInWithGoogle() {
        val sigInIntent = googleSignInClient.signInIntent
        launcher.launch(sigInIntent)
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val signInTask = GoogleSignIn.getSignedInAccountFromIntent(result.data)

                if (signInTask.isSuccessful) {
                    // Get the GoogleSignInAccount
                    val account = signInTask.result

                    if (account != null) {
                        // Check if the Google account is linked to a Firebase user account
                        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                        val user = firebase.auth.currentUser

                        if (user != null) {
                            if (user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }) {
                                // The Google account is already linked to a Firebase user.
                                // Sign in with Firebase
                                user.linkWithCredential(credential)
                                    .addOnCompleteListener { linkTask ->
                                        if (linkTask.isSuccessful) {
                                            // Successfully linked
                                            // Navigate to your desired screen after successful sign-in
                                            navigateToHomeFragment()
                                        } else {
                                            // Handle the error
                                            val exception = linkTask.exception
                                            toastManager.showShortToast(
                                                requireContext(),
                                                "Failed to link Google account."
                                            )
                                        }
                                    }
                            } else {
                                // The Google account is not linked to a Firebase user.
                                // You can handle this case accordingly.
                                // For example, show an error message or take some other action.
                                toastManager.showShortToast(
                                    requireContext(),
                                    "Google account is not linked to any user."
                                )
                            }
                        }
                    }

                } else {
                    // Google Sign-In failed
                    toastManager.showShortToast(requireContext(), "Google Sign-In failed.")
                }
            }
        }

    private fun navigateToHomeFragment() {
        findNavController().navigate(R.id.action_signInFragment_to_homeFragment)
    }

    companion object {
        private const val TAG = "SignInFragment"
    }

}