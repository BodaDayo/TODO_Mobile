package com.rgbstudios.todomobile.ui.fragments

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Editable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.data.remote.FirebaseAccess
import com.rgbstudios.todomobile.databinding.FragmentSettingsDetailsBinding
import com.rgbstudios.todomobile.ui.adapters.AppThemeAdapter
import com.rgbstudios.todomobile.ui.adapters.CategoryColorAdapter
import com.rgbstudios.todomobile.utils.ColorManager
import com.rgbstudios.todomobile.utils.DialogManager
import com.rgbstudios.todomobile.utils.ToastManager
import com.rgbstudios.todomobile.viewmodel.TodoViewModel

class SettingsDetailsFragment() : Fragment() {
    private val sharedViewModel: TodoViewModel by activityViewModels()
    private lateinit var binding: FragmentSettingsDetailsBinding
    private lateinit var fragmentContext: Context
    private val dialogManager = DialogManager()
    private val toastManager = ToastManager()
    private val firebase = FirebaseAccess()
    private val colorManager = ColorManager()
    private val colors = colorManager.getAllColors()
    private val colorList = mutableListOf(PRIMARY)
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var webClientId: String
    private val thisFragment = this

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingsDetailsBinding.inflate(inflater, container, false)
        fragmentContext = requireContext()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val item = sharedViewModel.settingsItemSelected.value

        webClientId = getString(R.string.default_web_client_id)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        binding.apply {
            when (item) {
                "completeSetUp" -> {
                    completeSetUpLayout.visibility = View.VISIBLE

                    val user = sharedViewModel.currentUser.value

                    if (user != null) {
                        completeSetUpEmailEditText.text =
                            Editable.Factory.getInstance().newEditable(user.email)
                    }

                    saveCompleteSetUp.setOnClickListener {
                        setupEmailAndPassword()
                    }
                }

                "changePass" -> {
                    changePassLayout.visibility = View.VISIBLE

                    saveChangePass.setOnClickListener {
                        toastManager.showShortToast(requireContext(), "Coming soon")
                        sharedViewModel.toggleSlider(true)
                    }
                }

                "connectedAcc" -> {
                    connectedAccLayout.visibility = View.VISIBLE

                    sharedViewModel.isGoogleConnected.observe(viewLifecycleOwner){

                        val isGoogleConnected = it ?: false

                        if (isGoogleConnected) {
                            googleTick.visibility = View.VISIBLE
                            googleSwitch.text = getString(R.string.remove)
                            googleSwitch.setTextColor(
                                ContextCompat.getColor(
                                    fragmentContext,
                                    R.color.my_darker_grey
                                )
                            )
                        } else {
                            googleTick.visibility = View.INVISIBLE
                            googleSwitch.text = getString(R.string.connect)
                            googleSwitch.setTextColor(
                                ContextCompat.getColor(
                                    fragmentContext,
                                    R.color.myPrimary
                                )
                            )
                        }
                    }

                    //connect google account for authentication
                    googleSwitch.setOnClickListener {
                        if (!thereIsNetworkConnectivity()) {
                            toastManager.showLongToast(
                                requireContext(),
                                "No internet connection. Please check your network settings and try again."
                            )
                            return@setOnClickListener
                        }

                        val isGoogleConnected = sharedViewModel.isGoogleConnected.value ?: false
                        if (isGoogleConnected) {
                            disconnectGoogle()
                        } else {
                            connectGoogle()
                        }
                    }

                    facebookSwitch.setOnClickListener {
                        //Todo
                    }

                }

                "deleteAcc" -> {
                    // Todo some warning
                }

                "changeAppTheme" -> {
                    appThemeLayout.visibility = View.VISIBLE

                    // Add the list of colors to the default primary color
                    colorList.addAll(colors)

                    val themeColorAdapter =
                        AppThemeAdapter(
                            colorList,
                            colorManager
                        )
                    appThemeColorRecyclerView.layoutManager = GridLayoutManager(context, 3)

                    // Set the adapter for the colorRecyclerView
                    appThemeColorRecyclerView.adapter = themeColorAdapter
                }
            }

        }
    }

    private fun setupEmailAndPassword() {
        binding.apply {
            val email = completeSetUpEmailEditText.text.toString().trim()
            val pass = completeSetUpPasswordEditText.text.toString().trim()
            val confirmPass = completeSetUpPasswordConfirmEditText.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
                if (pass == confirmPass) {

                    // Define a regex pattern for a strong password
                    val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$"

                    if (pass.matches(Regex(passwordPattern))) {

                        progressBar.visibility = View.VISIBLE
                        // updateEmail(email)
                        // link if successful

                        toastManager.showShortToast(requireContext(), "$email: implementation coming soon")

                        progressBar.visibility = View.GONE
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

            sharedViewModel.toggleSlider(true)
            // Todo Here
        }
    }

    private fun disconnectGoogle() {
        val user = firebase.auth.currentUser

        // Check if the user is signed in with Google
        val googleProviderId = GoogleAuthProvider.PROVIDER_ID
        if (user?.providerData?.any { it.providerId == googleProviderId } == true) {
            // Unlink the Google provider
            user.unlink(googleProviderId)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Successfully unlinked
                        googleSignInClient.signOut()
                            .addOnCompleteListener { signOut ->
                                if (signOut.isSuccessful) {
                                    toastManager.showShortToast(
                                        requireContext(),
                                        "Google successfully disconnected"
                                    )
                                    sharedViewModel.upDateConnectedAccount(1,false)
                                }
                            }
                    } else {
                        // Handle the error
                        val exception = task.exception
                        firebase.addLog(exception.toString())
                        toastManager.showShortToast(requireContext(), "Account disconnection failed! Try Again.")
                    }
                }
        }
    }

    private fun connectGoogle() {
        val sigInIntent = googleSignInClient.signInIntent
        launcher.launch(sigInIntent)
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val signInTask = GoogleSignIn.getSignedInAccountFromIntent(result.data)

            if (signInTask.isSuccessful) {
                // Get the GoogleSignInAccount
                val account = signInTask.result
                if (account != null) {
                    // Link the Google credential to the current Firebase user
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    firebase.auth.currentUser?.linkWithCredential(credential)
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Linked successfully
                                toastManager.showShortToast(requireContext(), "Google account linked successfully.")
                                sharedViewModel.upDateConnectedAccount(1,true)
                                sharedViewModel.updateWebClientId(webClientId)
                            } else {
                                val exception = task.exception
                                if (exception is FirebaseAuthUserCollisionException) {
                                    // The Google account is already linked to another user.
                                    toastManager.showShortToast(
                                        requireContext(),
                                        "Google account is already linked to another user."
                                    )
                                } else {
                                    // Some other error occurred during the link.
                                    toastManager.showShortToast(requireContext(), "Failed to link Google account.")
                                }
                                googleSignInClient.signOut()
                            }
                        }
                }
            } else {
                // Google Sign-In failed
                toastManager.showShortToast(requireContext(), "Google Sign-In failed.")
            }
        }
    }

    private fun thereIsNetworkConnectivity(): Boolean {

        // Get the ConnectivityManager instance
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Get the network capabilities
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

        // Check if the network capabilities have internet access
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    companion object {
        private const val TAG = "SettingsDetailsFragment"
        private const val PRIMARY = "primary"
    }
}