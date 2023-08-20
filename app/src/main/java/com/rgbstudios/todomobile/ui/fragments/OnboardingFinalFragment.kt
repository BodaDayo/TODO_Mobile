package com.rgbstudios.todomobile.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.databinding.FragmentOnboardingFinalBinding


class OnboardingFinalFragment : Fragment() {
    private lateinit var binding: FragmentOnboardingFinalBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOnboardingFinalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginButton.setOnClickListener {
            findNavController()?.navigate(R.id.action_onboardingFinalFragment_to_signInFragment)
        }
        binding.registerButton.setOnClickListener {
            findNavController()?.navigate(R.id.action_onboardingFinalFragment_to_signUpFragment)
        }

    }
}