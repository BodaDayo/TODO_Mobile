package com.rgbstudios.todomobile.ui.fragments

import android.app.Activity
import android.net.Uri
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
import com.google.android.recaptcha.RecaptchaAction.Companion.SIGNUP
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
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

        val sharedPreferences = SharedPreferencesManager(requireContext())

        val storedEmail = sharedPreferences.getString("email", "")
        val storedPass = sharedPreferences.getString("pass", "")

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
                            progressWithSignIn(email, pass)
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
                showBiometricListener(storedEmail, storedPass)
                createPromptInfo()
                biometricPrompt.authenticate(promptInfo)
            }

            // Check if biometric authentication is enabled
            val isBiometricEnabled = if (storedPass.isNotBlank()) {
                sharedViewModel.isBiometricEnabled.value == true
            } else {
                false
            }

            if (isBiometricEnabled) {
                fingerprintLayout.visibility = View.VISIBLE
            } else {
                fingerprintLayout.visibility = View.GONE
            }
        }
    }

    private fun progressWithSignIn(email: String, pass: String) {
        // Get the user ID from Firebase Auth
        val userId = auth.currentUser?.uid ?: ""

        // Send new user details to repository
        sharedViewModel.setUpNewUser(
            userId,
            email,
            pass,
            requireContext(),
            resources,
            TAG,
            null
        ) { isSuccessful ->
            if (isSuccessful) {
                navigateToHomeFragment()
                toastManager.showShortToast(
                    requireContext(),
                    "Set up complete!, Let's get things done! \uD83D\uDE80"
                )
            } else {
                firebase.addLog("UserEntity initiation failed at Sign In")
            }
        }
    }

    private fun progressWithFirstTimeSignIn(email: String, extractedDetails: Pair<String, Uri?>?) {
        // Get the user ID from Firebase Auth
        val userId = auth.currentUser?.uid ?: ""

        // Send new user to repository
        sharedViewModel.setUpNewUser(
            userId,
            email,
            "",
            requireContext(),
            resources,
            SIGNUP,
            extractedDetails
        ) { isSuccessful ->
            if (isSuccessful) {
                navigateToHomeFragment()
                toastManager.showShortToast(
                    requireContext(),
                    "Set up complete!, Let's get things done! \uD83D\uDE80"
                )
            } else {
                firebase.addLog("UserEntity initiation failed at Sign In")
            }
        }
    }

    private fun showBiometricListener(email: String, pass: String) {
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
                                    TAG,
                                    null
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
                val googleSignInTask = GoogleSignIn.getSignedInAccountFromIntent(result.data)

                if (googleSignInTask.isSuccessful) {
                    val account = googleSignInTask.result

                    binding.accProgressBar.visibility = View.VISIBLE
                    binding.socialLoginButtonsLayout.visibility = View.GONE

                    val email = account.email ?: ""
                    val emptyPassword = ""
                    val displayName = account?.displayName ?: ""
                    val photoUrl = account?.photoUrl
                    val detailsFromGoogle = Pair(displayName, photoUrl)

                    if (account != null) {
                        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                        firebase.auth.signInWithCredential(credential)
                            .addOnCompleteListener { signInTask ->
                                if (signInTask.isSuccessful) {
                                    val isNewUser = signInTask.result?.additionalUserInfo?.isNewUser ?: false

                                    if (isNewUser) {
                                        // It's the first time the user is using these credentials
                                        progressWithFirstTimeSignIn(email, detailsFromGoogle)
                                    } else {
                                        // Returning user
                                        progressWithSignIn(email, emptyPassword)
                                    }

                                    binding.accProgressBar.visibility = View.GONE
                                    binding.socialLoginButtonsLayout.visibility = View.VISIBLE

                                    sharedViewModel.updateWebClientId(webClientId)
                                } else {
                                    googleSignInClient.signOut()
                                    toastManager.showShortToast(requireContext(), "Sign-In with Google failed.")
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
        private const val SIGNUP = "SignUpFragment"
    }

}