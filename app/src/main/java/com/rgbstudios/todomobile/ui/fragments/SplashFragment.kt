package com.rgbstudios.todomobile.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.data.remote.FirebaseAccess
import com.rgbstudios.todomobile.utils.ToastManager


class SplashFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private var delayedNavigationHandler: Handler? = null
    private val toastManager = ToastManager()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val firebase = FirebaseAccess()
        auth = firebase.auth
        sharedPreferences = requireActivity().getPreferences(Context.MODE_PRIVATE)

        // Call the function to handle delayed navigation
        handleDelayedNavigation()
    }

    override fun onResume() {
        super.onResume()

        // Call the function to handle delayed navigation when the fragment resumes
        handleDelayedNavigation()
    }

    private fun handleDelayedNavigation() {
        // Remove any previously posted callbacks to avoid multiple executions
        delayedNavigationHandler?.removeCallbacksAndMessages(null)

        // Create a new Handler for delayed navigation
        delayedNavigationHandler = Handler(Looper.myLooper()!!)

        delayedNavigationHandler?.postDelayed(Runnable {
            // Check if it's the first launch
            val isFirstLaunch = sharedPreferences.getBoolean("is_first_launch", true)
            if (isFirstLaunch) {
                // check connection status
                checkNetworkConnectivity()

                // If it's the first launch, navigate to OnboardingFragmentOne
                findNavController().navigate(R.id.action_splashFragment_to_onboardingFragment)

                // Mark that the app has been launched before
                with(sharedPreferences.edit()) {
                    putBoolean("is_first_launch", false)
                    apply()
                }
            } else {
                // If it's not the first launch, check if the user is logged in
                if (auth.currentUser != null) {
                    // check connection status
                    checkNetworkConnectivity()

                    findNavController().navigate(R.id.action_splashFragment_to_homeFragment)
                } else {
                    // check connection status
                    checkNetworkConnectivity()

                    findNavController().navigate(R.id.action_splashFragment_to_onboardingFinalFragment)
                }
            }
        }, 1000)
    }

    private fun checkNetworkConnectivity() {
        if (!isAdded) {
            // Fragment is not attached to an activity, do nothing.
            return
        }

        // Get the ConnectivityManager instance
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Get the network capabilities
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

        // Check if the network capabilities have internet access
        if (networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) {
            // There is internet connection
        } else {
            // There is no internet connection
            toastManager.showToast(
                requireContext(),
                "No connection.\nSome features might be unavailable"
            )
        }
    }
}