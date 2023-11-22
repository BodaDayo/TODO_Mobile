package com.rgbstudios.todomobile.ui.fragments

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
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
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var webClientId: String

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

            signInTv.setOnClickListener {
                findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
            }

            signUpButton.text = getString(R.string.register)
            signUpButton.setOnClickListener {
                val email = emailEt.text.toString().trim()
                val pass = passEt.text.toString().trim()
                val confirmPass = confirmPassEt.text.toString().trim()

                if (!isValidEmail(email)) {
                    // Show an error message for invalid email format
                    toastManager.showShortToast(requireContext(), "Invalid email format.")
                    return@setOnClickListener
                }

                if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
                    if (pass == confirmPass) {

                        // Define a regex pattern for a strong password
                        val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$"

                        if (pass.matches(Regex(passwordPattern))) {

                            progressBar.visibility = View.VISIBLE
                            signUpButton.text = ""
                            firebase.signUp(email, pass) { signUpSuccessful, errorMessage ->
                                if (signUpSuccessful) {
                                    progressWithSignUp(email, pass, null)
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
                signInWithGoogle()
            }
        }
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
                                        progressWithSignUp(email, emptyPassword, detailsFromGoogle)
                                    } else {
                                        // Returning user
                                        progressWithSignIn(email)
                                    }

                                    binding.accProgressBar.visibility = View.GONE
                                    binding.socialLoginButtonsLayout.visibility = View.VISIBLE

                                    sharedViewModel.updateWebClientId(webClientId)
                                } else {
                                    googleSignInClient.signOut()
                                    binding.socialLoginButtonsLayout.visibility = View.VISIBLE
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

    private fun progressWithSignIn(email: String) {
        // Get the user ID from Firebase Auth
        val userId = auth.currentUser?.uid ?: ""

        // Send new user details to repository
        sharedViewModel.setUpNewUser(
            userId,
            email,
            "",
            requireContext(),
            resources,
            SIGNIN,
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
    private fun progressWithSignUp(email: String, pass: String, extractedDetails: Pair<String, Uri?>?) {
        // Get the user ID from Firebase Auth
        val userId = auth.currentUser?.uid ?: ""

        // Send new user to repository
        sharedViewModel.setUpNewUser(
            userId,
            email,
            pass,
            requireContext(),
            resources,
            TAG,
            extractedDetails
        ) { isSuccessful ->
            if (isSuccessful) {
                navigateToHomeFragment()
                toastManager.showShortToast(
                    requireContext(),
                    "Set up complete!, Let's get things done! \uD83D\uDE80"
                )
            } else {
                firebase.addLog("UserEntity initiation failed at Sign Up")
            }
        }
    }

    private fun navigateToHomeFragment() {
        findNavController().navigate(R.id.action_signUpFragment_to_homeFragment)
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex())
    }

    companion object {
        private const val TAG = "SignUpFragment"
        private const val SIGNIN = "SignInFragment"
    }

}