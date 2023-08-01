package com.rgbstudios.todomobile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.rgbstudios.todomobile.adapter.OnboardingPagerAdapter
import com.rgbstudios.todomobile.databinding.FragmentOnboardingBinding

class OnboardingFragment : Fragment() {

    private lateinit var binding: FragmentOnboardingBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pagerAdapter = OnboardingPagerAdapter(requireActivity())
        binding.viewPager.adapter = pagerAdapter

        updatePageIndicator(0)

        // Monitor page changes to show/hide the getStartedButton
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                updatePageIndicator(position)

                if (position == 2) {
                    binding.getStartedButton.visibility = View.VISIBLE
                    binding.skip.visibility = View.GONE
                } else {
                    binding.getStartedButton.visibility = View.GONE
                    binding.skip.visibility = View.VISIBLE
                }
            }

        })

        binding.getStartedButton.setOnClickListener {
            findNavController()?.navigate(R.id.action_onboardingFragment_to_onboardingFinalFragment)
        }
        binding.skip.setOnClickListener {
            findNavController()?.navigate(R.id.action_onboardingFragment_to_onboardingFinalFragment)
        }
    }

    private fun updatePageIndicator(position: Int) {
        val indicatorTintActive = resources.getColor(R.color.myPrimary, null)
        val indicatorTintInactive = resources.getColor(com.google.android.material.R.color.m3_sys_color_light_surface_variant, null)

        binding.circle1.setColorFilter(if (position == 0) indicatorTintActive else indicatorTintInactive)
        binding.circle2.setColorFilter(if (position == 1) indicatorTintActive else indicatorTintInactive)
        binding.circle3.setColorFilter(if (position == 2) indicatorTintActive else indicatorTintInactive)
    }


}
