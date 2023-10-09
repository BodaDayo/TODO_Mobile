package com.rgbstudios.todomobile.ui.fragments

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.data.remote.FirebaseAccess
import com.rgbstudios.todomobile.databinding.FragmentEditTaskBinding
import com.rgbstudios.todomobile.databinding.FragmentSettingsBinding
import com.rgbstudios.todomobile.ui.adapters.CategoryColorAdapter
import com.rgbstudios.todomobile.utils.ColorManager
import com.rgbstudios.todomobile.utils.DialogManager
import com.rgbstudios.todomobile.utils.ToastManager
import com.rgbstudios.todomobile.viewmodel.TodoViewModel
import java.util.concurrent.Executor


class SettingsFragment : Fragment() {

    private val sharedViewModel: TodoViewModel by activityViewModels()
    private lateinit var binding: FragmentSettingsBinding
    private lateinit var fragmentContext: Context
    private val dialogManager = DialogManager()
    private val toastManager = ToastManager()
    private val firebase = FirebaseAccess()
    private val thisFragment = this
    private val colorManager = ColorManager()
    private val colors = colorManager.getAllColors()
    private val colorList = mutableListOf(PRIMARY)
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: PromptInfo


    // Variable to store the selected color
    var selectedThemeColor: String? = null

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

        // Add the list of colors to the default primary color
        colorList.addAll(colors)

        binding.apply {
            // Check if biometric authentication is enabled
            val isBiometricEnabled = sharedViewModel.isBiometricEnabled.value ?: false

            fingerprintSwitch.isChecked = isBiometricEnabled

            val themeColorAdapter =
                CategoryColorAdapter(
                    colorList,
                    colorManager,
                    object : CategoryColorAdapter.ColorClickListener {
                        override fun onColorClick(colorIdentifier: String) {
                            // Handle the color click event and update the selected color
                            selectedThemeColor = colorIdentifier
                            appThemeColorLayout.visibility = View.GONE

                            // Set the background for categoryColorView
                            themeColorView.setBackgroundResource(R.drawable.circular_primary_background)

                            val colorPair = colorManager.getColorMapping(colorIdentifier)

                            // Set the background tint for categoryColorView
                            themeColorView.backgroundTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(fragmentContext, colorPair.second))
                        }
                    }
                )
            appThemeColorRecyclerView.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

            // Set the adapter for the colorRecyclerView
            appThemeColorRecyclerView.adapter = themeColorAdapter

            // Set an OnCheckedChangeListener to handle switch events
            fingerprintSwitch.setOnCheckedChangeListener { _, isChecked ->
                // Handle the switch state change (isChecked)
                if (isChecked) {
                    checkDeviceHasBiometric()
                } else {
                    toastManager.showShortToast(requireContext(), "Biometric authentication disabled.")

                    // Update the isBiometricEnabled in the viewModel
                    sharedViewModel.updateIsBiometricEnabled(false)
                }
            }

            changeAppThemeLayout.setOnClickListener {
                appThemeColorLayout.visibility = View.VISIBLE
            }
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

    companion object {
        private const val PRIMARY = "primary"
    }
}