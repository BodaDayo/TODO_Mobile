package com.rgbstudios.todomobile.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.databinding.FragmentSettingsBinding
import com.rgbstudios.todomobile.utils.SharedPreferencesManager
import com.rgbstudios.todomobile.utils.ToastManager
import com.rgbstudios.todomobile.viewmodel.TodoViewModel
import java.util.concurrent.Executor

class SettingsFragment : Fragment() {

    private val sharedViewModel: TodoViewModel by activityViewModels()
    private lateinit var binding: FragmentSettingsBinding
    private lateinit var fragmentContext: Context
    private val toastManager = ToastManager()
    private val thisFragment = this
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: PromptInfo
    private var isSlidingPaneLayoutOpen = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        fragmentContext = requireContext()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = SharedPreferencesManager(requireContext())

        val storedPass = sharedPreferences.getString("pass", "")

        binding.apply {

            // Lock the SlidingPaneLayout
            slider.lockMode = SlidingPaneLayout.LOCK_MODE_LOCKED

            sharedViewModel.closeSlider.observe(viewLifecycleOwner) { toClose ->
                if (toClose) {
                    slider.closePane()
                    isSlidingPaneLayoutOpen = false
                }
            }

            // Check if biometric authentication is enabled
            val isBiometricEnabled = sharedViewModel.isBiometricEnabled.value ?: false

            fingerprintSwitch.isChecked = isBiometricEnabled

            sharedViewModel.isEmailAuthSet.observe(viewLifecycleOwner) { emailAuthIsSet ->
                if (emailAuthIsSet) {
                    completeSetUpLayout.visibility = View.GONE
                    completeNB.visibility = View.GONE
                } else {
                    completeSetUpLayout.visibility = View.VISIBLE
                    completeNB.visibility = View.VISIBLE
                }
            }

            completeSetUpLayout.setOnClickListener {
                openDetailsPane("completeSetUp")
            }

            changePassLayout.setOnClickListener {
                openDetailsPane("changePass")
            }

            connectedAccLayout.setOnClickListener {
                openDetailsPane("connectedAcc")
            }

            changeEmailLayout.setOnClickListener {
                openDetailsPane("changeEmail")
            }

            fingerprintSwitch.setOnCheckedChangeListener { _, isChecked ->
                // Handle the switch state change (isChecked)
                if (isChecked) {
                    if (storedPass.isNotBlank()) {
                        checkDeviceHasBiometric()
                    } else {
                        toastManager.showShortToast(requireContext(), "Sign in with email/password to enable Biometric Authentication!")
                        fingerprintSwitch.isChecked = false
                    }
                } else {
                    toastManager.showShortToast(requireContext(), "Biometric authentication disabled.")

                    // Update the isBiometricEnabled in the viewModel
                    sharedViewModel.updateIsBiometricEnabled(false)
                }
            }

            deleteAccLayout.setOnClickListener {
                toastManager.showShortToast(requireContext(), "Coming soon")
                // openDetailsPane("deleteAcc")
            }

            changeAppThemeLayout.setOnClickListener {
                openDetailsPane("changeAppTheme")
            }

            timeFormatLayout.setOnClickListener {
                toastManager.showShortToast(requireContext(), "Coming soon")
                //Todo show dialog
            }

            defaultViewLayout.setOnClickListener {
                toastManager.showShortToast(requireContext(), "Coming soon")
                //Todo show dialog
            }

            keepTasksLayout.setOnClickListener {
                toastManager.showShortToast(requireContext(), "Coming soon")
                //Todo show dialog
            }

            changeSoundLayout.setOnClickListener {
                toastManager.showShortToast(requireContext(), "Coming soon")
                //Todo show dialog
            }

            changeVibrationLayout.setOnClickListener {
                toastManager.showShortToast(requireContext(), "Coming soon")
                //Todo show dialog
            }

            popBack.setOnClickListener {
                popBackStackManager()
            }

            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                popBackStackManager()
            }
        }

    }

    private fun openDetailsPane(item: String) {
        sharedViewModel.setSettingsItem(item)
        sharedViewModel.toggleSlider(false)

        // Replace the details pane with the details fragment
        val detailsFragment = SettingsDetailsFragment()
        childFragmentManager.beginTransaction()
            .replace(R.id.settingsDetails, detailsFragment)
            .commit()

        // Open the details pane
        binding.slider.openPane()
        isSlidingPaneLayoutOpen = true
    }

    private fun popBackStackManager() {
        if (isSlidingPaneLayoutOpen) {
            sharedViewModel.toggleSlider(true)
        } else {
            // If no changes, simply pop back stack
            activity?.supportFragmentManager?.popBackStack()
        }
    }

    private fun checkDeviceHasBiometric() {
        val biometricManager = BiometricManager.from(requireContext())
        when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                createBiometricListener()
                createPromptInfo()
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                toastManager.showShortToast(requireContext(), "Your device does not support biometric authentication.")
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                toastManager.showShortToast(requireContext(), "Biometric features are currently unavailable.")
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                toastManager.showShortToast(requireContext(), "Biometric feature is not set up on your device.")
            }
            else -> {
                toastManager.showShortToast(requireContext(), "An error occurred while checking biometric availability.")
            }
        }
    }

    private fun createBiometricListener() {
        executor = ContextCompat.getMainExecutor(requireContext())
        biometricPrompt = BiometricPrompt(thisFragment, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errString != "Cancel") {
                    toastManager.showShortToast(requireContext(), "Biometric authentication set up error: $errString")
                }
                binding.fingerprintSwitch.isChecked = false
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                toastManager.showShortToast(requireContext(), "Biometric authentication set up failed. Please try again.")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                toastManager.showShortToast(requireContext(), "Biometric authentication enabled.")

                // Update the isBiometricEnabled in the viewModel
                sharedViewModel.updateIsBiometricEnabled(true)
            }
        })
    }

    private fun createPromptInfo() {
        promptInfo = PromptInfo.Builder()
            .setTitle("Verify Fingerprint")
            .setSubtitle("")
            .setNegativeButtonText("Cancel")
            .build()
    }
}