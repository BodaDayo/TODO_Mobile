package com.rgbstudios.todomobile.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.rgbstudios.todomobile.OnboardingOneFragment
import com.rgbstudios.todomobile.OnboardingThreeFragment
import com.rgbstudios.todomobile.OnboardingTwoFragment

class OnboardingPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> OnboardingOneFragment()
            1 -> OnboardingTwoFragment()
            2 -> OnboardingThreeFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}
