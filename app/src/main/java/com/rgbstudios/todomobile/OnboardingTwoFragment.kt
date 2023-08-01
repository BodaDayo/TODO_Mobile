package com.rgbstudios.todomobile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.rgbstudios.todomobile.databinding.FragmentOnboardingTwoBinding

class OnboardingTwoFragment : Fragment() {

    private lateinit var binding: FragmentOnboardingTwoBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentOnboardingTwoBinding.inflate(inflater, container, false)
        return binding.root
    }

}