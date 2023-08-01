package com.rgbstudios.todomobile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.rgbstudios.todomobile.databinding.FragmentOnboardingThreeBinding

class OnboardingThreeFragment : Fragment() {

    private lateinit var binding: FragmentOnboardingThreeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentOnboardingThreeBinding.inflate(inflater, container, false)
        return binding.root
    }

}