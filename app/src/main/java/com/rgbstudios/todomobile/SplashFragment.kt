package com.rgbstudios.todomobile

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth


class SplashFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getPreferences(Context.MODE_PRIVATE)

        Handler(Looper.myLooper()!!).postDelayed(Runnable {
            // Check if it's the first launch
            val isFirstLaunch = sharedPreferences.getBoolean("is_first_launch", true)
            if (isFirstLaunch) {
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
                    findNavController().navigate(R.id.action_splashFragment_to_homeFragment)
                } else {
                    findNavController().navigate(R.id.action_splashFragment_to_onboardingFinalFragment)
                }
            }
        }, 1500)
    }
}